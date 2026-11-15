package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.AcademicRole
import com.example.data.model.AssessmentDeadline
import com.example.data.model.AssessmentDraft
import com.example.data.model.AssessmentStatus
import com.example.data.model.AssessmentType
import com.example.data.model.DateUtils
import com.example.ui.components.assessmentTypeIcon
import com.example.ui.theme.LocalAcademicColors
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditDeadlineScreen(
    existing: AssessmentDeadline?,
    availableCourses: List<String>,
    isSaving: Boolean,
    onNavigateBack: () -> Unit,
    onSave: (AssessmentDraft) -> Unit,
    onReopenCompleted: (AssessmentStatus) -> Unit,
    onDelete: (() -> Unit)?
) {
    val today = remember { DateUtils.todayEpochDay() }
    val academicColors = LocalAcademicColors.current
    val isLockedCompleted = existing?.statusEnum == AssessmentStatus.COMPLETED

    var title by remember(existing?.id) { mutableStateOf(existing?.title ?: "") }
    var courseCode by remember(existing?.id) { mutableStateOf(existing?.courseCode ?: "") }
    var courseName by remember(existing?.id) { mutableStateOf(existing?.courseName ?: "") }
    var assessmentType by remember(existing?.id) {
        mutableStateOf(existing?.typeEnum ?: AssessmentType.ASSIGNMENT)
    }
    var status by remember(existing?.id) {
        mutableStateOf(existing?.statusEnum ?: AssessmentStatus.UPCOMING)
    }
    var dueDateEpochDay by remember(existing?.id) {
        mutableLongStateOf(existing?.dueDateEpochDay ?: (today + 5L))
    }
    var dueTimeLabel by remember(existing?.id) {
        mutableStateOf(existing?.dueTimeLabel ?: "23:59")
    }
    var weightPercent by remember(existing?.id) {
        mutableIntStateOf(existing?.weightPercent?.toInt() ?: 20)
    }
    var roleContext by remember(existing?.id) {
        mutableStateOf(existing?.roleEnum ?: AcademicRole.STUDENT)
    }
    var notes by remember(existing?.id) { mutableStateOf(existing?.notes ?: "") }

    var validationError by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val daysDelta = dueDateEpochDay - today
    val urgencyText = DateUtils.formatUrgencyLabel(
        daysRemaining = daysDelta,
        isCompleted = status == AssessmentStatus.COMPLETED
    )
    val (previewBg, previewFg) = when {
        status == AssessmentStatus.COMPLETED ->
            academicColors.completedContainer to academicColors.completed
        daysDelta < 0L ->
            academicColors.overdueContainer to academicColors.overdue
        daysDelta <= 2L ->
            academicColors.dueSoonContainer to academicColors.dueSoon
        else ->
            MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (existing == null) "Add Deadline" else "Edit Assessment",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (existing != null && onDelete != null) {
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.testTag("delete_deadline_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Delete deadline",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Live Days Remaining Preview Banner
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = previewBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Calculated Countdown",
                                style = MaterialTheme.typography.labelSmall,
                                color = previewFg.copy(alpha = 0.85f)
                            )
                            Text(
                                text = "${DateUtils.formatEpochDay(dueDateEpochDay)} at $dueTimeLabel",
                                style = MaterialTheme.typography.titleSmall,
                                color = previewFg
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Text(
                                text = urgencyText,
                                style = MaterialTheme.typography.labelMedium,
                                color = previewFg,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                if (isLockedCompleted) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Completed items are locked against edits.",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "Reopen to Upcoming or In Progress to modify details.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            TextButton(onClick = { onReopenCompleted(AssessmentStatus.IN_PROGRESS) }) {
                                Text("Reopen")
                            }
                        }
                    }
                }

                if (validationError != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = validationError ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it.take(140)
                        validationError = null
                    },
                    enabled = !isLockedCompleted,
                    label = { Text("Assessment Title *") },
                    placeholder = { Text("e.g., Distributed Systems Lab 4") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_title")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = courseCode,
                        onValueChange = {
                            courseCode = it.uppercase().take(24)
                            validationError = null
                        },
                        enabled = !isLockedCompleted,
                        label = { Text("Course Code *") },
                        placeholder = { Text("CS301") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(0.42f)
                            .testTag("input_course_code")
                    )

                    OutlinedTextField(
                        value = courseName,
                        onValueChange = { courseName = it.take(100) },
                        enabled = !isLockedCompleted,
                        label = { Text("Course Name") },
                        placeholder = { Text("Distributed Systems") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(0.58f)
                            .testTag("input_course_name")
                    )
                }

                if (availableCourses.isNotEmpty() && !isLockedCompleted) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Quick course:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        availableCourses.forEach { code ->
                            SuggestionChip(
                                onClick = { courseCode = code },
                                label = { Text(code) }
                            )
                        }
                    }
                }

                // Assessment Type Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Assessment Type",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssessmentType.entries.forEach { type ->
                            FilterChip(
                                selected = assessmentType == type,
                                onClick = { if (!isLockedCompleted) assessmentType = type },
                                enabled = !isLockedCompleted,
                                leadingIcon = {
                                    Icon(
                                        imageVector = assessmentTypeIcon(type),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text(type.label) },
                                modifier = Modifier.testTag("form_type_${type.wireValue.lowercase()}")
                            )
                        }
                    }
                }

                // Due Date Picker & Quick Offsets
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Due Date & Time",
                        style = MaterialTheme.typography.titleSmall
                    )
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        enabled = !isLockedCompleted,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("pick_date_button")
                    ) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Select Date: ${DateUtils.formatEpochDay(dueDateEpochDay)}")
                    }

                    if (!isLockedCompleted) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val quickOffsets = listOf(
                                -1L to "-1d (Overdue)",
                                0L to "Today",
                                3L to "+3 days",
                                7L to "+1 week",
                                14L to "+2 weeks"
                            )
                            quickOffsets.forEach { (offset, label) ->
                                FilterChip(
                                    selected = dueDateEpochDay == today + offset,
                                    onClick = { dueDateEpochDay = today + offset },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Cutoff:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        listOf("09:00", "12:00", "14:00", "17:00", "23:59").forEach { time ->
                            FilterChip(
                                selected = dueTimeLabel == time,
                                onClick = { if (!isLockedCompleted) dueTimeLabel = time },
                                enabled = !isLockedCompleted,
                                label = { Text(time) }
                            )
                        }
                    }
                }

                // Status & Role
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Status",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssessmentStatus.entries.forEach { st ->
                            FilterChip(
                                selected = status == st,
                                onClick = {
                                    if (!isLockedCompleted) {
                                        status = st
                                    } else if (st != AssessmentStatus.COMPLETED) {
                                        onReopenCompleted(st)
                                    }
                                },
                                label = { Text(st.label) }
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Academic Role Context",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AcademicRole.entries.forEach { role ->
                            FilterChip(
                                selected = roleContext == role,
                                onClick = { if (!isLockedCompleted) roleContext = role },
                                enabled = !isLockedCompleted,
                                label = { Text(role.label) }
                            )
                        }
                    }
                }

                // Weighting Slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Grade Weighting",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "$weightPercent%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = weightPercent.toFloat(),
                        onValueChange = {
                            if (!isLockedCompleted) {
                                weightPercent = ((it / 5f).roundToInt() * 5).coerceIn(0, 100)
                            }
                        },
                        enabled = !isLockedCompleted,
                        valueRange = 0f..100f,
                        steps = 19
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it.take(500) },
                    enabled = !isLockedCompleted,
                    label = { Text("Notes / Venue / Rubric") },
                    placeholder = { Text("Submission guidelines, exam hall, or prep checklist") },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_notes")
                )

                Spacer(Modifier.height(4.dp))

                Button(
                    onClick = {
                        when {
                            title.isBlank() -> validationError = "Please enter an assessment title."
                            courseCode.isBlank() -> validationError = "Please enter a course code (e.g., CS301)."
                            else -> {
                                onSave(
                                    AssessmentDraft(
                                        title = title.trim(),
                                        courseCode = courseCode.trim().uppercase(),
                                        courseName = courseName.trim(),
                                        assessmentType = assessmentType,
                                        status = status,
                                        dueDateEpochDay = dueDateEpochDay,
                                        dueTimeLabel = dueTimeLabel.ifBlank { "23:59" },
                                        weightPercent = weightPercent,
                                        roleContext = roleContext,
                                        notes = notes.trim(),
                                        isSampleSeed = existing?.isSampleSeed ?: false
                                    )
                                )
                            }
                        }
                    },
                    enabled = !isSaving && !isLockedCompleted,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("save_deadline_button")
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Saving…")
                    } else {
                        Text(if (existing == null) "Save to Personal Plan" else "Update Assessment")
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateUtils.epochDayToUtcMillis(dueDateEpochDay)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            dueDateEpochDay = DateUtils.utcMillisToEpochDay(millis)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showDeleteConfirm && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete deadline?") },
            text = { Text("Remove \"${existing?.title}\" from your academic plan? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
