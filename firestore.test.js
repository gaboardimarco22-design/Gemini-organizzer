const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const nowTimestamp = () => new Date(Date.now() - 1000);

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

function validFolderPayload(uid, overrides = {}) {
  return {
    userId: uid,
    name: "Sviluppo Android",
    description: "Chat su Kotlin, Compose e Firebase",
    colorKey: "cobalt",
    iconKey: "code",
    isPinned: false,
    createdAt: nowTimestamp(),
    updatedAt: nowTimestamp(),
    ...overrides,
  };
}

function validChatPayload(uid, folderId = "", overrides = {}) {
  return {
    userId: uid,
    title: "Architettura MVVM",
    folderId,
    lastMessagePreview: "Ecco come strutturare i ViewModel...",
    autoOrganized: false,
    summaryTag: "Kotlin",
    messageCount: 2,
    isPinned: false,
    createdAt: nowTimestamp(),
    updatedAt: nowTimestamp(),
    ...overrides,
  };
}

function validMessagePayload(uid, chatId, overrides = {}) {
  return {
    userId: uid,
    chatId,
    role: "user",
    text: "Spiegami Jetpack Compose",
    sequence: 1,
    createdAt: nowTimestamp(),
    ...overrides,
  };
}

test("Unauthenticated user: cannot list or write folders or chats", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/folders`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/chats`).get());
  await assertFails(
    unauthDb.doc(`users/${ALICE_UID}/folders/f1`).set(validFolderPayload(ALICE_UID))
  );
});

test("Owner can create, read, update, and delete own folder", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const folderRef = aliceDb.doc(`users/${ALICE_UID}/folders/folder_1`);

  await assertSucceeds(folderRef.set(validFolderPayload(ALICE_UID)));
  await assertSucceeds(folderRef.get());
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/folders`).get());
  await assertSucceeds(
    folderRef.update({
      name: "Kotlin & Compose",
      isPinned: true,
      updatedAt: nowTimestamp(),
    })
  );
  await assertSucceeds(folderRef.delete());
});

test("Cross-user access: Bob cannot read or write Alice's folders or chats", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/folders/folder_1`).set(validFolderPayload(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/chats/chat_1`).set(validChatPayload(ALICE_UID, "folder_1"))
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.doc(`users/${ALICE_UID}/folders/folder_1`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/chats`).get());
  await assertFails(
    bobDb.doc(`users/${ALICE_UID}/chats/chat_1`).update({
      title: "Hacked",
      updatedAt: nowTimestamp(),
    })
  );
});

test("Shadow update & invalid schema rejection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const folderRef = aliceDb.doc(`users/${ALICE_UID}/folders/folder_1`);
  await assertSucceeds(folderRef.set(validFolderPayload(ALICE_UID)));

  // Ghost field on create
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/folders/folder_bad`).set({
      ...validFolderPayload(ALICE_UID),
      isAdmin: true,
    })
  );

  // Invalid color enum
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/folders/folder_bad2`).set(
      validFolderPayload(ALICE_UID, { colorKey: "neon_pink" })
    )
  );

  // Ghost field on update
  await assertFails(
    folderRef.update({
      name: "Legit Name",
      ghostField: "evil",
      updatedAt: nowTimestamp(),
    })
  );
});

test("Chat creation requires valid existing folderId or empty string", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Uncategorized (empty folderId) succeeds
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/chats/chat_uncat`).set(validChatPayload(ALICE_UID, ""))
  );

  // Non-existent folderId fails
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/chats/chat_orphan`).set(
      validChatPayload(ALICE_UID, "nonexistent_folder")
    )
  );

  // Create folder then assign chat succeeds
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/folders/folder_work`).set(validFolderPayload(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/chats/chat_work`).set(validChatPayload(ALICE_UID, "folder_work"))
  );
});

test("Chat messages subcollection enforces parent chat existence and immutable messages", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Orphan message fails
  await assertFails(
    aliceDb
      .doc(`users/${ALICE_UID}/chats/missing_chat/messages/msg_1`)
      .set(validMessagePayload(ALICE_UID, "missing_chat"))
  );

  // Create chat then message succeeds
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/chats/chat_1`).set(validChatPayload(ALICE_UID, ""))
  );
  const msgRef = aliceDb.doc(`users/${ALICE_UID}/chats/chat_1/messages/msg_1`);
  await assertSucceeds(msgRef.set(validMessagePayload(ALICE_UID, "chat_1")));

  // Updating an existing message fails (append-only)
  await assertFails(msgRef.update({ text: "Tampered" }));

  // Deleting message by owner succeeds (for chat deletion cleanup)
  await assertSucceeds(msgRef.delete());
});
