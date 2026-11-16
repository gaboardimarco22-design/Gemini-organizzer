package com.example.data.ai

import android.util.Log
import com.example.data.model.AiOrganizationSuggestion
import com.example.data.model.ChatMessage
import com.example.data.model.ChatThread
import com.example.data.model.Folder
import com.example.data.model.FolderColor
import com.example.data.model.FolderIcon
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

class GeminiRepository {

    private val chatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            systemInstruction = content {
                text(
                    "Sei l'assistente AI integrato in GemFolders. " +
                        "Rispondi in modo chiaro, accurato e ben strutturato nella lingua dell'utente " +
                        "(italiano di default). Quando appropriato usa elenchi puntati essenziali o brevi blocchi di codice."
                )
            }
        )
    }

    private val singleOrganizeSchema = Schema.obj(
        properties = mapOf(
            "suggestedTitle" to Schema.string(
                description = "Titolo breve e chiaro della chat (2-5 parole)."
            ),
            "summaryTag" to Schema.string(
                description = "Etichetta tematica sintetica di 1-2 parole (es. Kotlin, Cucina, Viaggi)."
            ),
            "matchedFolderId" to Schema.string(
                description = "L'ID esatto di una cartella esistente adatta, oppure stringa vuota \"\" se serve una nuova cartella."
            ),
            "suggestedNewFolderName" to Schema.string(
                description = "Nome della nuova cartella da creare se matchedFolderId è vuoto (1-3 parole), altrimenti stringa vuota."
            ),
            "suggestedNewFolderDescription" to Schema.string(
                description = "Breve descrizione della nuova cartella (max 1 frase), oppure stringa vuota."
            ),
            "suggestedColorKey" to Schema.enumeration(
                values = FolderColor.entries.map { it.key },
                description = "Chiave colore più adatta al tema della cartella."
            ),
            "suggestedIconKey" to Schema.enumeration(
                values = FolderIcon.entries.map { it.key },
                description = "Chiave icona più adatta al tema della cartella."
            ),
            "reason" to Schema.string(
                description = "Spiegazione brevissima in italiano (max 12 parole) del perché di questa classificazione."
            )
        )
    )

    private val singleOrganizeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = singleOrganizeSchema
            }
        )
    }

    private val batchOrganizeSchema = Schema.obj(
        properties = mapOf(
            "assignments" to Schema.array(
                description = "Lista di assegnazioni per ogni chat analizzata.",
                items = Schema.obj(
                    properties = mapOf(
                        "chatId" to Schema.string(description = "ID esatto della chat."),
                        "suggestedTitle" to Schema.string(description = "Titolo sintetico migliorato (2-5 parole)."),
                        "summaryTag" to Schema.string(description = "Tag tematico (1-2 parole)."),
                        "matchedFolderId" to Schema.string(description = "ID esatto di una cartella esistente oppure \"\" se serve crearne una nuova."),
                        "suggestedNewFolderName" to Schema.string(description = "Nome nuova cartella se matchedFolderId è \"\"."),
                        "suggestedNewFolderDescription" to Schema.string(description = "Breve descrizione della nuova cartella."),
                        "suggestedColorKey" to Schema.enumeration(
                            values = FolderColor.entries.map { it.key },
                            description = "Colore suggerito."
                        ),
                        "suggestedIconKey" to Schema.enumeration(
                            values = FolderIcon.entries.map { it.key },
                            description = "Icona suggerita."
                        ),
                        "reason" to Schema.string(description = "Motivo breve in italiano.")
                    )
                )
            )
        )
    )

    private val batchOrganizeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = batchOrganizeSchema
            }
        )
    }

    fun streamChatReply(
        history: List<ChatMessage>,
        newUserMessage: String
    ): Flow<String> = flow {
        val sdkHistory = history.takeLast(MAX_HISTORY_MESSAGES).map { msg ->
            content(role = if (msg.role == "model") "model" else "user") {
                text(msg.text)
            }
        }
        val chat = chatModel.startChat(history = sdkHistory)
        val accumulated = StringBuilder()
        try {
            withTimeout(AI_TIMEOUT_MS) {
                chat.sendMessageStream(newUserMessage).collect { chunk ->
                    val delta = chunk.text.orEmpty()
                    if (delta.isNotEmpty()) {
                        accumulated.append(delta)
                        emit(accumulated.toString())
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("Gemini ha restituito una risposta vuota.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Chat streaming timed out", e)
            throw IllegalStateException("Richiesta scaduta dopo 60 secondi. Riprova.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Chat streaming failed", e)
            throw e
        }
    }

    suspend fun suggestFolderForChat(
        chatId: String,
        messages: List<ChatMessage>,
        existingFolders: List<Folder>
    ): AiOrganizationSuggestion {
        val validFolderIds = existingFolders.map { it.id }.toSet()
        val foldersContext = if (existingFolders.isEmpty()) {
            "Nessuna cartella esistente."
        } else {
            existingFolders.joinToString("\n") { folder ->
                "- id=\"${folder.id}\" | nome=\"${folder.name}\" | descrizione=\"${folder.description}\""
            }
        }
        val transcript = messages.take(10).joinToString("\n") { msg ->
            val who = if (msg.role == "model") "Gemini" else "Utente"
            "$who: ${msg.text.take(400)}"
        }

        val prompt = """
            Analizza questa conversazione e scegli la cartella migliore tra quelle esistenti, oppure proponi una nuova cartella chiara e riusabile in italiano.
            
            CARTELLERIA ESISTENTE:
            $foldersContext
            
            CONVERSAZIONE:
            $transcript
            
            Regole:
            - Se una cartella esistente è già pertinente, imposta matchedFolderId al suo id esatto e lascia suggestedNewFolderName vuoto "".
            - Se nessuna cartella esistente è adatta (o non ce ne sono), imposta matchedFolderId a "" e proponi suggestedNewFolderName (es. "Sviluppo & Codice", "Lavoro", "Viaggi", "Cucina", "Studio", "Idee Creative").
            - Genera sempre suggestedTitle (2-5 parole in italiano) e summaryTag (1-2 parole).
        """.trimIndent()

        try {
            val response = withTimeout(AI_TIMEOUT_MS) {
                singleOrganizeModel.generateContent(prompt)
            }
            val raw = response.text ?: throw IllegalStateException("Risposta JSON vuota da Gemini.")
            val json = JSONObject(raw)
            val matchedId = json.optString("matchedFolderId", "").trim()
                .takeIf { it in validFolderIds } ?: ""

            return AiOrganizationSuggestion(
                chatId = chatId,
                suggestedTitle = json.optString("suggestedTitle", "Chat Gemini").trim().ifEmpty { "Chat Gemini" }.take(120),
                summaryTag = json.optString("summaryTag", "").trim().take(40),
                matchedFolderId = matchedId,
                suggestedNewFolderName = if (matchedId.isEmpty()) {
                    json.optString("suggestedNewFolderName", "Generale").trim().ifEmpty { "Generale" }.take(50)
                } else {
                    ""
                },
                suggestedNewFolderDescription = json.optString("suggestedNewFolderDescription", "").trim().take(200),
                suggestedColorKey = FolderColor.fromKey(json.optString("suggestedColorKey", "cobalt")).key,
                suggestedIconKey = FolderIcon.fromKey(json.optString("suggestedIconKey", "folder")).key,
                reason = json.optString("reason", "Organizzata automaticamente da Gemini").trim()
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Folder suggestion timed out", e)
            throw IllegalStateException("Classificazione Gemini scaduta dopo 60 secondi.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Folder suggestion failed", e)
            throw e
        }
    }

    suspend fun batchOrganizeChats(
        chats: List<ChatThread>,
        existingFolders: List<Folder>
    ): List<AiOrganizationSuggestion> {
        if (chats.isEmpty()) return emptyList()
        val validFolderIds = existingFolders.map { it.id }.toSet()
        val validChatIds = chats.map { it.id }.toSet()

        val foldersContext = if (existingFolders.isEmpty()) {
            "Nessuna cartella esistente."
        } else {
            existingFolders.joinToString("\n") { folder ->
                "- id=\"${folder.id}\" | nome=\"${folder.name}\" | descrizione=\"${folder.description}\""
            }
        }
        val chatsContext = chats.take(25).joinToString("\n") { chat ->
            "- chatId=\"${chat.id}\" | titolo=\"${chat.title}\" | anteprima=\"${chat.lastMessagePreview}\" | tag=\"${chat.summaryTag}\""
        }

        val prompt = """
            Organizza le seguenti chat di Gemini in cartelle tematiche coerenti in italiano.
            Raggruppa chat simili sotto la stessa cartella esistente o sotto lo stesso nome di nuova cartella quando condividono il tema.
            
            CARTELLE ESISTENTI:
            $foldersContext
            
            CHAT DA ORGANIZZARE:
            $chatsContext
            
            Restituisci un elemento in "assignments" per ciascun chatId.
        """.trimIndent()

        try {
            val response = withTimeout(AI_TIMEOUT_MS) {
                batchOrganizeModel.generateContent(prompt)
            }
            val raw = response.text ?: throw IllegalStateException("Risposta JSON vuota da Gemini.")
            val root = JSONObject(raw)
            val arr = root.optJSONArray("assignments") ?: return emptyList()

            val results = mutableListOf<AiOrganizationSuggestion>()
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                val chatId = item.optString("chatId", "").trim()
                if (chatId !in validChatIds) continue
                val matchedId = item.optString("matchedFolderId", "").trim()
                    .takeIf { it in validFolderIds } ?: ""

                results.add(
                    AiOrganizationSuggestion(
                        chatId = chatId,
                        suggestedTitle = item.optString("suggestedTitle", "").trim().take(120),
                        summaryTag = item.optString("summaryTag", "").trim().take(40),
                        matchedFolderId = matchedId,
                        suggestedNewFolderName = if (matchedId.isEmpty()) {
                            item.optString("suggestedNewFolderName", "Generale").trim().ifEmpty { "Generale" }.take(50)
                        } else {
                            ""
                        },
                        suggestedNewFolderDescription = item.optString("suggestedNewFolderDescription", "").trim().take(200),
                        suggestedColorKey = FolderColor.fromKey(item.optString("suggestedColorKey", "cobalt")).key,
                        suggestedIconKey = FolderIcon.fromKey(item.optString("suggestedIconKey", "folder")).key,
                        reason = item.optString("reason", "Organizzata da Gemini").trim()
                    )
                )
            }
            return results
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Batch organize timed out", e)
            throw IllegalStateException("Organizzazione batch scaduta dopo 60 secondi.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Batch organize failed", e)
            throw e
        }
    }

    private companion object {
        const val TAG = "GeminiRepository"
        const val MODEL_NAME = "gemini-3.8-flash"
        const val AI_TIMEOUT_MS = 60_000L
        const val MAX_HISTORY_MESSAGES = 24
    }
}
