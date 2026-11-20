package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.OfflinePin
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.AudioTrack
import com.example.data.model.Playlist
import com.example.data.model.SonoraCatalog
import com.example.data.model.UiState
import com.example.ui.components.CoverArtThumbnail
import com.example.ui.components.GlassCard
import com.example.ui.components.formatDurationMs
import com.example.ui.theme.LocalSonoraDesign

@Composable
fun PlaylistsScreen(
    playlistsState: UiState<List<Playlist>>,
    tracksState: UiState<List<AudioTrack>>,
    onOpenPlaylist: (String) -> Unit,
    onPlayPlaylist: (Playlist, List<AudioTrack>) -> Unit,
    onCreatePlaylist: (String, String, String, Boolean) -> Unit,
    onTogglePlaylistOffline: (Playlist) -> Unit,
    onDeletePlaylist: (Playlist) -> Unit,
    contentBottomPadding: Dp
) {
    val design = LocalSonoraDesign.current
    var showCreateDialog by remember { mutableStateOf(false) }

    val allTracks = (tracksState as? UiState.Success)?.data.orEmpty()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Tus Listas de Reproducción",
                    style = MaterialTheme.typography.titleLarge,
                    color = design.textPrimary
                )
                Text(
                    text = "Organiza, ordena y fija para modo offline",
                    style = MaterialTheme.typography.bodySmall,
                    color = design.textSecondary
                )
            }
            Button(
                onClick = { showCreateDialog = true },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = design.accentGlow,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("create_playlist_button")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Nueva", style = MaterialTheme.typography.labelLarge)
            }
        }

        when (playlistsState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = design.accentGlow)
                }
            }
            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    GlassCard {
                        Text(playlistsState.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(20.dp))
                    }
                }
            }
            is UiState.Success -> {
                val playlists = playlistsState.data
                if (playlists.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        GlassCard(cornerRadius = 28.dp) {
                            Column(
                                modifier = Modifier.padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Rounded.QueueMusic, contentDescription = null, tint = design.accentGlow, modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(10.dp))
                                Text("Sin listas aún", style = MaterialTheme.typography.titleMedium, color = design.textPrimary)
                                Spacer(Modifier.height(6.dp))
                                Text("Crea tu primera lista Hi-Res y sincronízala en todos tus dispositivos.", style = MaterialTheme.typography.bodySmall, color = design.textSecondary)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = contentBottomPadding + 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(playlists, key = { it.id }) { playlist ->
                            val resolvedTracks = remember(playlist.trackIds, allTracks) {
                                playlist.trackIds.mapNotNull { id -> allTracks.firstOrNull { it.id == id } }
                            }
                            val totalDuration = resolvedTracks.sumOf { it.durationMs }

                            GlassCard(
                                elevated = true,
                                cornerRadius = 24.dp,
                                onClick = { onOpenPlaylist(playlist.id) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("playlist_card_${playlist.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CoverArtThumbnail(
                                        coverKey = playlist.coverKey,
                                        contentDescription = playlist.name,
                                        modifier = Modifier.size(72.dp),
                                        cornerRadius = 18.dp
                                    )
                                    Spacer(Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = playlist.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = design.textPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            if (playlist.offlinePinned) {
                                                Icon(
                                                    imageVector = Icons.Rounded.OfflinePin,
                                                    contentDescription = "Fijada Offline",
                                                    tint = design.cloudBadgeColor,
                                                    modifier = Modifier.size(17.dp)
                                                )
                                            }
                                        }
                                        if (playlist.description.isNotBlank()) {
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                text = playlist.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = design.textSecondary,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            text = "${resolvedTracks.size} pistas • ${formatDurationMs(totalDuration)}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = design.accentGlow
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        IconButton(
                                            onClick = { onPlayPlaylist(playlist, resolvedTracks) },
                                            enabled = resolvedTracks.isNotEmpty(),
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.PlayArrow,
                                                contentDescription = "Reproducir lista",
                                                tint = design.accentGlow
                                            )
                                        }
                                        Row {
                                            IconButton(
                                                onClick = { onTogglePlaylistOffline(playlist) },
                                                modifier = Modifier.minimumInteractiveComponentSize()
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.OfflinePin,
                                                    contentDescription = "Alternar offline",
                                                    tint = if (playlist.offlinePinned) design.cloudBadgeColor else design.textSecondary.copy(alpha = 0.5f)
                                                )
                                            }
                                            IconButton(
                                                onClick = { onDeletePlaylist(playlist) },
                                                modifier = Modifier.minimumInteractiveComponentSize()
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.DeleteOutline,
                                                    contentDescription = "Eliminar lista",
                                                    tint = design.textSecondary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, desc, coverKey, offline ->
                onCreatePlaylist(name, desc, coverKey, offline)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String, Boolean) -> Unit
) {
    val design = LocalSonoraDesign.current
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCover by remember { mutableStateOf("liquid_aurora") }
    var offlinePinned by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva Lista de Reproducción") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(60) },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("playlist_name_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it.take(240) },
                    label = { Text("Descripción (opcional)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Portada artística", style = MaterialTheme.typography.labelLarge)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SonoraCatalog.coverKeys.forEach { key ->
                        val isSelected = selectedCover == key
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) design.accentGlow else Color.Transparent,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { selectedCover = key }
                        ) {
                            CoverArtThumbnail(
                                coverKey = key,
                                contentDescription = key,
                                modifier = Modifier.fillMaxSize(),
                                cornerRadius = 14.dp
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Disponible sin conexión", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = offlinePinned, onCheckedChange = { offlinePinned = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name, description, selectedCover, offlinePinned) },
                enabled = name.trim().isNotEmpty(),
                modifier = Modifier.testTag("confirm_create_playlist")
            ) {
                Text("Crear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
