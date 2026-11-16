package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GemFoldersRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createFolderAndChat_authenticatedOwner_succeedsAndEmitsRealtimeUpdates() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = GemFoldersRepository(firestore, auth)

        val folderId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createFolder(
                name = "Sviluppo Mobile",
                description = "Domande su Kotlin e Compose",
                colorKey = "cobalt",
                iconKey = "code"
            ).getOrThrow()
        }
        assertTrue(folderId.isNotBlank())

        val folders = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeFolders(uid).first { list -> list.any { it.id == folderId } }
        }
        assertEquals("Sviluppo Mobile", folders.first { it.id == folderId }.name)

        val chatId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createChat(
                title = "StateFlow in Compose",
                folderId = folderId,
                summaryTag = "Kotlin",
                autoOrganized = true
            ).getOrThrow()
        }
        assertTrue(chatId.isNotBlank())

        val msgId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.appendMessageAndUpdateChat(
                chatId = chatId,
                role = "user",
                text = "Come uso collectAsStateWithLifecycle?",
                sequence = 1L
            ).getOrThrow()
        }
        assertTrue(msgId.isNotBlank())

        val messages = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeMessages(uid, chatId).first { list -> list.any { it.id == msgId } }
        }
        assertEquals(1, messages.size)
        assertEquals("user", messages.first().role)
    }

    @Test
    fun deleteFolder_movesContainedChatsToUncategorized() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = GemFoldersRepository(firestore, auth)

        val folderId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createFolder(name = "Temporanea").getOrThrow()
        }
        val chatId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createChat(title = "Chat nella cartella", folderId = folderId).getOrThrow()
        }

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.deleteFolder(folderId).getOrThrow()
        }

        val chat = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getChatById(chatId).getOrThrow()
        }
        assertEquals("", chat?.folderId)
        assertTrue(
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeFolders(uid).first { list -> list.none { it.id == folderId } }
            }.none { it.id == folderId }
        )
    }

    @Test
    fun crossUserAccess_readingAnotherUsersChat_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = GemFoldersRepository(firestore, auth)
        val chatId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createChat(title = "Segreto di Alice").getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = GemFoldersRepository(firestore, auth)
        val result = withTimeout(FLOW_TIMEOUT_MS) {
            try {
                bobRepo.observeChats(aliceUid).first()
                null
            } catch (e: Exception) {
                e
            }
        }
        val firestoreException = checkNotNull(result?.unwrapFirestoreException()) {
            "Expected FirebaseFirestoreException for cross-user read, got $result"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)

        // Ensure Alice's chat still exists when signed back in as Alice
        signInTestUser(ALICE_EMAIL)
        val aliceChat = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.getChatById(chatId).getOrThrow()
        }
        assertEquals("Segreto di Alice", aliceChat?.title)
    }

    @Test
    fun unauthenticatedUser_observeFolders_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = GemFoldersRepository(firestore, auth)

        val exception = withTimeout(FLOW_TIMEOUT_MS) {
            try {
                repository.observeFolders("some_uid").first()
                null
            } catch (e: Exception) {
                e
            }
        }
        val firestoreException = checkNotNull(exception?.unwrapFirestoreException()) {
            "Expected FirebaseFirestoreException for unauthenticated list, got $exception"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_gemfolders@test.com"
        const val BOB_EMAIL = "bob_gemfolders@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
