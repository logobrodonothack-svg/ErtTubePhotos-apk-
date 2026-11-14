const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const DATABASE_ID = "ai-studio-android-erttubep-59b42cbb-f8ab-45ff-9f8c-3484d46ef5cb";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastTimestamp() {
  return new Date(Date.now() - 1000);
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

function getDb(context) {
  return context.firestore({ databaseId: DATABASE_ID });
}

test("Unauthenticated user: cannot read or write profiles or photos", async () => {
  const unauthDb = getDb(testEnv.unauthenticatedContext());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("photos").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Unauth",
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );
});

test("Authenticated user: can create and update own profile", async () => {
  const aliceDb = getDb(testEnv.authenticatedContext(ALICE_UID));
  const now = pastTimestamp();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      handle: "@alice_ert",
      preferredGalleryMode: "cinema_reels",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      handle: "@alice_studio",
      backupEnabled: true,
      backupWifiOnly: false,
      updatedAt: pastTimestamp(),
    })
  );
});

test("Authenticated user: shadow update with ghost field on profile fails", async () => {
  const aliceDb = getDb(testEnv.authenticatedContext(ALICE_UID));
  const now = pastTimestamp();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      isAdmin: true,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Authenticated user: cannot read or write another user's profile or photos", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const adminDb = getDb(context);
    const now = pastTimestamp();
    await adminDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob",
      email: "bob@example.com",
      createdAt: now,
      updatedAt: now,
    });
    await adminDb.collection("users").doc(BOB_UID).collection("photos").doc("photo_1").set({
      userId: BOB_UID,
      title: "Bob Sunset",
      album: "Travel",
      favorite: false,
      createdAt: now,
      updatedAt: now,
    });
  });

  const aliceDb = getDb(testEnv.authenticatedContext(ALICE_UID));
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("photos").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("photos").doc("photo_1").get());
});

test("Authenticated user: can create, list, update, and delete own photos", async () => {
  const aliceDb = getDb(testEnv.authenticatedContext(ALICE_UID));
  const now = pastTimestamp();
  const photoRef = aliceDb.collection("users").doc(ALICE_UID).collection("photos").doc("photo_1");

  await assertSucceeds(
    photoRef.set({
      userId: ALICE_UID,
      title: "Studio Frame 01",
      caption: "Shot on 35mm",
      album: "Studio",
      favorite: true,
      mediaType: "video",
      durationMs: 18500,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("photos").get());

  await assertSucceeds(
    photoRef.update({
      favorite: false,
      trashed: true,
      trashedAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );

  await assertSucceeds(
    photoRef.update({
      trashed: false,
      trashedAt: null,
      updatedAt: pastTimestamp(),
    })
  );

  await assertSucceeds(photoRef.delete());
});

test("Authenticated user: cannot create item with invalid album, oversized title, bad mediaType, or bad trashed", async () => {
  const aliceDb = getDb(testEnv.authenticatedContext(ALICE_UID));
  const now = pastTimestamp();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("photos").doc("bad_album").set({
      userId: ALICE_UID,
      title: "Test",
      album: "InvalidAlbum",
      favorite: false,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("photos").doc("long_title").set({
      userId: ALICE_UID,
      title: "a".repeat(101),
      album: "Reels",
      favorite: false,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("photos").doc("bad_media").set({
      userId: ALICE_UID,
      title: "Bad Media",
      album: "Reels",
      favorite: false,
      mediaType: "audio",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("photos").doc("bad_trashed").set({
      userId: ALICE_UID,
      title: "Bad Trashed",
      album: "Reels",
      favorite: false,
      trashed: "yes",
      createdAt: now,
      updatedAt: now,
    })
  );
});
