package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AssessmentDeadline
import com.example.data.model.AssessmentType
import com.example.data.model.StatusFilter
import com.example.ui.components.AssessmentCard
import com.example.ui.components.assessmentTypeIcon
import com.example.ui.theme.LocalAcademicColors
import com.example.ui.viewmodel.PlannerUiState

@Composable
fun PlannerScreen(
    state: PlannerUiState.Ready,
    onSearchQueryChange: (String) -> Unit,
    onSelectCourse: (String?) -> Unit,
    onSelectType: (AssessmentType?) -> Unit,
    onSelectStatusFilter: (StatusFilter) -> Unit,
    onClearFilters: () -> Unit,
    onEditAssessment: (AssessmentDeadline) -> Unit,
    onCycleStatus: (AssessmentDeadline) -> Unit,
    onLoadSamplePlan: () -> Unit,
    onAddDeadline: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filters = state.filterState
    val academicColors = LocalAcademicColors.current

    Column(modifier = modifier.fillMaxSize()) {
        // Search & Multi-Facet Filter Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = filters.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text(stringResource(id = R.string.search_placeholder)) },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = "Search assessments")
                },
                trailingIcon = {
                    if (filters.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_input")
            )

            // Filter Row 1: Course
            if (state.availableCourses.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Course:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FilterChip(
                        selected = filters.selectedCourse == null,
                        onClick = { onSelectCourse(null) },
                        label = { Text("All") }
                    )
                    state.availableCourses.forEach { course ->
                        FilterChip(
                            selected = filters.selectedCourse == course,
                            onClick = { onSelectCourse(course) },
                            label = { Text(course) },
                            modifier = Modifier.testTag("filter_course_$course")
                        )
                    }
                }
            }

            // Filter Row 2: Assessment Type
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Type:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FilterChip(
                    selected = filters.selectedType == null,
                    onClick = { onSelectType(null) },
                    label = { Text("All") }
                )
                AssessmentType.entries.forEach { type ->
                    FilterChip(
                        selected = filters.selectedType == type,
                        onClick = { onSelectType(type) },
                        leadingIcon = {
                            Icon(
                                imageVector = assessmentTypeIcon(type),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text(type.label) },
                        modifier = Modifier.testTag("filter_type_${type.wireValue.lowercase()}")
                    )
                }
            }

            // Filter Row 3: Status
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Status:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                StatusFilter.entries.forEach { statusFilter ->
                    val isOverdueChip = statusFilter == StatusFilter.OVERDUE
                    FilterChip(
                        selected = filters.selectedStatusFilter == statusFilter,
                        onClick = { onSelectStatusFilter(statusFilter) },
                        label = { Text(statusFilter.label) },
                        colors = if (isOverdueChip) {
                            FilterChipDefaults.filterChipColors(
                                selectedContainerColor = academicColors.overdueContainer,
                                selectedLabelColor = academicColors.overdue
                            )
                        } else {
                            FilterChipDefaults.filterChipColors()
                        },
                        modifier = Modifier.testTag("filter_status_${statusFilter.name.lowercase()}")
                    )
                }
            }

            // Results Count & Reset
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val overdueFilteredCount = state.filteredAssessments.count {
                    it.isOverdue(state.todayEpochDay)
                }
                val countSummary = buildString {
                    append("Showing ${state.filteredAssessments.size} of ${state.allAssessments.size}")
                    if (overdueFilteredCount > 0) {
                        append(" · $overdueFilteredCount overdue")
                    }
                }
                Text(
                    text = countSummary,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (overdueFilteredCount > 0) {
                        academicColors.overdue
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                if (filters.hasActiveFilters) {
                    TextButton(
                        onClick = onClearFilters,
                        modifier = Modifier.testTag("clear_filters_button")
                    ) {
                        Icon(
                            Icons.Filled.FilterAltOff,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Reset")
                    }
                }
            }
        }

        if (state.filteredAssessments.isEmpty()) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SearchOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(38.dp)
                    )
                    Text(
                        text = stringResource(id = R.string.empty_planner_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(id = R.string.empty_planner_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (filters.hasActiveFilters) {
                            OutlinedButton(onClick = onClearFilters) {
                                Text("Clear Filters")
                            }
                        } else {
                            OutlinedButton(onClick = onLoadSamplePlan) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Load Sample Plan")
                            }
                        }
                        Button(onClick = onAddDeadline) {
                            Text("Add Deadline")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = state.filteredAssessments,
                    key = { it.id }
                ) { assessment ->
                    AssessmentCard(
                        assessment = assessment,
                        todayEpochDay = state.todayEpochDay,
                        onCardClick = { onEditAssessment(assessment) },
                        onCycleStatus = { onCycleStatus(assessment) }
                    )
                }
            }
        }
    }
}
