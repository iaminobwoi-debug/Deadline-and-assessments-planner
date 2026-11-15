package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AcademicRole
import com.example.data.model.AssessmentDeadline
import com.example.data.model.AssessmentDraft
import com.example.data.model.AssessmentStatus
import com.example.data.model.DateUtils
import com.example.data.model.SampleAcademicPlan
import com.example.data.model.UserProfile
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class AssessmentRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun userDocRef(uid: String) = db.collection("users").document(uid)

    private fun assessmentsCollection(uid: String) =
        userDocRef(uid).collection("assessments")

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val docRef = userDocRef(userId)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    fun observeAssessments(userId: String): Flow<List<AssessmentDeadline>> = flow {
        val colRef = assessmentsCollection(userId)
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        AssessmentDeadline::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, colRef.path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureUserProfileAndSeedIfNew(
        displayName: String,
        email: String
    ): Result<Boolean> {
        val uid = requireUserId()
        val profileRef = userDocRef(uid)
        return try {
            val snapshot = profileRef.get().await()
            if (!snapshot.exists()) {
                val batch = db.batch()
                val profilePayload = mapOf(
                    "userId" to uid,
                    "displayName" to displayName.take(100),
                    "email" to email.take(150),
                    "preferredRole" to AcademicRole.STUDENT.wireValue,
                    "seededSamplePlan" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(profileRef, profilePayload)

                val samples = SampleAcademicPlan.buildSampleDrafts(DateUtils.todayEpochDay())
                for (draft in samples) {
                    val docRef = assessmentsCollection(uid).document()
                    batch.set(docRef, draftToCreatePayload(uid, draft))
                }
                batch.commit().await()
                Result.success(true)
            } else {
                Result.success(false)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, profileRef.path)
            Result.failure(e)
        }
    }

    suspend fun seedSamplePlan(): Result<Int> {
        val uid = requireUserId()
        val colRef = assessmentsCollection(uid)
        return try {
            val samples = SampleAcademicPlan.buildSampleDrafts(DateUtils.todayEpochDay())
            val batch = db.batch()
            for (draft in samples) {
                val docRef = colRef.document()
                batch.set(docRef, draftToCreatePayload(uid, draft))
            }
            val profileRef = userDocRef(uid)
            val profileSnap = profileRef.get().await()
            if (profileSnap.exists()) {
                batch.update(
                    profileRef,
                    mapOf(
                        "seededSamplePlan" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.set(
                    profileRef,
                    mapOf(
                        "userId" to uid,
                        "displayName" to (auth.currentUser?.displayName ?: "").take(100),
                        "email" to (auth.currentUser?.email ?: "").take(150),
                        "preferredRole" to AcademicRole.STUDENT.wireValue,
                        "seededSamplePlan" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(samples.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, colRef.path)
            Result.failure(e)
        }
    }

    suspend fun updatePreferredRole(role: AcademicRole): Result<Unit> {
        val uid = requireUserId()
        val profileRef = userDocRef(uid)
        return try {
            val snap = profileRef.get().await()
            if (snap.exists()) {
                profileRef.update(
                    mapOf(
                        "preferredRole" to role.wireValue,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                profileRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to (auth.currentUser?.displayName ?: "").take(100),
                        "email" to (auth.currentUser?.email ?: "").take(150),
                        "preferredRole" to role.wireValue,
                        "seededSamplePlan" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, profileRef.path)
            Result.failure(e)
        }
    }

    suspend fun createAssessment(draft: AssessmentDraft): Result<String> {
        val uid = requireUserId()
        val docRef = assessmentsCollection(uid).document()
        return try {
            docRef.set(draftToCreatePayload(uid, draft)).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getAssessment(assessmentId: String): Result<AssessmentDeadline?> {
        val uid = requireUserId()
        val docRef = assessmentsCollection(uid).document(assessmentId)
        return try {
            val snap = docRef.get().await()
            val item = snap.toObject(
                AssessmentDeadline::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(item)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateAssessment(
        existing: AssessmentDeadline,
        draft: AssessmentDraft
    ): Result<Unit> {
        val uid = requireUserId()
        require(existing.id.isNotBlank()) { "Assessment ID must not be blank for update" }
        val docRef = assessmentsCollection(uid).document(existing.id)
        return try {
            if (existing.statusEnum == AssessmentStatus.COMPLETED && draft.status != AssessmentStatus.COMPLETED) {
                docRef.update(
                    mapOf(
                        "status" to draft.status.wireValue,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "title" to draft.title.trim().take(140),
                        "courseCode" to draft.courseCode.trim().uppercase().take(24),
                        "courseName" to draft.courseName.trim().take(100),
                        "assessmentType" to draft.assessmentType.wireValue,
                        "status" to draft.status.wireValue,
                        "dueDateEpochDay" to draft.dueDateEpochDay,
                        "dueTimeLabel" to draft.dueTimeLabel.trim().take(16),
                        "weightPercent" to draft.weightPercent.coerceIn(0, 100).toLong(),
                        "roleContext" to draft.roleContext.wireValue,
                        "notes" to draft.notes.trim().take(500),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateAssessmentStatus(
        assessmentId: String,
        newStatus: AssessmentStatus
    ): Result<Unit> {
        val uid = requireUserId()
        require(assessmentId.isNotBlank()) { "Assessment ID must not be blank" }
        val docRef = assessmentsCollection(uid).document(assessmentId)
        return try {
            docRef.update(
                mapOf(
                    "status" to newStatus.wireValue,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteAssessment(assessmentId: String): Result<Unit> {
        val uid = requireUserId()
        require(assessmentId.isNotBlank()) { "Assessment ID must not be blank" }
        val docRef = assessmentsCollection(uid).document(assessmentId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    private fun draftToCreatePayload(uid: String, draft: AssessmentDraft): Map<String, Any> {
        return mapOf(
            "userId" to uid,
            "title" to draft.title.trim().take(140),
            "courseCode" to draft.courseCode.trim().uppercase().take(24),
            "courseName" to draft.courseName.trim().take(100),
            "assessmentType" to draft.assessmentType.wireValue,
            "status" to draft.status.wireValue,
            "dueDateEpochDay" to draft.dueDateEpochDay,
            "dueTimeLabel" to draft.dueTimeLabel.trim().take(16).ifEmpty { "23:59" },
            "weightPercent" to draft.weightPercent.coerceIn(0, 100).toLong(),
            "roleContext" to draft.roleContext.wireValue,
            "notes" to draft.notes.trim().take(500),
            "isSampleSeed" to draft.isSampleSeed,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}
