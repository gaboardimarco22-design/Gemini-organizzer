package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class FolderColor(val key: String, val labelIt: String) {
    COBALT("cobalt", "Cobalto"),
    AMBER("amber", "Ambra"),
    EMERALD("emerald", "Smeraldo"),
    VIOLET("violet", "Viola"),
    CORAL("coral", "Corallo"),
    TEAL("teal", "Turchese");

    companion object {
        fun fromKey(key: String): FolderColor =
            entries.firstOrNull { it.key == key } ?: COBALT
    }
}

enum class FolderIcon(val key: String, val labelIt: String) {
    FOLDER("folder", "Cartella"),
    CODE("code", "Codice"),
    WORK("work", "Lavoro"),
    SCHOOL("school", "Studio"),
    LIGHTBULB("lightbulb", "Idee"),
    TRAVEL("travel", "Viaggi"),
    HEALTH("health", "Salute"),
    STAR("star", "Preferiti");

    companion object {
        fun fromKey(key: String): FolderIcon =
            entries.firstOrNull { it.key == key } ?: FOLDER
    }
}

@IgnoreExtraProperties
data class Folder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val colorKey: String = FolderColor.COBALT.key,
    val iconKey: String = FolderIcon.FOLDER.key,
    val isPinned: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChatThread(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val folderId: String = "",
    val lastMessagePreview: String = "",
    val autoOrganized: Boolean = false,
    val summaryTag: String = "",
    val messageCount: Long = 0L,
    val isPinned: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val chatId: String = "",
    val role: String = "user",
    val text: String = "",
    val sequence: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class AiOrganizationSuggestion(
    val chatId: String,
    val suggestedTitle: String,
    val summaryTag: String,
    val matchedFolderId: String,
    val suggestedNewFolderName: String,
    val suggestedNewFolderDescription: String,
    val suggestedColorKey: String,
    val suggestedIconKey: String,
    val reason: String
)
