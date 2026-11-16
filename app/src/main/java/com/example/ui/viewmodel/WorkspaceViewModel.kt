package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.GeminiRepository
import com.example.data.model.AiOrganizationSuggestion
import com.example.data.model.ChatMessage
import com.example.data.model.ChatThread
import com.example.data.model.Folder
import com.example.data.model.FolderColor
import com.example.data.model.FolderIcon
import com.example.data.repository.GemFoldersRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DataState<out T> {
    data object Loading : DataState<Nothing>
    data class Success<T>(val data: T) : DataState<T>
    data class Error(val message: String) : DataState<Nothing>
}

sealed interface FolderFilter {
    data object All : FolderFilter
    data object Uncategorized : FolderFilter
    data class Specific(val folderId: String) : FolderFilter
}

class WorkspaceViewModel(
    private val repository: GemFoldersRepository,
    private val geminiRepository: GeminiRepository,
    private val currentUserId: String
) : ViewModel() {

    val foldersState: StateFlow<DataState<List<Folder>>> =
        repository.observeFolders(currentUserId)
            .map<List<Folder>, DataState<List<Folder>>> { DataState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing folders", rootError)
                emit(DataState.Error(rootError.localizedMessage ?: "Impossibile caricare le cartelle"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = DataState.Loading
            )

    val chatsState: StateFlow<DataState<List<ChatThread>>> =
        repository.observeChats(currentUserId)
            .map<List<ChatThread>, DataState<List<ChatThread>>> { DataState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing chats", rootError)
                emit(DataState.Error(rootError.localizedMessage ?: "Impossibile caricare le chat"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = DataState.Loading
            )

    private val _folderFilter = MutableStateFlow<FolderFilter>(FolderFilter.All)
    val folderFilter: StateFlow<FolderFilter> = _folderFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _autoOrganizeEnabled = MutableStateFlow(true)
    val autoOrganizeEnabled: StateFlow<Boolean> = _autoOrganizeEnabled.asStateFlow()

    private val _activeChatId = MutableStateFlow<String?>(null)
    val activeChatId: StateFlow<String?> = _activeChatId.asStateFlow()

    private val _isChatScreenOpen = MutableStateFlow(false)
    val isChatScreenOpen: StateFlow<Boolean> = _isChatScreenOpen.asStateFlow()

    private val _draftFolderId = MutableStateFlow("")
    val draftFolderId: StateFlow<String> = _draftFolderId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeMessages: StateFlow<List<ChatMessage>> = _activeChatId
        .flatMapLatest { chatId ->
            if (chatId.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                repository.observeMessages(currentUserId, chatId)
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        val rootError = firestoreError ?: error
                        Log.w(TAG, "Error observing messages", rootError)
                        _statusMessage.value = rootError.localizedMessage ?: "Errore caricamento messaggi"
                        emit(emptyList())
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    private val _streamingReply = MutableStateFlow("")
    val streamingReply: StateFlow<String> = _streamingReply.asStateFlow()

    private val _isSendingMessage = MutableStateFlow(false)
    val isSendingMessage: StateFlow<Boolean> = _isSendingMessage.asStateFlow()

    private val _isOrganizing = MutableStateFlow(false)
    val isOrganizing: StateFlow<Boolean> = _isOrganizing.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _chatError = MutableStateFlow<String?>(null)
    val chatError: StateFlow<String?> = _chatError.asStateFlow()

    fun setFolderFilter(filter: FolderFilter) {
        _folderFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleAutoOrganize(enabled: Boolean) {
        _autoOrganizeEnabled.value = enabled
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun clearChatError() {
        _chatError.value = null
    }

    fun openNewChat(preselectedFolderId: String? = null) {
        val targetFolder = preselectedFolderId ?: when (val f = _folderFilter.value) {
            is FolderFilter.Specific -> f.folderId
            else -> ""
        }
        _activeChatId.value = null
        _draftFolderId.value = targetFolder
        _streamingReply.value = ""
        _chatError.value = null
        _isChatScreenOpen.value = true
    }

    fun openExistingChat(chatId: String) {
        _activeChatId.value = chatId
        _draftFolderId.value = ""
        _streamingReply.value = ""
        _chatError.value = null
        _isChatScreenOpen.value = true
    }

    fun closeChatScreen() {
        _isChatScreenOpen.value = false
        _chatError.value = null
    }

    fun sendMessage(rawText: String) {
        val text = rawText.trim()
        if (text.isEmpty() || _isSendingMessage.value) return

        viewModelScope.launch {
            _isSendingMessage.value = true
            _chatError.value = null
            _streamingReply.value = ""

            var chatId = _activeChatId.value
            val priorHistory = activeMessages.value
            val isNewConversation = chatId == null

            try {
                if (chatId == null) {
                    val initialTitle = text.replace("\n", " ").trim().take(48)
                        .ifEmpty { "Nuova chat Gemini" }
                    chatId = repository.createChat(
                        title = initialTitle,
                        folderId = _draftFolderId.value,
                        summaryTag = "",
                        autoOrganized = false
                    ).getOrThrow()
                    _activeChatId.value = chatId
                }

                val baseSeq = (priorHistory.maxOfOrNull { it.sequence } ?: 0L) + 1L
                repository.appendMessageAndUpdateChat(
                    chatId = chatId,
                    role = "user",
                    text = text,
                    sequence = baseSeq
                ).getOrThrow()

                var finalReply = ""
                geminiRepository.streamChatReply(
                    history = priorHistory,
                    newUserMessage = text
                ).collect { partial ->
                    finalReply = partial
                    _streamingReply.value = partial
                }

                if (finalReply.isBlank()) {
                    throw IllegalStateException("Gemini non ha restituito testo.")
                }

                repository.appendMessageAndUpdateChat(
                    chatId = chatId,
                    role = "model",
                    text = finalReply,
                    sequence = baseSeq + 1L
                ).getOrThrow()
                _streamingReply.value = ""
                _isSendingMessage.value = false

                // Automatically organize on first reply when toggle is active and chat is uncategorized
                val currentChat = (chatsState.value as? DataState.Success)?.data
                    ?.firstOrNull { it.id == chatId }
                val shouldAutoOrganize = _autoOrganizeEnabled.value &&
                    (isNewConversation || (currentChat != null && currentChat.folderId.isEmpty() && !currentChat.autoOrganized))
                if (shouldAutoOrganize) {
                    organizeChatInternal(chatId, showToastOnSuccess = true)
                }
            } catch (e: CancellationException) {
                _isSendingMessage.value = false
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "sendMessage failed", e)
                _streamingReply.value = ""
                _isSendingMessage.value = false
                _chatError.value = e.localizedMessage ?: "Errore durante l'invio a Gemini."
            }
        }
    }

    fun organizeSingleChat(chatId: String) {
        if (_isOrganizing.value) return
        viewModelScope.launch {
            organizeChatInternal(chatId, showToastOnSuccess = true)
        }
    }

    private suspend fun organizeChatInternal(chatId: String, showToastOnSuccess: Boolean) {
        _isOrganizing.value = true
        try {
            val messages = repository.getMessagesOnce(chatId).getOrElse { emptyList() }
            if (messages.isEmpty()) {
                _statusMessage.value = "Invia almeno un messaggio prima di organizzare la chat."
                return
            }
            val currentFolders = (foldersState.value as? DataState.Success)?.data.orEmpty()
            val suggestion = geminiRepository.suggestFolderForChat(
                chatId = chatId,
                messages = messages,
                existingFolders = currentFolders
            )
            val resolvedFolder = resolveOrCreateFolder(suggestion, currentFolders)
            repository.assignChatToFolder(
                chatId = chatId,
                folderId = resolvedFolder.first,
                title = suggestion.suggestedTitle,
                summaryTag = suggestion.summaryTag,
                autoOrganized = true
            ).getOrThrow()

            if (showToastOnSuccess) {
                _statusMessage.value = "Gemini ha organizzato in \"${resolvedFolder.second}\": ${suggestion.reason}"
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "organizeChatInternal failed", e)
            _statusMessage.value = e.localizedMessage ?: "Impossibile organizzare con Gemini."
        } finally {
            _isOrganizing.value = false
        }
    }

    fun organizeAllUncategorized(includeAlreadyOrganized: Boolean = false) {
        if (_isOrganizing.value) return
        viewModelScope.launch {
            _isOrganizing.value = true
            try {
                val allChats = (chatsState.value as? DataState.Success)?.data.orEmpty()
                val targets = if (includeAlreadyOrganized) {
                    allChats
                } else {
                    allChats.filter { it.folderId.isEmpty() }
                }
                if (targets.isEmpty()) {
                    _statusMessage.value = "Tutte le chat sono già organizzate in cartelle!"
                    return@launch
                }

                val currentFolders = (foldersState.value as? DataState.Success)?.data.orEmpty().toMutableList()
                val suggestions = geminiRepository.batchOrganizeChats(targets, currentFolders)
                if (suggestions.isEmpty()) {
                    _statusMessage.value = "Nessuna modifica suggerita da Gemini."
                    return@launch
                }

                // Cache newly created folders by normalized lowercase name so similar chats share one folder
                val createdByName = mutableMapOf<String, Pair<String, String>>()
                var movedCount = 0

                for (s in suggestions) {
                    val existing = currentFolders.firstOrNull { it.id == s.matchedFolderId }
                    val (folderId, folderName) = if (existing != null) {
                        existing.id to existing.name
                    } else {
                        val candidateName = s.suggestedNewFolderName.ifBlank { "Generale" }
                        val key = candidateName.lowercase()
                        val matchingExisting = currentFolders.firstOrNull {
                            it.name.trim().lowercase() == key
                        }
                        if (matchingExisting != null) {
                            matchingExisting.id to matchingExisting.name
                        } else {
                            createdByName[key] ?: run {
                                val newId = repository.createFolder(
                                    name = candidateName,
                                    description = s.suggestedNewFolderDescription,
                                    colorKey = s.suggestedColorKey,
                                    iconKey = s.suggestedIconKey
                                ).getOrThrow()
                                val createdFolder = Folder(
                                    id = newId,
                                    userId = currentUserId,
                                    name = candidateName,
                                    description = s.suggestedNewFolderDescription,
                                    colorKey = s.suggestedColorKey,
                                    iconKey = s.suggestedIconKey
                                )
                                currentFolders.add(createdFolder)
                                val pair = newId to candidateName
                                createdByName[key] = pair
                                pair
                            }
                        }
                    }

                    repository.assignChatToFolder(
                        chatId = s.chatId,
                        folderId = folderId,
                        title = s.suggestedTitle.ifBlank { null },
                        summaryTag = s.summaryTag,
                        autoOrganized = true
                    ).getOrThrow()
                    movedCount++
                }

                _statusMessage.value = "Gemini ha organizzato $movedCount chat nelle rispettive cartelle!"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "organizeAllUncategorized failed", e)
                _statusMessage.value = e.localizedMessage ?: "Errore durante l'organizzazione automatica."
            } finally {
                _isOrganizing.value = false
            }
        }
    }

    private suspend fun resolveOrCreateFolder(
        suggestion: AiOrganizationSuggestion,
        existingFolders: List<Folder>
    ): Pair<String, String> {
        val matched = existingFolders.firstOrNull { it.id == suggestion.matchedFolderId }
        if (matched != null) return matched.id to matched.name

        val targetName = suggestion.suggestedNewFolderName.ifBlank { "Generale" }
        val sameName = existingFolders.firstOrNull {
            it.name.trim().equals(targetName.trim(), ignoreCase = true)
        }
        if (sameName != null) return sameName.id to sameName.name

        val newId = repository.createFolder(
            name = targetName,
            description = suggestion.suggestedNewFolderDescription,
            colorKey = suggestion.suggestedColorKey,
            iconKey = suggestion.suggestedIconKey
        ).getOrThrow()
        return newId to targetName
    }

    fun createFolder(
        name: String,
        description: String,
        colorKey: String = FolderColor.COBALT.key,
        iconKey: String = FolderIcon.FOLDER.key
    ) {
        viewModelScope.launch {
            repository.createFolder(name, description, colorKey, iconKey)
                .onSuccess { newId ->
                    _statusMessage.value = "Cartella \"$name\" creata"
                    if (_isChatScreenOpen.value && _activeChatId.value != null) {
                        assignChatToFolder(_activeChatId.value!!, newId, autoOrganized = false)
                    }
                }
                .onFailure { e ->
                    _statusMessage.value = e.localizedMessage ?: "Errore creazione cartella"
                }
        }
    }

    fun updateFolder(
        folderId: String,
        name: String,
        description: String,
        colorKey: String,
        iconKey: String,
        isPinned: Boolean
    ) {
        viewModelScope.launch {
            repository.updateFolder(folderId, name, description, colorKey, iconKey, isPinned)
                .onSuccess {
                    _statusMessage.value = "Cartella aggiornata"
                }
                .onFailure { e ->
                    _statusMessage.value = e.localizedMessage ?: "Errore aggiornamento cartella"
                }
        }
    }

    fun deleteFolder(folderId: String) {
        viewModelScope.launch {
            if ((_folderFilter.value as? FolderFilter.Specific)?.folderId == folderId) {
                _folderFilter.value = FolderFilter.All
            }
            if (_draftFolderId.value == folderId) {
                _draftFolderId.value = ""
            }
            repository.deleteFolder(folderId)
                .onSuccess {
                    _statusMessage.value = "Cartella eliminata (le chat sono ora in Senza cartella)"
                }
                .onFailure { e ->
                    _statusMessage.value = e.localizedMessage ?: "Errore eliminazione cartella"
                }
        }
    }

    fun assignChatToFolder(chatId: String, folderId: String, autoOrganized: Boolean = false) {
        viewModelScope.launch {
            repository.assignChatToFolder(
                chatId = chatId,
                folderId = folderId,
                autoOrganized = autoOrganized
            ).onSuccess {
                val folderName = (foldersState.value as? DataState.Success)?.data
                    ?.firstOrNull { it.id == folderId }?.name ?: "Senza cartella"
                _statusMessage.value = if (folderId.isEmpty()) {
                    "Chat spostata in Senza cartella"
                } else {
                    "Chat spostata in \"$folderName\""
                }
            }.onFailure { e ->
                _statusMessage.value = e.localizedMessage ?: "Errore spostamento chat"
            }
        }
    }

    fun setDraftFolderId(folderId: String) {
        _draftFolderId.value = folderId
    }

    fun renameChat(chatId: String, newTitle: String, isPinned: Boolean) {
        viewModelScope.launch {
            repository.updateChatMeta(chatId, newTitle, isPinned)
                .onFailure { e ->
                    _statusMessage.value = e.localizedMessage ?: "Errore aggiornamento chat"
                }
        }
    }

    fun togglePinChat(chat: ChatThread) {
        viewModelScope.launch {
            repository.updateChatMeta(chat.id, chat.title, !chat.isPinned)
                .onFailure { e ->
                    _statusMessage.value = e.localizedMessage ?: "Errore fissaggio chat"
                }
        }
    }

    fun deleteChat(chatId: String) {
        viewModelScope.launch {
            if (_activeChatId.value == chatId) {
                _activeChatId.value = null
                _isChatScreenOpen.value = false
            }
            repository.deleteChat(chatId)
                .onSuccess {
                    _statusMessage.value = "Chat eliminata"
                }
                .onFailure { e ->
                    _statusMessage.value = e.localizedMessage ?: "Errore eliminazione chat"
                }
        }
    }

    private companion object {
        const val TAG = "WorkspaceViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
