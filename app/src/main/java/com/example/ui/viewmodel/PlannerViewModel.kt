package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AcademicRole
import com.example.data.model.AssessmentDeadline
import com.example.data.model.AssessmentDraft
import com.example.data.model.AssessmentStatus
import com.example.data.model.AssessmentType
import com.example.data.model.DateUtils
import com.example.data.model.PlannerSummary
import com.example.data.model.StatusFilter
import com.example.data.model.UserProfile
import com.example.data.repository.AssessmentRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlannerFilterState(
    val searchQuery: String = "",
    val selectedCourse: String? = null,
    val selectedType: AssessmentType? = null,
    val selectedStatusFilter: StatusFilter = StatusFilter.ALL,
    val selectedRoleLens: AcademicRole? = null
) {
    val hasActiveFilters: Boolean
        get() = searchQuery.isNotBlank() ||
            selectedCourse != null ||
            selectedType != null ||
            selectedStatusFilter != StatusFilter.ALL ||
            selectedRoleLens != null
}

sealed interface PlannerUiState {
    data object Loading : PlannerUiState
    data class Error(val message: String) : PlannerUiState
    data class Ready(
        val allAssessments: List<AssessmentDeadline>,
        val filteredAssessments: List<AssessmentDeadline>,
        val dashboardUpcomingItems: List<AssessmentDeadline>,
        val availableCourses: List<String>,
        val summary: PlannerSummary,
        val filterState: PlannerFilterState,
        val userProfile: UserProfile?,
        val todayEpochDay: Long
    ) : PlannerUiState
}

class PlannerViewModel(
    private val repository: AssessmentRepository,
    private val currentUserId: String,
    private val userDisplayName: String,
    private val userEmail: String
) : ViewModel() {

    private val _filterState = MutableStateFlow(PlannerFilterState())
    val filterState: StateFlow<PlannerFilterState> = _filterState.asStateFlow()

    private val _isMutating = MutableStateFlow(false)
    val isMutating: StateFlow<Boolean> = _isMutating.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val assessmentsStream: StateFlow<StreamResult<List<AssessmentDeadline>>> =
        repository.observeAssessments(currentUserId)
            .map<List<AssessmentDeadline>, StreamResult<List<AssessmentDeadline>>> {
                StreamResult.Data(it)
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing assessments", rootError)
                emit(StreamResult.Error(rootError.message ?: "Failed to load assessments"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = StreamResult.Loading
            )

    private val profileStream: StateFlow<UserProfile?> =
        repository.observeUserProfile(currentUserId)
            .catch { error ->
                if (error is CancellationException &&
                    generateSequence<Throwable>(error) { it.cause }
                        .none { it is FirebaseFirestoreException }
                ) {
                    throw error
                }
                Log.w(TAG, "Error observing profile", error)
                emit(null)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = null
            )

    val uiState: StateFlow<PlannerUiState> = combine(
        assessmentsStream,
        profileStream,
        _filterState
    ) { stream, profile, filters ->
        when (stream) {
            is StreamResult.Loading -> PlannerUiState.Loading
            is StreamResult.Error -> PlannerUiState.Error(stream.message)
            is StreamResult.Data -> {
                val today = DateUtils.todayEpochDay()
                val allItems = stream.value
                val courses = allItems
                    .map { it.courseCode.trim().uppercase() }
                    .filter { it.isNotEmpty() }
                    .distinct()
                    .sorted()

                val roleScopedItems = if (filters.selectedRoleLens == null) {
                    allItems
                } else {
                    allItems.filter { it.roleEnum == filters.selectedRoleLens }
                }

                val summary = computeSummary(roleScopedItems, today)
                val filtered = applyFiltersAndSort(allItems, filters, today)
                val dashboardUpcoming = filterUpcomingForDashboard(summary.upcomingItems, filters)

                PlannerUiState.Ready(
                    allAssessments = allItems,
                    filteredAssessments = filtered,
                    dashboardUpcomingItems = dashboardUpcoming,
                    availableCourses = courses,
                    summary = summary,
                    filterState = filters,
                    userProfile = profile,
                    todayEpochDay = today
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = PlannerUiState.Loading
    )

    init {
        viewModelScope.launch {
            repository.ensureUserProfileAndSeedIfNew(
                displayName = userDisplayName,
                email = userEmail
            )
        }
    }

    fun setSearchQuery(query: String) {
        _filterState.update { it.copy(searchQuery = query) }
    }

    fun selectCourse(courseCode: String?) {
        _filterState.update {
            it.copy(selectedCourse = if (it.selectedCourse == courseCode) null else courseCode)
        }
    }

    fun selectType(type: AssessmentType?) {
        _filterState.update {
            it.copy(selectedType = if (it.selectedType == type) null else type)
        }
    }

    fun selectStatusFilter(statusFilter: StatusFilter) {
        _filterState.update { it.copy(selectedStatusFilter = statusFilter) }
    }

    fun selectRoleLens(role: AcademicRole?) {
        _filterState.update { it.copy(selectedRoleLens = role) }
        if (role != null) {
            viewModelScope.launch {
                repository.updatePreferredRole(role)
            }
        }
    }

    fun clearFilters() {
        _filterState.value = PlannerFilterState()
    }

    fun toggleComplete(assessment: AssessmentDeadline) {
        val nextStatus = when (assessment.statusEnum) {
            AssessmentStatus.COMPLETED -> AssessmentStatus.UPCOMING
            AssessmentStatus.UPCOMING -> AssessmentStatus.IN_PROGRESS
            AssessmentStatus.IN_PROGRESS -> AssessmentStatus.COMPLETED
        }
        setStatus(assessment.id, nextStatus)
    }

    fun setStatus(assessmentId: String, newStatus: AssessmentStatus) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.updateAssessmentStatus(assessmentId, newStatus)
            _isMutating.value = false
            result.onSuccess {
                _actionMessage.value = "Marked as ${newStatus.label}"
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Failed to update status"
            }
        }
    }

    fun saveAssessment(
        existing: AssessmentDeadline?,
        draft: AssessmentDraft,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = if (existing == null) {
                repository.createAssessment(draft).map { Unit }
            } else {
                repository.updateAssessment(existing, draft)
            }
            _isMutating.value = false
            result.onSuccess {
                _actionMessage.value = if (existing == null) {
                    "Added \"${draft.title}\" to your plan"
                } else {
                    "Updated \"${draft.title}\""
                }
                onSuccess()
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Could not save deadline"
            }
        }
    }

    fun deleteAssessment(assessmentId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.deleteAssessment(assessmentId)
            _isMutating.value = false
            result.onSuccess {
                _actionMessage.value = "Deadline removed from plan"
                onSuccess()
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Could not delete deadline"
            }
        }
    }

    fun loadSamplePlan() {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.seedSamplePlan()
            _isMutating.value = false
            result.onSuccess { count ->
                _actionMessage.value = "Loaded $count sample academic deadlines"
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Failed to load sample plan"
            }
        }
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    private fun computeSummary(
        items: List<AssessmentDeadline>,
        today: Long
    ): PlannerSummary {
        val overdue = items
            .filter { it.isOverdue(today) }
            .sortedBy { it.dueDateEpochDay }
        val upcoming = items
            .filter { it.statusEnum != AssessmentStatus.COMPLETED && !it.isOverdue(today) }
            .sortedBy { it.dueDateEpochDay }
        val dueToday = upcoming.count { it.dueDateEpochDay == today }
        val dueThisWeek = upcoming.count { it.daysRemaining(today) in 0L..7L }
        val completed = items.count { it.statusEnum == AssessmentStatus.COMPLETED }
        val activeWeight = items
            .filter { it.statusEnum != AssessmentStatus.COMPLETED }
            .sumOf { it.weightPercent.toInt() }
        val breakdown = AssessmentType.entries.associateWith { type ->
            items.count { it.typeEnum == type }
        }

        return PlannerSummary(
            totalCount = items.size,
            activeCount = items.size - completed,
            overdueCount = overdue.size,
            dueTodayCount = dueToday,
            dueThisWeekCount = dueThisWeek,
            completedCount = completed,
            totalActiveWeight = activeWeight,
            overdueItems = overdue,
            upcomingItems = upcoming,
            typeBreakdown = breakdown
        )
    }

    private fun applyFiltersAndSort(
        items: List<AssessmentDeadline>,
        filters: PlannerFilterState,
        today: Long
    ): List<AssessmentDeadline> {
        val query = filters.searchQuery.trim().lowercase()
        return items.filter { item ->
            val matchesQuery = query.isEmpty() ||
                item.title.lowercase().contains(query) ||
                item.courseCode.lowercase().contains(query) ||
                item.courseName.lowercase().contains(query) ||
                item.notes.lowercase().contains(query)

            val matchesCourse = filters.selectedCourse == null ||
                item.courseCode.equals(filters.selectedCourse, ignoreCase = true)

            val matchesType = filters.selectedType == null ||
                item.typeEnum == filters.selectedType

            val matchesRole = filters.selectedRoleLens == null ||
                item.roleEnum == filters.selectedRoleLens

            val matchesStatus = when (filters.selectedStatusFilter) {
                StatusFilter.ALL -> true
                StatusFilter.OVERDUE -> item.isOverdue(today)
                StatusFilter.UPCOMING -> item.statusEnum == AssessmentStatus.UPCOMING && !item.isOverdue(today)
                StatusFilter.IN_PROGRESS -> item.statusEnum == AssessmentStatus.IN_PROGRESS && !item.isOverdue(today)
                StatusFilter.COMPLETED -> item.statusEnum == AssessmentStatus.COMPLETED
            }

            matchesQuery && matchesCourse && matchesType && matchesRole && matchesStatus
        }.sortedWith(
            compareBy<AssessmentDeadline>(
                {
                    when {
                        it.isOverdue(today) -> 0
                        it.statusEnum != AssessmentStatus.COMPLETED -> 1
                        else -> 2
                    }
                },
                { it.dueDateEpochDay },
                { it.dueTimeLabel }
            )
        )
    }

    private fun filterUpcomingForDashboard(
        upcomingItems: List<AssessmentDeadline>,
        filters: PlannerFilterState
    ): List<AssessmentDeadline> {
        val query = filters.searchQuery.trim().lowercase()
        return upcomingItems.filter { item ->
            val matchesQuery = query.isEmpty() ||
                item.courseCode.lowercase().contains(query) ||
                item.courseName.lowercase().contains(query) ||
                item.title.lowercase().contains(query) ||
                item.notes.lowercase().contains(query)

            val matchesCourse = filters.selectedCourse == null ||
                item.courseCode.equals(filters.selectedCourse, ignoreCase = true)

            matchesQuery && matchesCourse
        }
    }

    private sealed interface StreamResult<out T> {
        data object Loading : StreamResult<Nothing>
        data class Data<T>(val value: T) : StreamResult<T>
        data class Error(val message: String) : StreamResult<Nothing>
    }

    private companion object {
        const val TAG = "PlannerVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
