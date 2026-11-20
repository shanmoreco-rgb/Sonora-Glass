package com.example.ui.screens

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.OfflinePin
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.AudioTrack
import com.example.data.model.Playlist
import com.example.ui.components.CoverArtThumbnail
import com.example.ui.components.GlassCard
import com.example.ui.components.HiResFormatBadge
import com.example.ui.theme.LocalSonoraDesign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlist: Playlist?,
    allTracks: List<AudioTrack>,
    onBack: () -> Unit,
    onPlayTracks: (List<AudioTrack>, Int) -> Unit,
    onMoveTrack: (Playlist, Int, Int) -> Unit,
    onRemoveTrack: (Playlist, Int) -> Unit,
    onAddTrack: (Playlist, AudioTrack) -> Unit,
    onToggleOffline: (Playlist) -> Unit,
    contentBottomPadding: Dp
) {
    val design = LocalSonoraDesign.current
    var showAddTracksSheet by remember { mutableStateOf(false) }

    if (playlist == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Lista no encontrada", color = design.textSecondary)
        }
        return
    }

    val playlistTracks = remember(playlist.trackIds, allTracks) {
        playlist.trackIds.mapNotNull { tid -> allTracks.firstOrNull { it.id == tid } }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver", tint = design.textPrimary)
            }
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.titleLarge,
                color = design.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { onToggleOffline(playlist) },
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Rounded.OfflinePin,
                    contentDescription = "Fijar para modo offline",
                    tint = if (playlist.offlinePinned) design.cloudBadgeColor else design.textSecondary
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = contentBottomPadding + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                GlassCard(elevated = true, cornerRadius = 26.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CoverArtThumbnail(
                                coverKey = playlist.coverKey,
                                contentDescription = playlist.name,
                                modifier = Modifier.size(84.dp),
                                cornerRadius = 20.dp
                            )
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(playlist.name, style = MaterialTheme.typography.headlineSmall, color = design.textPrimary)
                                if (playlist.description.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(playlist.description, style = MaterialTheme.typography.bodySmall, color = design.textSecondary)
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "${playlistTracks.size} pistas • ${if (playlist.offlinePinned) "Offline Activo" else "Solo en Nube"}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = design.accentGlow
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { onPlayTracks(playlistTracks, 0) },
                                enabled = playlistTracks.isNotEmpty(),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = design.accentGlow,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.weight(1f).testTag("playlist_play_btn")
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Reproducir")
                            }
                            OutlinedButton(
                                onClick = {
                                    val shuffled = playlistTracks.shuffled()
                                    onPlayTracks(shuffled, 0)
                                },
                                enabled = playlistTracks.isNotEmpty(),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Aleatorio")
                            }
                            IconButton(
                                onClick = { showAddTracksSheet = true },
                                modifier = Modifier.minimumInteractiveComponentSize().testTag("playlist_add_tracks_btn")
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = "Añadir pistas", tint = design.accentGlow)
                            }
                        }
                    }
                }
            }

            if (playlistTracks.isEmpty()) {
                item {
                    GlassCard(cornerRadius = 22.dp, modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Lista vacía", style = MaterialTheme.typography.titleMedium, color = design.textPrimary)
                            Spacer(Modifier.height(6.dp))
                            Text("Pulsa + para agregar pistas de alta resolución.", style = MaterialTheme.typography.bodySmall, color = design.textSecondary)
                        }
                    }
                }
            } else {
                itemsIndexed(playlistTracks, key = { idx, t -> "${t.id}_$idx" }) { index, track ->
                    GlassCard(
                        cornerRadius = 18.dp,
                        onClick = { onPlayTracks(playlistTracks, index) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CoverArtThumbnail(
                                coverKey = track.coverKey,
                                contentDescription = track.title,
                                modifier = Modifier.size(46.dp),
                                cornerRadius = 12.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = design.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(3.dp))
                                HiResFormatBadge(track = track)
                            }
                            IconButton(
                                onClick = { onMoveTrack(playlist, index, index - 1) },
                                enabled = index > 0,
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Rounded.ArrowUpward, contentDescription = "Subir pista", tint = design.textSecondary)
                            }
                            IconButton(
                                onClick = { onMoveTrack(playlist, index, index + 1) },
                                enabled = index < playlistTracks.lastIndex,
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Rounded.ArrowDownward, contentDescription = "Bajar pista", tint = design.textSecondary)
                            }
                            IconButton(
                                onClick = { onRemoveTrack(playlist, index) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = "Quitar de la lista", tint = design.textSecondary)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddTracksSheet) {
        val available = allTracks.filter { !playlist.trackIds.contains(it.id) }
        ModalBottomSheet(
            onDismissRequest = { showAddTracksSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Text("Agregar pistas a ${playlist.name}", style = MaterialTheme.typography.titleMedium, color = design.textPrimary)
                Spacer(Modifier.height(12.dp))
                if (available.isEmpty()) {
                    Text("Todas las pistas de tu biblioteca ya están en esta lista.", style = MaterialTheme.typography.bodyMedium, color = design.textSecondary)
                } else {
                    available.forEach { track ->
                        GlassCard(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            onClick = {
                                onAddTrack(playlist, track)
                                showAddTracksSheet = false
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CoverArtThumbnail(track.coverKey, track.title, Modifier.size(42.dp), 10.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(track.title, style = MaterialTheme.typography.titleSmall, color = design.textPrimary)
                                    Text("${track.artist} • ${track.formatBadge}", style = MaterialTheme.typography.bodySmall, color = design.textSecondary)
                                }
                                Icon(Icons.Rounded.Add, contentDescription = "Añadir", tint = design.accentGlow)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
