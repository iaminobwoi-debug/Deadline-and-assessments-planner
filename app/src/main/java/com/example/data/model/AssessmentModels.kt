package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

enum class AssessmentType(val wireValue: String, val label: String) {
    ASSIGNMENT("ASSIGNMENT", "Assignment"),
    TEST("TEST", "Test"),
    PRESENTATION("PRESENTATION", "Presentation"),
    EXAMINATION("EXAMINATION", "Examination");

    companion object {
        fun fromWire(value: String): AssessmentType =
            entries.firstOrNull { it.wireValue == value } ?: ASSIGNMENT
    }
}

enum class AssessmentStatus(val wireValue: String, val label: String) {
    UPCOMING("UPCOMING", "Upcoming"),
    IN_PROGRESS("IN_PROGRESS", "In Progress"),
    COMPLETED("COMPLETED", "Completed");

    companion object {
        fun fromWire(value: String): AssessmentStatus =
            entries.firstOrNull { it.wireValue == value } ?: UPCOMING
    }
}

enum class AcademicRole(val wireValue: String, val label: String, val tagline: String) {
    STUDENT("STUDENT", "Student", "Track submissions, tests & exam prep"),
    LECTURER("LECTURER", "Lecturer", "Monitor module deadlines & marking load"),
    COORDINATOR("COORDINATOR", "Coordinator", "Oversee programme-wide assessment clashes");

    companion object {
        fun fromWire(value: String): AcademicRole =
            entries.firstOrNull { it.wireValue == value } ?: STUDENT
    }
}

enum class StatusFilter(val label: String) {
    ALL("All"),
    OVERDUE("Overdue"),
    UPCOMING("Upcoming"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed")
}

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val preferredRole: String = AcademicRole.STUDENT.wireValue,
    val seededSamplePlan: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AssessmentDeadline(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val courseCode: String = "",
    val courseName: String = "",
    val assessmentType: String = AssessmentType.ASSIGNMENT.wireValue,
    val status: String = AssessmentStatus.UPCOMING.wireValue,
    val dueDateEpochDay: Long = 0L,
    val dueTimeLabel: String = "23:59",
    val weightPercent: Long = 20L,
    val roleContext: String = AcademicRole.STUDENT.wireValue,
    val notes: String = "",
    val isSampleSeed: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val typeEnum: AssessmentType
        get() = AssessmentType.fromWire(assessmentType)

    val statusEnum: AssessmentStatus
        get() = AssessmentStatus.fromWire(status)

    val roleEnum: AcademicRole
        get() = AcademicRole.fromWire(roleContext)

    fun daysRemaining(todayEpochDay: Long = DateUtils.todayEpochDay()): Long =
        dueDateEpochDay - todayEpochDay

    fun isOverdue(todayEpochDay: Long = DateUtils.todayEpochDay()): Boolean =
        statusEnum != AssessmentStatus.COMPLETED && dueDateEpochDay < todayEpochDay

    fun isDueToday(todayEpochDay: Long = DateUtils.todayEpochDay()): Boolean =
        statusEnum != AssessmentStatus.COMPLETED && dueDateEpochDay == todayEpochDay

    fun isDueSoon(todayEpochDay: Long = DateUtils.todayEpochDay()): Boolean {
        val delta = daysRemaining(todayEpochDay)
        return statusEnum != AssessmentStatus.COMPLETED && delta in 0L..7L
    }
}

data class AssessmentDraft(
    val title: String,
    val courseCode: String,
    val courseName: String,
    val assessmentType: AssessmentType,
    val status: AssessmentStatus,
    val dueDateEpochDay: Long,
    val dueTimeLabel: String,
    val weightPercent: Int,
    val roleContext: AcademicRole,
    val notes: String,
    val isSampleSeed: Boolean = false
)

data class PlannerSummary(
    val totalCount: Int = 0,
    val activeCount: Int = 0,
    val overdueCount: Int = 0,
    val dueTodayCount: Int = 0,
    val dueThisWeekCount: Int = 0,
    val completedCount: Int = 0,
    val totalActiveWeight: Int = 0,
    val overdueItems: List<AssessmentDeadline> = emptyList(),
    val upcomingItems: List<AssessmentDeadline> = emptyList(),
    val typeBreakdown: Map<AssessmentType, Int> = emptyMap()
)

object DateUtils {
    private const val MILLIS_PER_DAY = 86_400_000L

    fun todayEpochDay(): Long {
        val local = Calendar.getInstance()
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        utc.clear()
        utc.set(
            local.get(Calendar.YEAR),
            local.get(Calendar.MONTH),
            local.get(Calendar.DAY_OF_MONTH),
            0,
            0,
            0
        )
        utc.set(Calendar.MILLISECOND, 0)
        return utc.timeInMillis / MILLIS_PER_DAY
    }

    fun epochDayToUtcMillis(epochDay: Long): Long = epochDay * MILLIS_PER_DAY

    fun utcMillisToEpochDay(millis: Long): Long = millis / MILLIS_PER_DAY

    fun formatEpochDay(epochDay: Long): String {
        val formatter = SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return formatter.format(epochDayToUtcMillis(epochDay))
    }

    fun formatCompactDate(epochDay: Long): String {
        val formatter = SimpleDateFormat("d MMM", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return formatter.format(epochDayToUtcMillis(epochDay))
    }

    fun formatUrgencyLabel(daysRemaining: Long, isCompleted: Boolean): String {
        if (isCompleted) return "Completed"
        return when {
            daysRemaining < -1L -> "${-daysRemaining}d overdue"
            daysRemaining == -1L -> "1d overdue"
            daysRemaining == 0L -> "Due today"
            daysRemaining == 1L -> "Tomorrow"
            else -> "${daysRemaining}d left"
        }
    }
}

object SampleAcademicPlan {
    fun buildSampleDrafts(todayEpochDay: Long = DateUtils.todayEpochDay()): List<AssessmentDraft> = listOf(
        AssessmentDraft(
            title = "Raft Consensus Fault-Tolerance Lab",
            courseCode = "CS301",
            courseName = "Distributed Systems",
            assessmentType = AssessmentType.ASSIGNMENT,
            status = AssessmentStatus.IN_PROGRESS,
            dueDateEpochDay = todayEpochDay - 2L,
            dueTimeLabel = "23:59",
            weightPercent = 20,
            roleContext = AcademicRole.STUDENT,
            notes = "Submit state-machine test logs and failure-recovery report.",
            isSampleSeed = true
        ),
        AssessmentDraft(
            title = "Literature Review Draft & Rubric Check",
            courseCode = "RES410",
            courseName = "Research Methods",
            assessmentType = AssessmentType.ASSIGNMENT,
            status = AssessmentStatus.UPCOMING,
            dueDateEpochDay = todayEpochDay - 1L,
            dueTimeLabel = "17:00",
            weightPercent = 15,
            roleContext = AcademicRole.COORDINATOR,
            notes = "Verify APA citations and ethics checklist sign-off.",
            isSampleSeed = true
        ),
        AssessmentDraft(
            title = "Eigenvalues & SVD Diagnostic Quiz",
            courseCode = "MATH204",
            courseName = "Applied Linear Algebra",
            assessmentType = AssessmentType.TEST,
            status = AssessmentStatus.UPCOMING,
            dueDateEpochDay = todayEpochDay,
            dueTimeLabel = "14:00",
            weightPercent = 15,
            roleContext = AcademicRole.STUDENT,
            notes = "45-minute timed assessment in Hall B; bring scientific calculator.",
            isSampleSeed = true
        ),
        AssessmentDraft(
            title = "Capstone Architecture Pitch",
            courseCode = "SE402",
            courseName = "Software Engineering Studio",
            assessmentType = AssessmentType.PRESENTATION,
            status = AssessmentStatus.IN_PROGRESS,
            dueDateEpochDay = todayEpochDay + 2L,
            dueTimeLabel = "10:30",
            weightPercent = 25,
            roleContext = AcademicRole.STUDENT,
            notes = "8-minute live demo + 4 minutes Q&A with industry assessors.",
            isSampleSeed = true
        ),
        AssessmentDraft(
            title = "Mid-Semester Policy Brief Seminar",
            courseCode = "BUS315",
            courseName = "Organisational Strategy",
            assessmentType = AssessmentType.PRESENTATION,
            status = AssessmentStatus.UPCOMING,
            dueDateEpochDay = todayEpochDay + 4L,
            dueTimeLabel = "13:00",
            weightPercent = 20,
            roleContext = AcademicRole.LECTURER,
            notes = "Publish peer-evaluation rubric 24h prior to seminar.",
            isSampleSeed = true
        ),
        AssessmentDraft(
            title = "Predictive Modelling Midterm Test",
            courseCode = "DATA320",
            courseName = "Statistical Machine Learning",
            assessmentType = AssessmentType.TEST,
            status = AssessmentStatus.UPCOMING,
            dueDateEpochDay = todayEpochDay + 6L,
            dueTimeLabel = "09:30",
            weightPercent = 25,
            roleContext = AcademicRole.STUDENT,
            notes = "Covers regularized regression, cross-validation, and ROC analysis.",
            isSampleSeed = true
        ),
        AssessmentDraft(
            title = "Compiler Intermediate Code Generator",
            courseCode = "CS301",
            courseName = "Distributed Systems",
            assessmentType = AssessmentType.ASSIGNMENT,
            status = AssessmentStatus.UPCOMING,
            dueDateEpochDay = todayEpochDay + 10L,
            dueTimeLabel = "23:59",
            weightPercent = 15,
            roleContext = AcademicRole.LECTURER,
            notes = "Automated grading suite opens 5 days before submission cutoff.",
            isSampleSeed = true
        ),
        AssessmentDraft(
            title = "Operating Systems Final Examination",
            courseCode = "CS305",
            courseName = "Operating Systems",
            assessmentType = AssessmentType.EXAMINATION,
            status = AssessmentStatus.UPCOMING,
            dueDateEpochDay = todayEpochDay + 16L,
            dueTimeLabel = "09:00",
            weightPercent = 45,
            roleContext = AcademicRole.COORDINATOR,
            notes = "Main Examination Hall A; 2.5 hours closed-book paper.",
            isSampleSeed = true
        ),
        AssessmentDraft(
            title = "Database Normalisation Problem Set 1",
            courseCode = "DATA320",
            courseName = "Statistical Machine Learning",
            assessmentType = AssessmentType.ASSIGNMENT,
            status = AssessmentStatus.COMPLETED,
            dueDateEpochDay = todayEpochDay - 5L,
            dueTimeLabel = "23:59",
            weightPercent = 10,
            roleContext = AcademicRole.STUDENT,
            notes = "Graded and returned with schema feedback.",
            isSampleSeed = true
        )
    )
}
