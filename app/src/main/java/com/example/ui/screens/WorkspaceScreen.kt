package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ChatThread
import com.example.data.model.Folder
import com.example.data.model.FolderColor
import com.example.data.model.FolderIcon
import com.example.ui.components.FolderBadgePill
import com.example.ui.components.FolderEditorDialog
import com.example.ui.components.MoveToFolderBottomSheet
import com.example.ui.components.folderIconVector
import com.example.ui.theme.folderSwatch
import com.example.ui.viewmodel.DataState
import com.example.ui.viewmodel.FolderFilter
import com.example.ui.viewmodel.WorkspaceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceScreen(
    viewModel: WorkspaceViewModel,
    userDisplayName: String,
    userEmail: String,
    onSignOut: () -> Unit
) {
    val foldersState by viewModel.foldersState.collectAsStateWithLifecycle()
    val chatsState by viewModel.chatsState.collectAsStateWithLifecycle()
    val folderFilter by viewModel.folderFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val autoOrganizeEnabled by viewModel.autoOrganizeEnabled.collectAsStateWithLifecycle()
    val activeChatId by viewModel.activeChatId.collectAsStateWithLifecycle()
    val isChatOpen by viewModel.isChatScreenOpen.collectAsStateWithLifecycle()
    val draftFolderId by viewModel.draftFolderId.collectAsStateWithLifecycle()
    val activeMessages by viewModel.activeMessages.collectAsStateWithLifecycle()
    val streamingReply by viewModel.streamingReply.collectAsStateWithLifecycle()
    val isSending by viewModel.isSendingMessage.collectAsStateWithLifecycle()
    val isOrganizing by viewModel.isOrganizing.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val chatError by viewModel.chatError.collectAsStateWithLifecycle()

    val folders = (foldersState as? DataState.Success)?.data.orEmpty()
    val chats = (chatsState as? DataState.Success)?.data.orEmpty()
    val activeChat = remember(chats, activeChatId) {
        chats.firstOrNull { it.id == activeChatId }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusMessage) {
        val msg = statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    var showFolderEditorFor by remember { mutableStateOf<Folder?>(null) }
    var isCreatingFolder by remember { mutableStateOf(false) }
    var moveSheetForChat by remember { mutableStateOf<ChatThread?>(null) }
    var renameChatTarget by remember { mutableStateOf<ChatThread?>(null) }
    var deleteFolderConfirm by remember { mutableStateOf<Folder?>(null) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedLayout = maxWidth >= 840.dp

        if (isExpandedLayout) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(0.44f).fillMaxHeight()) {
                    WorkspaceListPane(
                        foldersState = foldersState,
                        chatsState = chatsState,
                        folders = folders,
                        chats = chats,
                        folderFilter = folderFilter,
                        searchQuery = searchQuery,
                        autoOrganizeEnabled = autoOrganizeEnabled,
                        isOrganizing = isOrganizing,
                        selectedChatId = activeChatId,
                        userDisplayName = userDisplayName,
                        userEmail = userEmail,
                        snackbarHostState = snackbarHostState,
                        onSearchChange = viewModel::setSearchQuery,
                        onSelectFilter = viewModel::setFolderFilter,
                        onToggleAutoOrganize = viewModel::toggleAutoOrganize,
                        onBatchOrganize = { includeAll -> viewModel.organizeAllUncategorized(includeAll) },
                        onNewChat = { viewModel.openNewChat() },
                        onOpenChat = { viewModel.openExistingChat(it.id) },
                        onCreateFolderClick = { isCreatingFolder = true },
                        onEditFolderClick = { showFolderEditorFor = it },
                        onDeleteFolderClick = { deleteFolderConfirm = it },
                        onMoveChatClick = { moveSheetForChat = it },
                        onAutoOrganizeChatClick = { viewModel.organizeSingleChat(it.id) },
                        onRenameChatClick = { renameChatTarget = it },
                        onTogglePinChatClick = { viewModel.togglePinChat(it) },
                        onDeleteChatClick = { viewModel.deleteChat(it.id) },
                        onSignOut = onSignOut
                    )
                }
                VerticalDivider()
                Box(modifier = Modifier.weight(0.56f).fillMaxHeight()) {
                    ChatScreen(
                        activeChat = activeChat,
                        folders = folders,
                        draftFolderId = draftFolderId,
                        messages = activeMessages,
                        streamingReply = streamingReply,
                        isSending = isSending,
                        isOrganizing = isOrganizing,
                        chatError = chatError,
                        showBackButton = false,
                        onBack = viewModel::closeChatScreen,
                        onSendMessage = viewModel::sendMessage,
                        onAutoOrganizeChat = viewModel::organizeSingleChat,
                        onAssignFolder = { chatId, folderId ->
                            if (chatId != null) {
                                viewModel.assignChatToFolder(chatId, folderId)
                            } else {
                                viewModel.setDraftFolderId(folderId)
                            }
                        },
                        onCreateFolder = { name, desc, color, icon ->
                            viewModel.createFolder(name, desc, color, icon)
                        },
                        onDismissError = viewModel::clearChatError
                    )
                }
            }
        } else {
            if (isChatOpen) {
                ChatScreen(
                    activeChat = activeChat,
                    folders = folders,
                    draftFolderId = draftFolderId,
                    messages = activeMessages,
                    streamingReply = streamingReply,
                    isSending = isSending,
                    isOrganizing = isOrganizing,
                    chatError = chatError,
                    showBackButton = true,
                    onBack = viewModel::closeChatScreen,
                    onSendMessage = viewModel::sendMessage,
                    onAutoOrganizeChat = viewModel::organizeSingleChat,
                    onAssignFolder = { chatId, folderId ->
                        if (chatId != null) {
                            viewModel.assignChatToFolder(chatId, folderId)
                        } else {
                            viewModel.setDraftFolderId(folderId)
                        }
                    },
                    onCreateFolder = { name, desc, color, icon ->
                        viewModel.createFolder(name, desc, color, icon)
                    },
                    onDismissError = viewModel::clearChatError
                )
            } else {
                WorkspaceListPane(
                    foldersState = foldersState,
                    chatsState = chatsState,
                    folders = folders,
                    chats = chats,
                    folderFilter = folderFilter,
                    searchQuery = searchQuery,
                    autoOrganizeEnabled = autoOrganizeEnabled,
                    isOrganizing = isOrganizing,
                    selectedChatId = activeChatId,
                    userDisplayName = userDisplayName,
                    userEmail = userEmail,
                    snackbarHostState = snackbarHostState,
                    onSearchChange = viewModel::setSearchQuery,
                    onSelectFilter = viewModel::setFolderFilter,
                    onToggleAutoOrganize = viewModel::toggleAutoOrganize,
                    onBatchOrganize = { includeAll -> viewModel.organizeAllUncategorized(includeAll) },
                    onNewChat = { viewModel.openNewChat() },
                    onOpenChat = { viewModel.openExistingChat(it.id) },
                    onCreateFolderClick = { isCreatingFolder = true },
                    onEditFolderClick = { showFolderEditorFor = it },
                    onDeleteFolderClick = { deleteFolderConfirm = it },
                    onMoveChatClick = { moveSheetForChat = it },
                    onAutoOrganizeChatClick = { viewModel.organizeSingleChat(it.id) },
                    onRenameChatClick = { renameChatTarget = it },
                    onTogglePinChatClick = { viewModel.togglePinChat(it) },
                    onDeleteChatClick = { viewModel.deleteChat(it.id) },
                    onSignOut = onSignOut
                )
            }
        }
    }

    if (isCreatingFolder || showFolderEditorFor != null) {
        val editing = showFolderEditorFor
        FolderEditorDialog(
            initialFolder = editing,
            onDismiss = {
                isCreatingFolder = false
                showFolderEditorFor = null
            },
            onConfirm = { name, description, colorKey, iconKey, isPinned ->
                if (editing == null) {
                    viewModel.createFolder(name, description, colorKey, iconKey)
                } else {
                    viewModel.updateFolder(editing.id, name, description, colorKey, iconKey, isPinned)
                }
                isCreatingFolder = false
                showFolderEditorFor = null
            }
        )
    }

    val moveChat = moveSheetForChat
    if (moveChat != null) {
        MoveToFolderBottomSheet(
            folders = folders,
            currentFolderId = moveChat.folderId,
            canAutoOrganizeWithGemini = moveChat.messageCount > 0,
            onSelectFolder = { folderId ->
                viewModel.assignChatToFolder(moveChat.id, folderId)
            },
            onAutoOrganizeWithGemini = {
                viewModel.organizeSingleChat(moveChat.id)
            },
            onCreateNewFolder = { isCreatingFolder = true },
            onDismiss = { moveSheetForChat = null }
        )
    }

    val renameTarget = renameChatTarget
    if (renameTarget != null) {
        var newTitle by remember(renameTarget) { mutableStateOf(renameTarget.title) }
        AlertDialog(
            onDismissRequest = { renameChatTarget = null },
            title = { Text("Rinomina chat") },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { if (it.length <= 120) newTitle = it },
                    label = { Text("Titolo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.trim().isNotEmpty()) {
                            viewModel.renameChat(renameTarget.id, newTitle.trim(), renameTarget.isPinned)
                            renameChatTarget = null
                        }
                    },
                    enabled = newTitle.trim().isNotEmpty()
                ) {
                    Text("Salva")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameChatTarget = null }) {
                    Text("Annulla")
                }
            }
        )
    }

    val folderToDelete = deleteFolderConfirm
    if (folderToDelete != null) {
        AlertDialog(
            onDismissRequest = { deleteFolderConfirm = null },
            title = { Text("Eliminare \"${folderToDelete.name}\"?") },
            text = {
                Text("La cartella verrà rimossa, ma le sue chat non saranno eliminate e passeranno in \"Senza cartella\".")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteFolder(folderToDelete.id)
                        deleteFolderConfirm = null
                    }
                ) {
                    Text("Elimina cartella")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteFolderConfirm = null }) {
                    Text("Annulla")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkspaceListPane(
    foldersState: DataState<List<Folder>>,
    chatsState: DataState<List<ChatThread>>,
    folders: List<Folder>,
    chats: List<ChatThread>,
    folderFilter: FolderFilter,
    searchQuery: String,
    autoOrganizeEnabled: Boolean,
    isOrganizing: Boolean,
    selectedChatId: String?,
    userDisplayName: String,
    userEmail: String,
    snackbarHostState: SnackbarHostState,
    onSearchChange: (String) -> Unit,
    onSelectFilter: (FolderFilter) -> Unit,
    onToggleAutoOrganize: (Boolean) -> Unit,
    onBatchOrganize: (includeAlreadyOrganized: Boolean) -> Unit,
    onNewChat: () -> Unit,
    onOpenChat: (ChatThread) -> Unit,
    onCreateFolderClick: () -> Unit,
    onEditFolderClick: (Folder) -> Unit,
    onDeleteFolderClick: (Folder) -> Unit,
    onMoveChatClick: (ChatThread) -> Unit,
    onAutoOrganizeChatClick: (ChatThread) -> Unit,
    onRenameChatClick: (ChatThread) -> Unit,
    onTogglePinChatClick: (ChatThread) -> Unit,
    onDeleteChatClick: (ChatThread) -> Unit,
    onSignOut: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    var showAccountMenu by remember { mutableStateOf(false) }

    val uncategorizedCount = remember(chats) { chats.count { it.folderId.isEmpty() } }
    val folderCounts = remember(chats) {
        chats.filter { it.folderId.isNotEmpty() }.groupingBy { it.folderId }.eachCount()
    }
    val folderMap = remember(folders) { folders.associateBy { it.id } }

    val filteredChats = remember(chats, folderFilter, searchQuery) {
        val byFolder = when (folderFilter) {
            FolderFilter.All -> chats
            FolderFilter.Uncategorized -> chats.filter { it.folderId.isEmpty() }
            is FolderFilter.Specific -> chats.filter { it.folderId == folderFilter.folderId }
        }
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) byFolder else byFolder.filter {
            it.title.lowercase().contains(q) ||
                it.lastMessagePreview.lowercase().contains(q) ||
                it.summaryTag.lowercase().contains(q)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "GemFolders",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "${folders.size} cartelle • ${chats.size} chat",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onCreateFolderClick,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("create_folder_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CreateNewFolder,
                            contentDescription = "Nuova cartella"
                        )
                    }

                    Box {
                        val initials = remember(userDisplayName, userEmail) {
                            (userDisplayName.ifBlank { userEmail }).firstOrNull()?.uppercase() ?: "U"
                        }
                        Surface(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(38.dp)
                                .clip(CircleShape)
                                .clickable { showAccountMenu = true }
                                .testTag("account_menu_button"),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = initials,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showAccountMenu,
                            onDismissRequest = { showAccountMenu = false }
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                                if (userDisplayName.isNotBlank()) {
                                    Text(
                                        text = userDisplayName,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                }
                                if (userEmail.isNotBlank()) {
                                    Text(
                                        text = userEmail,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text("Auto-organizza nuove chat", modifier = Modifier.weight(1f))
                                        Switch(
                                            checked = autoOrganizeEnabled,
                                            onCheckedChange = onToggleAutoOrganize
                                        )
                                    }
                                },
                                onClick = { onToggleAutoOrganize(!autoOrganizeEnabled) }
                            )
                            if (chats.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Riorganizza tutte le chat con Gemini") },
                                    leadingIcon = {
                                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                                    },
                                    onClick = {
                                        showAccountMenu = false
                                        onBatchOrganize(true)
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Esci dall'account") },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                                },
                                onClick = {
                                    showAccountMenu = false
                                    onSignOut()
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewChat,
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("Nuova chat") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("new_chat_fab")
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "search_bar") {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Cerca chat o argomenti...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Pulisci ricerca")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("search_chats_input")
                )
            }

            item(key = "smart_organizer_banner") {
                AnimatedVisibility(visible = uncategorizedCount > 0 || isOrganizing) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isOrganizing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isOrganizing) {
                                        "Gemini sta organizzando..."
                                    } else {
                                        "$uncategorizedCount chat senza cartella"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Lascia che Gemini le raggruppi per argomento in un tocco.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                )
                            }
                            Button(
                                onClick = { onBatchOrganize(false) },
                                enabled = !isOrganizing && uncategorizedCount > 0,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("batch_organize_button")
                            ) {
                                Text("Organizza")
                            }
                        }
                    }
                }
            }

            item(key = "folders_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Le tue cartelle",
                        style = MaterialTheme.typography.titleMedium
                    )
                    TextButton(
                        onClick = onCreateFolderClick,
                        modifier = Modifier.testTag("new_folder_inline_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Nuova")
                    }
                }
            }

            item(key = "folders_carousel") {
                if (foldersState is DataState.Loading) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        Text("Caricamento cartelle...", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(folders, key = { it.id }) { folder ->
                            val isSelected = (folderFilter as? FolderFilter.Specific)?.folderId == folder.id
                            val swatch = folderSwatch(FolderColor.fromKey(folder.colorKey), isDark)
                            val count = folderCounts[folder.id] ?: 0
                            var menuExpanded by remember { mutableStateOf(false) }

                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = if (isSelected) swatch.container else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) swatch.accent else MaterialTheme.colorScheme.outlineVariant
                                ),
                                tonalElevation = if (isSelected) 2.dp else 0.dp,
                                modifier = Modifier
                                    .width(172.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable {
                                        onSelectFilter(
                                            if (isSelected) FolderFilter.All else FolderFilter.Specific(folder.id)
                                        )
                                    }
                                    .testTag("folder_card_${folder.id}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(swatch.container),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = folderIconVector(FolderIcon.fromKey(folder.iconKey)),
                                                contentDescription = null,
                                                tint = swatch.accent,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Box {
                                            IconButton(
                                                onClick = { menuExpanded = true },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.MoreVert,
                                                    contentDescription = "Opzioni cartella",
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            DropdownMenu(
                                                expanded = menuExpanded,
                                                onDismissRequest = { menuExpanded = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("Modifica cartella") },
                                                    leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                                                    onClick = {
                                                        menuExpanded = false
                                                        onEditFolderClick(folder)
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Elimina cartella") },
                                                    leadingIcon = { Icon(Icons.Rounded.DeleteOutline, null) },
                                                    onClick = {
                                                        menuExpanded = false
                                                        onDeleteFolderClick(folder)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = folder.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "$count ${if (count == 1) "chat" else "chat"}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        item(key = "add_folder_card") {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .width(136.dp)
                                    .height(114.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable(onClick = onCreateFolderClick)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(14.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CreateNewFolder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = "Nuova cartella",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item(key = "filter_chips_row") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = folderFilter == FolderFilter.All,
                            onClick = { onSelectFilter(FolderFilter.All) },
                            label = { Text("Tutte (${chats.size})") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = folderFilter == FolderFilter.Uncategorized,
                            onClick = { onSelectFilter(FolderFilter.Uncategorized) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.FolderOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text("Senza cartella ($uncategorizedCount)") }
                        )
                    }
                    items(folders, key = { "chip_${it.id}" }) { folder ->
                        val selected = (folderFilter as? FolderFilter.Specific)?.folderId == folder.id
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectFilter(FolderFilter.Specific(folder.id)) },
                            label = { Text(folder.name) }
                        )
                    }
                }
            }

            if (chatsState is DataState.Error) {
                item(key = "chats_error") {
                    Surface(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = chatsState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else if (chatsState is DataState.Loading) {
                item(key = "chats_loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (filteredChats.isEmpty()) {
                item(key = "chats_empty") {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Chat,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = if (searchQuery.isNotBlank()) {
                                    "Nessuna chat trovata"
                                } else {
                                    "Nessuna chat in questa vista"
                                },
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Avvia una nuova chat con Gemini: verrà salvata e organizzata nelle tue cartelle.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(onClick = onNewChat) {
                                Icon(Icons.Rounded.Add, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Inizia una chat con Gemini")
                            }
                        }
                    }
                }
            } else {
                items(filteredChats, key = { it.id }) { chat ->
                    val chatFolder = folderMap[chat.folderId]
                    val isCurrent = chat.id == selectedChatId
                    ChatThreadCard(
                        chat = chat,
                        folder = chatFolder,
                        isSelected = isCurrent,
                        isOrganizing = isOrganizing,
                        onOpen = { onOpenChat(chat) },
                        onMoveToFolder = { onMoveChatClick(chat) },
                        onAutoOrganize = { onAutoOrganizeChatClick(chat) },
                        onRename = { onRenameChatClick(chat) },
                        onTogglePin = { onTogglePinChatClick(chat) },
                        onDelete = { onDeleteChatClick(chat) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatThreadCard(
    chat: ChatThread,
    folder: Folder?,
    isSelected: Boolean,
    isOrganizing: Boolean,
    onOpen: () -> Unit,
    onMoveToFolder: () -> Unit,
    onAutoOrganize: () -> Unit,
    onRename: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onOpen)
            .testTag("chat_card_${chat.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (chat.isPinned) {
                        Icon(
                            imageVector = Icons.Rounded.PushPin,
                            contentDescription = "Fissata",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = chat.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier
                            .size(36.dp)
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "Azioni chat",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Organizza con Gemini") },
                            leadingIcon = { Icon(Icons.Rounded.AutoAwesome, null) },
                            enabled = !isOrganizing && chat.messageCount > 0,
                            onClick = {
                                menuExpanded = false
                                onAutoOrganize()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sposta in cartella") },
                            leadingIcon = { Icon(Icons.Rounded.DriveFileMove, null) },
                            onClick = {
                                menuExpanded = false
                                onMoveToFolder()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (chat.isPinned) "Rimuovi da fissate" else "Fissa in alto") },
                            leadingIcon = { Icon(Icons.Rounded.PushPin, null) },
                            onClick = {
                                menuExpanded = false
                                onTogglePin()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Rinomina") },
                            leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                            onClick = {
                                menuExpanded = false
                                onRename()
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Elimina chat") },
                            leadingIcon = { Icon(Icons.Rounded.DeleteOutline, null) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            if (chat.lastMessagePreview.isNotBlank()) {
                Text(
                    text = chat.lastMessagePreview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FolderBadgePill(
                    folder = folder,
                    onClick = onMoveToFolder
                )
                if (chat.summaryTag.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "#${chat.summaryTag}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                if (chat.autoOrganized) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = "Organizzata automaticamente da Gemini",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = "${chat.messageCount} msg",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
