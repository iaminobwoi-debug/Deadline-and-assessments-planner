package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AcademicRole
import com.example.data.model.AssessmentDeadline
import com.example.data.model.AssessmentType
import com.example.data.model.DateUtils
import com.example.data.model.StatusFilter
import com.example.ui.components.AssessmentCard
import com.example.ui.components.assessmentTypeAccent
import com.example.ui.components.assessmentTypeIcon
import com.example.ui.theme.LocalAcademicColors
import com.example.ui.viewmodel.PlannerUiState

@Composable
fun OverviewScreen(
    state: PlannerUiState.Ready,
    userName: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectCourse: (String?) -> Unit,
    onClearCourseSearch: () -> Unit,
    onSelectRoleLens: (AcademicRole?) -> Unit,
    onOpenOverdueInPlanner: () -> Unit,
    onOpenTypeInPlanner: (AssessmentType) -> Unit,
    onOpenPlanner: () -> Unit,
    onEditAssessment: (AssessmentDeadline) -> Unit,
    onCycleStatus: (AssessmentDeadline) -> Unit,
    onLoadSamplePlan: () -> Unit,
    onAddDeadline: () -> Unit,
    modifier: Modifier = Modifier
) {
    val academicColors = LocalAcademicColors.current
    val summary = state.summary
    val selectedRole = state.filterState.selectedRoleLens
    val hasCourseFilter = state.filterState.searchQuery.isNotBlank() ||
        state.filterState.selectedCourse != null

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Role Perspective Strip (Students, Lecturers, Programme Coordinators)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedRole == null,
                        onClick = { onSelectRoleLens(null) },
                        label = { Text("All Roles") },
                        modifier = Modifier.testTag("role_chip_all")
                    )
                    AcademicRole.entries.forEach { role ->
                        FilterChip(
                            selected = selectedRole == role,
                            onClick = { onSelectRoleLens(if (selectedRole == role) null else role) },
                            label = { Text(role.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("role_chip_${role.wireValue.lowercase()}")
                        )
                    }
                }

                Text(
                    text = selectedRole?.tagline
                        ?: "Unified academic schedule across student, lecturer & coordinator milestones",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 2. Editorial Hero Summary Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(128.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_academic_hero),
                            contentDescription = "Academic planner banner",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.10f),
                                            Color.Black.copy(alpha = 0.55f)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = DateUtils.formatEpochDay(state.todayEpochDay),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.90f)
                            )
                            Text(
                                text = if (userName.isNotBlank()) "Plan for $userName" else "Academic Overview",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = buildSummarySentence(summary),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val isWide = maxWidth >= 520.dp
                            if (isWide) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    MetricTile(
                                        label = "Overdue",
                                        value = summary.overdueCount.toString(),
                                        accent = academicColors.overdue,
                                        container = academicColors.overdueContainer,
                                        onClick = onOpenOverdueInPlanner,
                                        modifier = Modifier.weight(1f)
                                    )
                                    MetricTile(
                                        label = "Due ≤ 7d",
                                        value = summary.dueThisWeekCount.toString(),
                                        accent = academicColors.dueSoon,
                                        container = academicColors.dueSoonContainer,
                                        onClick = onOpenPlanner,
                                        modifier = Modifier.weight(1f)
                                    )
                                    MetricTile(
                                        label = "Active",
                                        value = "${summary.activeCount}",
                                        accent = MaterialTheme.colorScheme.primary,
                                        container = MaterialTheme.colorScheme.primaryContainer,
                                        onClick = onOpenPlanner,
                                        modifier = Modifier.weight(1f)
                                    )
                                    MetricTile(
                                        label = "Completed",
                                        value = summary.completedCount.toString(),
                                        accent = academicColors.completed,
                                        container = academicColors.completedContainer,
                                        onClick = onOpenPlanner,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        MetricTile(
                                            label = "Overdue",
                                            value = summary.overdueCount.toString(),
                                            accent = academicColors.overdue,
                                            container = academicColors.overdueContainer,
                                            onClick = onOpenOverdueInPlanner,
                                            modifier = Modifier.weight(1f)
                                        )
                                        MetricTile(
                                            label = "Due ≤ 7d",
                                            value = summary.dueThisWeekCount.toString(),
                                            accent = academicColors.dueSoon,
                                            container = academicColors.dueSoonContainer,
                                            onClick = onOpenPlanner,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        MetricTile(
                                            label = "Active (${summary.totalActiveWeight}% wt)",
                                            value = summary.activeCount.toString(),
                                            accent = MaterialTheme.colorScheme.primary,
                                            container = MaterialTheme.colorScheme.primaryContainer,
                                            onClick = onOpenPlanner,
                                            modifier = Modifier.weight(1f)
                                        )
                                        MetricTile(
                                            label = "Completed",
                                            value = summary.completedCount.toString(),
                                            accent = academicColors.completed,
                                            container = academicColors.completedContainer,
                                            onClick = onOpenPlanner,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Empty state prompt if user deleted everything
        if (state.allAssessments.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Your assessment plan is clear",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Add a personal deadline or restore the sample assignments, tests, presentations, and exams.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(onClick = onLoadSamplePlan) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Load Sample Plan")
                            }
                            Button(onClick = onAddDeadline) {
                                Text("Add Deadline")
                            }
                        }
                    }
                }
            }
        }

        // 3. Overdue Attention Section
        if (summary.overdueItems.isNotEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = academicColors.overdueContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.WarningAmber,
                                    contentDescription = null,
                                    tint = academicColors.overdue
                                )
                                Text(
                                    text = "Overdue Work (${summary.overdueCount})",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = academicColors.overdue
                                )
                            }
                            TextButton(
                                onClick = onOpenOverdueInPlanner,
                                colors = ButtonDefaults.textButtonColors(contentColor = academicColors.overdue)
                            ) {
                                Text("Filter Overdue")
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        summary.overdueItems.forEach { overdueItem ->
                            val overdueDays = -overdueItem.daysRemaining(state.todayEpochDay)
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onEditAssessment(overdueItem) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${overdueItem.courseCode} · ${overdueItem.typeEnum.label}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = academicColors.overdue
                                        )
                                        Text(
                                            text = overdueItem.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = academicColors.overdueContainer
                                    ) {
                                        Text(
                                            text = "${overdueDays}d overdue",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = academicColors.overdue,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Assessment Mix Breakdown
        if (summary.totalCount > 0) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Assessment Breakdown",
                            style = MaterialTheme.typography.titleMedium
                        )

                        // Proportional bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            AssessmentType.entries.forEach { type ->
                                val count = summary.typeBreakdown[type] ?: 0
                                if (count > 0) {
                                    Box(
                                        modifier = Modifier
                                            .weight(count.toFloat())
                                            .height(10.dp)
                                            .background(assessmentTypeAccent(type))
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AssessmentType.entries.forEach { type ->
                                val count = summary.typeBreakdown[type] ?: 0
                                val accent = assessmentTypeAccent(type)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = accent.copy(alpha = 0.11f),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onOpenTypeInPlanner(type) }
                                        .minimumInteractiveComponentSize()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = assessmentTypeIcon(type),
                                            contentDescription = null,
                                            tint = accent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "${type.label}s: $count",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = accent
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Upcoming Deadlines with Course Search Bar & Chips
        if (summary.upcomingItems.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Upcoming Deadlines (${state.dashboardUpcomingItems.size}/${summary.upcomingItems.size})",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        if (hasCourseFilter) {
                            TextButton(
                                onClick = onClearCourseSearch,
                                modifier = Modifier.testTag("dashboard_clear_course_filter")
                            ) {
                                Text("Reset")
                            }
                        } else {
                            TextButton(onClick = onOpenPlanner) {
                                Text("Full Planner")
                            }
                        }
                    }

                    OutlinedTextField(
                        value = state.filterState.searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = {
                            Text("Search or filter upcoming by course (e.g. CS301)…")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Filter upcoming deadlines by course"
                            )
                        },
                        trailingIcon = {
                            if (state.filterState.searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { onSearchQueryChange("") },
                                    modifier = Modifier.testTag("dashboard_clear_search")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Clear,
                                        contentDescription = "Clear course search"
                                    )
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
                            .testTag("dashboard_search_input")
                    )

                    if (state.availableCourses.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = state.filterState.selectedCourse == null,
                                onClick = { onSelectCourse(null) },
                                label = { Text("All Courses") },
                                modifier = Modifier.testTag("dashboard_course_all")
                            )
                            state.availableCourses.forEach { course ->
                                FilterChip(
                                    selected = state.filterState.selectedCourse == course,
                                    onClick = { onSelectCourse(course) },
                                    label = { Text(course) },
                                    modifier = Modifier.testTag("dashboard_course_$course")
                                )
                            }
                        }
                    }
                }
            }

            if (state.dashboardUpcomingItems.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SearchOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = "No upcoming deadlines match your course filter",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Try another course code or reset the filter to view all upcoming tasks.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(onClick = onClearCourseSearch) {
                                Text("Reset Course Filter")
                            }
                        }
                    }
                }
            } else {
                items(
                    items = state.dashboardUpcomingItems,
                    key = { it.id }
                ) { item ->
                    AssessmentCard(
                        assessment = item,
                        todayEpochDay = state.todayEpochDay,
                        onCardClick = { onEditAssessment(item) },
                        onCycleStatus = { onCycleStatus(item) }
                    )
                }
            }
        } else if (state.allAssessments.isNotEmpty() && summary.overdueItems.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = academicColors.completedContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircleOutline,
                            contentDescription = null,
                            tint = academicColors.completed
                        )
                        Column {
                            Text(
                                text = "All upcoming tasks completed",
                                style = MaterialTheme.typography.titleSmall,
                                color = academicColors.completed
                            )
                            Text(
                                text = "Tap Add Deadline below to schedule your next assessment.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    accent: Color,
    container: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = container,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = accent
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = accent.copy(alpha = 0.90f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun buildSummarySentence(summary: com.example.data.model.PlannerSummary): String {
    if (summary.totalCount == 0) {
        return "No assessments scheduled yet. Load the sample plan or add your first deadline."
    }
    val parts = mutableListOf<String>()
    if (summary.overdueCount > 0) {
        parts.add("${summary.overdueCount} overdue")
    }
    if (summary.dueTodayCount > 0) {
        parts.add("${summary.dueTodayCount} due today")
    }
    if (summary.dueThisWeekCount > 0) {
        parts.add("${summary.dueThisWeekCount} due within 7 days")
    }
    return if (parts.isEmpty()) {
        "${summary.activeCount} active assessments scheduled beyond this week · ${summary.completedCount} completed."
    } else {
        "Summary: ${parts.joinToString(", ")} · ${summary.completedCount} completed overall."
    }
}
