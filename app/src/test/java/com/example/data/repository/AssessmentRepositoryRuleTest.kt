package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.AcademicRole
import com.example.data.model.AssessmentDraft
import com.example.data.model.AssessmentStatus
import com.example.data.model.AssessmentType
import com.example.data.model.DateUtils
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AssessmentRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private fun sampleDraft(
        title: String = "Distributed Systems Lab",
        status: AssessmentStatus = AssessmentStatus.UPCOMING
    ) = AssessmentDraft(
        title = title,
        courseCode = "CS301",
        courseName = "Distributed Systems",
        assessmentType = AssessmentType.ASSIGNMENT,
        status = status,
        dueDateEpochDay = DateUtils.todayEpochDay() + 3L,
        dueTimeLabel = "23:59",
        weightPercent = 25,
        roleContext = AcademicRole.STUDENT,
        notes = "Unit test submission",
        isSampleSeed = false
    )

    @Test
    fun createAssessment_authenticatedOwner_createsAndObservesItem() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = AssessmentRepository(firestore, auth)

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createAssessment(sampleDraft())
        }
        assertTrue(createResult.isSuccess)
        val docId = createResult.getOrThrow()
        assertTrue(docId.isNotBlank())

        val items = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeAssessments(uid).first { list -> list.any { it.id == docId } }
        }
        val created = items.first { it.id == docId }
        assertEquals("Distributed Systems Lab", created.title)
        assertEquals("CS301", created.courseCode)
    }

    @Test
    fun seedSamplePlan_authenticatedOwner_seedsAllCategories() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = AssessmentRepository(firestore, auth)

        val seedResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.seedSamplePlan()
        }
        assertTrue(seedResult.isSuccess)
        assertTrue(seedResult.getOrThrow() >= 8)

        val items = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeAssessments(uid).first { it.size >= 8 }
        }
        val types = items.map { it.typeEnum }.toSet()
        assertTrue(types.contains(AssessmentType.ASSIGNMENT))
        assertTrue(types.contains(AssessmentType.TEST))
        assertTrue(types.contains(AssessmentType.PRESENTATION))
        assertTrue(types.contains(AssessmentType.EXAMINATION))
    }

    @Test
    fun updateAndDeleteAssessment_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = AssessmentRepository(firestore, auth)
        val docId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createAssessment(sampleDraft("Midterm Exam")).getOrThrow()
        }

        val statusResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateAssessmentStatus(docId, AssessmentStatus.COMPLETED)
        }
        assertTrue(statusResult.isSuccess)

        val fetched = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getAssessment(docId).getOrThrow()
        }
        assertNotNull(fetched)
        assertEquals(AssessmentStatus.COMPLETED, fetched?.statusEnum)

        val deleteResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.deleteAssessment(docId)
        }
        assertTrue(deleteResult.isSuccess)
    }

    @Test
    fun getAssessment_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = AssessmentRepository(firestore, auth)
        val docId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createAssessment(sampleDraft("Alice Private Exam")).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobError = try {
            withTimeout(FLOW_TIMEOUT_MS) {
                AssessmentRepository(firestore, auth).observeAssessments(aliceUid).first()
            }
            null
        } catch (e: Exception) {
            e
        }
        assertNotNull(bobError)
        val firestoreException = checkNotNull(bobError?.unwrapFirestoreException()) {
            "Expected FirebaseFirestoreException, got $bobError"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
        assertTrue(docId.isNotBlank())
    }

    @Test
    fun observeAssessments_unauthenticatedCaller_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = AssessmentRepository(firestore, auth)

        val exception = try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeAssessments("alice_unauth_uid").first()
            }
            null
        } catch (e: Exception) {
            e
        }
        assertNotNull(exception)
        val firestoreException = checkNotNull(exception?.unwrapFirestoreException()) {
            "Expected FirebaseFirestoreException, got $exception"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_student@test.com"
        const val BOB_EMAIL = "bob_lecturer@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
