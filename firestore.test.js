const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_student_123";
const BOB_UID = "bob_lecturer_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastTimestamp() {
  return new Date(Date.now() - 2000);
}

function validAssessment(uid, overrides = {}) {
  const now = pastTimestamp();
  return {
    userId: uid,
    title: "Distributed Systems Lab 3",
    courseCode: "CS301",
    courseName: "Distributed Systems",
    assessmentType: "ASSIGNMENT",
    status: "UPCOMING",
    dueDateEpochDay: 20775,
    dueTimeLabel: "23:59",
    weightPercent: 20,
    roleContext: "STUDENT",
    notes: "Submit tar.gz via course portal",
    isSampleSeed: false,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function validProfile(uid, overrides = {}) {
  const now = pastTimestamp();
  return {
    userId: uid,
    displayName: "Alice",
    email: "alice@uni.edu",
    preferredRole: "STUDENT",
    seededSamplePlan: true,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write assessments", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("assessments").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("assessments")
      .doc("a1")
      .set(validAssessment(ALICE_UID))
  );
});

test("Owner can create, read, update, and delete own assessment", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("assessments")
    .doc("assess_1");

  await assertSucceeds(docRef.set(validAssessment(ALICE_UID)));
  await assertSucceeds(docRef.get());
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("assessments").get()
  );
  await assertSucceeds(
    docRef.update({
      status: "IN_PROGRESS",
      updatedAt: pastTimestamp(),
    })
  );
  await assertSucceeds(docRef.delete());
});

test("Cross-user access is denied for reading, writing, and deleting", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .collection("assessments")
      .doc("alice_exam")
      .set(validAssessment(ALICE_UID, { assessmentType: "EXAMINATION" }));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const aliceDoc = bobDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("assessments")
    .doc("alice_exam");

  await assertFails(aliceDoc.get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("assessments").get()
  );
  await assertFails(
    aliceDoc.set(validAssessment(BOB_UID))
  );
  await assertFails(aliceDoc.delete());
});

test("Shadow update with ghost field and invalid enum are rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("assessments")
    .doc("assess_2");

  await assertFails(
    docRef.set(validAssessment(ALICE_UID, { isVerified: true }))
  );
  await assertFails(
    docRef.set(validAssessment(ALICE_UID, { assessmentType: "QUIZ_INVALID" }))
  );
  await assertFails(
    docRef.set(validAssessment(ALICE_UID, { weightPercent: 150 }))
  );
});

test("Terminal state locking prevents editing title when COMPLETED unless reopening", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("assessments")
    .doc("assess_3");

  await assertSucceeds(
    docRef.set(validAssessment(ALICE_UID, { status: "COMPLETED" }))
  );
  await assertFails(
    docRef.update({
      title: "Tampered Title",
      updatedAt: pastTimestamp(),
    })
  );
  await assertSucceeds(
    docRef.update({
      status: "IN_PROGRESS",
      updatedAt: pastTimestamp(),
    })
  );
});

test("User profile creation and role update succeed for owner and fail for others", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      preferredRole: "COORDINATOR",
      updatedAt: pastTimestamp(),
    })
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).get()
  );
});
