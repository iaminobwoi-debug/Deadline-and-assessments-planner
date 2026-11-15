package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CoPresent
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AssessmentDeadline
import com.example.data.model.AssessmentStatus
import com.example.data.model.AssessmentType
import com.example.data.model.DateUtils
import com.example.ui.theme.LocalAcademicColors

@Composable
fun assessmentTypeIcon(type: AssessmentType): ImageVector = when (type) {
    AssessmentType.ASSIGNMENT -> Icons.AutoMirrored.Filled.Assignment
    AssessmentType.TEST -> Icons.Filled.Quiz
    AssessmentType.PRESENTATION -> Icons.Filled.CoPresent
    AssessmentType.EXAMINATION -> Icons.Filled.School
}

@Composable
fun assessmentTypeAccent(type: AssessmentType): Color {
    val colors = LocalAcademicColors.current
    return when (type) {
        AssessmentType.ASSIGNMENT -> colors.assignmentAccent
        AssessmentType.TEST -> colors.testAccent
        AssessmentType.PRESENTATION -> colors.presentationAccent
        AssessmentType.EXAMINATION -> colors.examAccent
    }
}

@Composable
fun AssessmentCard(
    assessment: AssessmentDeadline,
    todayEpochDay: Long,
    onCardClick: () -> Unit,
    onCycleStatus: () -> Unit,
    modifier: Modifier = Modifier
) {
    val academicColors = LocalAcademicColors.current
    val isCompleted = assessment.statusEnum == AssessmentStatus.COMPLETED
    val isOverdue = assessment.isOverdue(todayEpochDay)
    val daysRemaining = assessment.daysRemaining(todayEpochDay)
    val typeAccent = assessmentTypeAccent(assessment.typeEnum)
    val railColor = when {
        isCompleted -> academicColors.completed
        isOverdue -> academicColors.overdue
        else -> typeAccent
    }

    val (urgencyBg, urgencyFg) = when {
        isCompleted -> academicColors.completedContainer to academicColors.completed
        isOverdue -> academicColors.overdueContainer to academicColors.overdue
        daysRemaining <= 2L -> academicColors.dueSoonContainer to academicColors.dueSoon
        else -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    }

    val urgencyText = DateUtils.formatUrgencyLabel(daysRemaining, isCompleted)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCompleted) 0.5.dp else 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onCardClick)
            .testTag("assessment_card_${assessment.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(railColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp, end = 12.dp, top = 14.dp, bottom = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = assessment.courseCode.ifBlank { "COURSE" },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = typeAccent.copy(alpha = 0.12f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = assessmentTypeIcon(assessment.typeEnum),
                                    contentDescription = null,
                                    tint = typeAccent,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = assessment.typeEnum.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = typeAccent
                                )
                            }
                        }

                        if (assessment.weightPercent > 0L) {
                            Text(
                                text = "${assessment.weightPercent}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onCycleStatus,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("toggle_status_${assessment.id}")
                    ) {
                        val (statusIcon, statusTint, statusDesc) = when (assessment.statusEnum) {
                            AssessmentStatus.COMPLETED -> Triple(
                                Icons.Filled.CheckCircle,
                                academicColors.completed,
                                "Completed, tap to reopen"
                            )
                            AssessmentStatus.IN_PROGRESS -> Triple(
                                Icons.Filled.PendingActions,
                                MaterialTheme.colorScheme.primary,
                                "In progress, tap to mark completed"
                            )
                            AssessmentStatus.UPCOMING -> Triple(
                                Icons.Filled.RadioButtonUnchecked,
                                MaterialTheme.colorScheme.onSurfaceVariant,
                                "Upcoming, tap to mark in progress"
                            )
                        }
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = statusDesc,
                            tint = statusTint
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = assessment.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isCompleted) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (assessment.courseName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${assessment.courseName} · ${assessment.roleEnum.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (assessment.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = assessment.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "${DateUtils.formatEpochDay(assessment.dueDateEpochDay)} · ${assessment.dueTimeLabel}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = urgencyBg
                    ) {
                        Text(
                            text = urgencyText,
                            style = MaterialTheme.typography.labelSmall,
                            color = urgencyFg,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
