package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.ChatMessage
import com.example.data.model.ChatThread
import com.example.data.model.Folder
import com.example.data.model.FolderColor
import com.example.data.model.FolderIcon
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class GemFoldersRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("L'utente deve accedere con Google prima di usare Firestore.")
    }

    fun observeFolders(userId: String): Flow<List<Folder>> = flow {
        val path = "users/$userId/folders"
        db.collection("users")
            .document(userId)
            .collection("folders")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    Folder::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedWith(
                    compareByDescending<Folder> { it.isPinned }
                        .thenByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path, auth)
                }
                throw error
            }
            .collect { emit(it) }
    }

    fun observeChats(userId: String): Flow<List<ChatThread>> = flow {
        val path = "users/$userId/chats"
        db.collection("users")
            .document(userId)
            .collection("chats")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    ChatThread::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedWith(
                    compareByDescending<ChatThread> { it.isPinned }
                        .thenByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path, auth)
                }
                throw error
            }
            .collect { emit(it) }
    }

    fun observeMessages(userId: String, chatId: String): Flow<List<ChatMessage>> = flow {
        val path = "users/$userId/chats/$chatId/messages"
        db.collection("users")
            .document(userId)
            .collection("chats")
            .document(chatId)
            .collection("messages")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    ChatMessage::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedWith(
                    compareBy<ChatMessage> { it.sequence }
                        .thenBy { it.createdAt ?: Timestamp(0, 0) }
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path, auth)
                }
                throw error
            }
            .collect { emit(it) }
    }

    suspend fun createFolder(
        name: String,
        description: String = "",
        colorKey: String = FolderColor.COBALT.key,
        iconKey: String = FolderIcon.FOLDER.key,
        isPinned: Boolean = false
    ): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val cleanName = name.trim().take(50)
        if (cleanName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Il nome della cartella non può essere vuoto."))
        }
        val safeColor = FolderColor.fromKey(colorKey).key
        val safeIcon = FolderIcon.fromKey(iconKey).key
        val docRef = db.collection("users").document(uid).collection("folders").document()
        val payload = mapOf(
            "userId" to uid,
            "name" to cleanName,
            "description" to description.trim().take(200),
            "colorKey" to safeColor,
            "iconKey" to safeIcon,
            "isPinned" to isPinned,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun updateFolder(
        folderId: String,
        name: String,
        description: String,
        colorKey: String,
        iconKey: String,
        isPinned: Boolean
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val cleanName = name.trim().take(50)
        if (folderId.isBlank() || cleanName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Dati cartella non validi."))
        }
        val docRef = db.collection("users").document(uid).collection("folders").document(folderId)
        val updates = mapOf(
            "name" to cleanName,
            "description" to description.trim().take(200),
            "colorKey" to FolderColor.fromKey(colorKey).key,
            "iconKey" to FolderIcon.fromKey(iconKey).key,
            "isPinned" to isPinned,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun deleteFolder(folderId: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        if (folderId.isBlank()) {
            return Result.failure(IllegalArgumentException("ID cartella non valido."))
        }
        val chatsRef = db.collection("users").document(uid).collection("chats")
        val folderRef = db.collection("users").document(uid).collection("folders").document(folderId)
        return try {
            val chatsInFolder = chatsRef.whereEqualTo("folderId", folderId).get().await()
            for (chatDoc in chatsInFolder.documents) {
                chatDoc.reference.update(
                    mapOf(
                        "folderId" to "",
                        "autoOrganized" to false,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            folderRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, folderRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun createChat(
        title: String,
        folderId: String = "",
        summaryTag: String = "",
        autoOrganized: Boolean = false
    ): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val cleanTitle = title.trim().ifEmpty { "Nuova chat Gemini" }.take(120)
        val docRef = db.collection("users").document(uid).collection("chats").document()
        val payload = mapOf(
            "userId" to uid,
            "title" to cleanTitle,
            "folderId" to folderId.trim(),
            "lastMessagePreview" to "",
            "autoOrganized" to autoOrganized,
            "summaryTag" to summaryTag.trim().take(40),
            "messageCount" to 0L,
            "isPinned" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun getChatById(chatId: String): Result<ChatThread?> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid).collection("chats").document(chatId)
        return try {
            val snap = docRef.get().await()
            Result.success(
                snap.toObject(
                    ChatThread::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun appendMessageAndUpdateChat(
        chatId: String,
        role: String,
        text: String,
        sequence: Long,
        newTitle: String? = null
    ): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val cleanText = text.trim().take(32000)
        if (chatId.isBlank() || cleanText.isEmpty()) {
            return Result.failure(IllegalArgumentException("Messaggio o chat non validi."))
        }
        val chatRef = db.collection("users").document(uid).collection("chats").document(chatId)
        val msgRef = chatRef.collection("messages").document()
        val msgPayload = mapOf(
            "userId" to uid,
            "chatId" to chatId,
            "role" to role,
            "text" to cleanText,
            "sequence" to sequence,
            "createdAt" to FieldValue.serverTimestamp()
        )
        val preview = cleanText.replace("\n", " ").trim().take(240)
        val chatUpdates = mutableMapOf<String, Any>(
            "lastMessagePreview" to preview,
            "messageCount" to FieldValue.increment(1L),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!newTitle.isNullOrBlank()) {
            chatUpdates["title"] = newTitle.trim().take(120)
        }
        return try {
            msgRef.set(msgPayload).await()
            chatRef.update(chatUpdates).await()
            Result.success(msgRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, msgRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun assignChatToFolder(
        chatId: String,
        folderId: String,
        title: String? = null,
        summaryTag: String? = null,
        autoOrganized: Boolean = false
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        if (chatId.isBlank()) {
            return Result.failure(IllegalArgumentException("ID chat non valido."))
        }
        val chatRef = db.collection("users").document(uid).collection("chats").document(chatId)
        val updates = mutableMapOf<String, Any>(
            "folderId" to folderId.trim(),
            "autoOrganized" to autoOrganized,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!title.isNullOrBlank()) {
            updates["title"] = title.trim().take(120)
        }
        if (summaryTag != null) {
            updates["summaryTag"] = summaryTag.trim().take(40)
        }
        return try {
            chatRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, chatRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun updateChatMeta(
        chatId: String,
        title: String,
        isPinned: Boolean
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val cleanTitle = title.trim().take(120)
        if (chatId.isBlank() || cleanTitle.isEmpty()) {
            return Result.failure(IllegalArgumentException("Titolo chat non valido."))
        }
        val chatRef = db.collection("users").document(uid).collection("chats").document(chatId)
        return try {
            chatRef.update(
                mapOf(
                    "title" to cleanTitle,
                    "isPinned" to isPinned,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, chatRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun deleteChat(chatId: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        if (chatId.isBlank()) {
            return Result.failure(IllegalArgumentException("ID chat non valido."))
        }
        val chatRef = db.collection("users").document(uid).collection("chats").document(chatId)
        return try {
            val msgs = chatRef.collection("messages").get().await()
            for (msgDoc in msgs.documents) {
                msgDoc.reference.delete().await()
            }
            chatRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, chatRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun getMessagesOnce(chatId: String): Result<List<ChatMessage>> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val path = "users/$uid/chats/$chatId/messages"
        return try {
            val snap = db.collection("users")
                .document(uid)
                .collection("chats")
                .document(chatId)
                .collection("messages")
                .get()
                .await()
            val list = snap.toObjects(
                ChatMessage::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).sortedWith(
                compareBy<ChatMessage> { it.sequence }
                    .thenBy { it.createdAt ?: Timestamp(0, 0) }
            )
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path, auth)
            Result.failure(e)
        }
    }
}
