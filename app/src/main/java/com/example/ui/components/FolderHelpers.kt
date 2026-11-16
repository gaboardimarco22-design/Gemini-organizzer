package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.WorkOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Folder
import com.example.data.model.FolderColor
import com.example.data.model.FolderIcon
import com.example.ui.theme.folderSwatch

fun folderIconVector(icon: FolderIcon): ImageVector = when (icon) {
    FolderIcon.FOLDER -> Icons.Rounded.Folder
    FolderIcon.CODE -> Icons.Rounded.Code
    FolderIcon.WORK -> Icons.Rounded.WorkOutline
    FolderIcon.SCHOOL -> Icons.Rounded.School
    FolderIcon.LIGHTBULB -> Icons.Rounded.Lightbulb
    FolderIcon.TRAVEL -> Icons.Rounded.Flight
    FolderIcon.HEALTH -> Icons.Rounded.FavoriteBorder
    FolderIcon.STAR -> Icons.Rounded.Star
}

@Composable
fun FolderBadgePill(
    folder: Folder?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val isDark = isSystemInDarkTheme()
    val swatch = if (folder != null) {
        folderSwatch(FolderColor.fromKey(folder.colorKey), isDark)
    } else null

    val containerColor = swatch?.container ?: MaterialTheme.colorScheme.surfaceVariant
    val contentColor = swatch?.onContainer ?: MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = swatch?.border ?: MaterialTheme.colorScheme.outlineVariant
    val icon = if (folder != null) {
        folderIconVector(FolderIcon.fromKey(folder.iconKey))
    } else {
        Icons.Rounded.FolderOff
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(50),
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = swatch?.accent ?: contentColor
            )
            Text(
                text = folder?.name ?: "Senza cartella",
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FolderEditorDialog(
    initialFolder: Folder?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String, colorKey: String, iconKey: String, isPinned: Boolean) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    var name by remember(initialFolder) { mutableStateOf(initialFolder?.name.orEmpty()) }
    var description by remember(initialFolder) { mutableStateOf(initialFolder?.description.orEmpty()) }
    var colorKey by remember(initialFolder) {
        mutableStateOf(initialFolder?.colorKey ?: FolderColor.COBALT.key)
    }
    var iconKey by remember(initialFolder) {
        mutableStateOf(initialFolder?.iconKey ?: FolderIcon.FOLDER.key)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialFolder == null) "Nuova cartella" else "Modifica cartella",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 50) name = it },
                    label = { Text("Nome cartella") },
                    placeholder = { Text("es. Sviluppo Kotlin, Viaggi...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("folder_name_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 200) description = it },
                    label = { Text("Descrizione per Gemini (opzionale)") },
                    placeholder = { Text("Aiuta Gemini a capire quali chat inserire qui") },
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("folder_description_input")
                )
                Text(
                    text = "Colore",
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FolderColor.entries.forEach { color ->
                        val swatch = folderSwatch(color, isDark)
                        val selected = color.key == colorKey
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(swatch.accent)
                                .then(
                                    if (selected) {
                                        Modifier.border(
                                            width = 2.5.dp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            shape = CircleShape
                                        )
                                    } else Modifier
                                )
                                .clickable { colorKey = color.key },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = color.labelIt,
                                    tint = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
                Text(
                    text = "Icona",
                    style = MaterialTheme.typography.labelLarge
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val activeSwatch = folderSwatch(FolderColor.fromKey(colorKey), isDark)
                    FolderIcon.entries.forEach { icon ->
                        val selected = icon.key == iconKey
                        Surface(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { iconKey = icon.key },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) activeSwatch.container else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (selected) BorderStroke(1.5.dp, activeSwatch.accent) else null
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = folderIconVector(icon),
                                    contentDescription = icon.labelIt,
                                    tint = if (selected) activeSwatch.accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.trim().isNotEmpty()) {
                        onConfirm(
                            name.trim(),
                            description.trim(),
                            colorKey,
                            iconKey,
                            initialFolder?.isPinned ?: false
                        )
                    }
                },
                enabled = name.trim().isNotEmpty(),
                modifier = Modifier.testTag("save_folder_button")
            ) {
                Text(if (initialFolder == null) "Crea" else "Salva")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveToFolderBottomSheet(
    folders: List<Folder>,
    currentFolderId: String,
    canAutoOrganizeWithGemini: Boolean,
    onSelectFolder: (String) -> Unit,
    onAutoOrganizeWithGemini: (() -> Unit)?,
    onCreateNewFolder: () -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Sposta in cartella",
                style = MaterialTheme.typography.headlineSmall
            )
            if (canAutoOrganizeWithGemini && onAutoOrganizeWithGemini != null) {
                Button(
                    onClick = {
                        onDismiss()
                        onAutoOrganizeWithGemini()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("sheet_auto_organize_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Lascia scegliere a Gemini")
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        onSelectFolder("")
                        onDismiss()
                    },
                shape = RoundedCornerShape(14.dp),
                color = if (currentFolderId.isEmpty()) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FolderOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Senza cartella",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f)
                    )
                    if (currentFolderId.isEmpty()) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Selezionata",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            folders.forEach { folder ->
                val swatch = folderSwatch(FolderColor.fromKey(folder.colorKey), isDark)
                val selected = folder.id == currentFolderId
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            onSelectFolder(folder.id)
                            onDismiss()
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected) swatch.container else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = if (selected) BorderStroke(1.dp, swatch.accent) else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(swatch.container),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = folderIconVector(FolderIcon.fromKey(folder.iconKey)),
                                contentDescription = null,
                                tint = swatch.accent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = folder.name,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (folder.description.isNotBlank()) {
                                Text(
                                    text = folder.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (selected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Selezionata",
                                tint = swatch.accent
                            )
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    onDismiss()
                    onCreateNewFolder()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Rounded.CreateNewFolder, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Nuova cartella")
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}
