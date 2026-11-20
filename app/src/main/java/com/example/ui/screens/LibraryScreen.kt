package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.OfflinePin
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.unit.dp
import com.example.data.model.AudioSourceType
import com.example.data.model.AudioTrack
import com.example.data.model.Playlist
import com.example.data.model.UiState
import com.example.ui.components.CoverArtThumbnail
import com.example.ui.components.GlassCard
import com.example.ui.components.HiResFormatBadge
import com.example.ui.components.formatDurationMs
import com.example.ui.theme.LocalSonoraDesign
import com.example.ui.theme.MonoMetricStyle

private enum class LibraryFilter(val label: String) {
    ALL("Todas"),
    HIRES("Alta Res"),
    OFFLINE("Offline"),
    FAVORITES("Favoritas")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    tracksState: UiState<List<AudioTrack>>,
    playlistsState: UiState<List<Playlist>>,
    activeTrackId: String?,
    offlineModeEnabled: Boolean,
    isImporting: Boolean,
    onImportAudio: (List<android.net.Uri>) -> Unit,
    onPlayTracks: (List<AudioTrack>, Int) -> Unit,
    onToggleFavorite: (AudioTrack) -> Unit,
    onToggleOffline: (AudioTrack) -> Unit,
    onDeleteTrack: (AudioTrack) -> Unit,
    onAddToPlaylist: (Playlist, AudioTrack) -> Unit,
    contentBottomPadding: androidx.compose.ui.unit.Dp
) {
    val design = LocalSonoraDesign.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(LibraryFilter.ALL) }
    var trackForPlaylistSheet by remember { mutableStateOf<AudioTrack?>(null) }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                onImportAudio(uris)
            }
        }
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Search & Import Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar pista, artista o FLAC/WAV...", maxLines = 1) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Limpiar búsqueda")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = design.accentGlow,
                        unfocusedBorderColor = design.glassBorderColors.first().copy(alpha = 0.4f),
                        focusedContainerColor = design.glassFill,
                        unfocusedContainerColor = design.glassFill
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("library_search_input")
                )

                Button(
                    onClick = {
                        audioPickerLauncher.launch(
                            arrayOf(
                                "audio/*",
                                "audio/flac",
                                "audio/wav",
                                "audio/x-wav",
                                "audio/mp4",
                                "audio/ogg"
                            )
                        )
                    },
                    enabled = !isImporting,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = design.accentGlow,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                    modifier = Modifier
                        .height(54.dp)
                        .testTag("import_audio_button")
                ) {
                    if (isImporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Rounded.UploadFile, contentDescription = "Importar Hi-Res")
                        Spacer(Modifier.width(6.dp))
                        Text("Importar", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(LibraryFilter.entries) { filter ->
                    val selected = selectedFilter == filter
                    FilterChip(
                        selected = selected,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = design.accentGlow.copy(alpha = 0.22f),
                            selectedLabelColor = design.accentGlow,
                            containerColor = design.glassFill,
                            labelColor = design.textSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = design.glassBorderColors.first().copy(alpha = 0.3f),
                            selectedBorderColor = design.accentGlow
                        ),
                        modifier = Modifier.testTag("filter_${filter.name.lowercase()}")
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        when (tracksState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = design.accentGlow)
                }
            }
            is UiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    GlassCard(cornerRadius = 24.dp) {
                        Text(
                            text = tracksState.message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            }
            is UiState.Success -> {
                val filteredTracks = remember(tracksState.data, searchQuery, selectedFilter, offlineModeEnabled) {
                    tracksState.data.filter { track ->
                        val matchesQuery = searchQuery.isBlank() ||
                            track.title.contains(searchQuery, ignoreCase = true) ||
                            track.artist.contains(searchQuery, ignoreCase = true) ||
                            track.codec.contains(searchQuery, ignoreCase = true) ||
                            track.album.contains(searchQuery, ignoreCase = true)

                        val matchesFilter = when (selectedFilter) {
                            LibraryFilter.ALL -> !offlineModeEnabled || track.offlineAvailable
                            LibraryFilter.HIRES -> track.isHiResAudio && (!offlineModeEnabled || track.offlineAvailable)
                            LibraryFilter.OFFLINE -> track.offlineAvailable
                            LibraryFilter.FAVORITES -> track.favorite && (!offlineModeEnabled || track.offlineAvailable)
                        }
                        matchesQuery && matchesFilter
                    }
                }

                if (filteredTracks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        GlassCard(cornerRadius = 28.dp) {
                            Column(
                                modifier = Modifier.padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LibraryMusic,
                                    contentDescription = null,
                                    tint = design.accentGlow,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = "Sin pistas en esta vista",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = design.textPrimary
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "Importa archivos FLAC, WAV, ALAC o MP3 desde tu dispositivo para reproducirlos sin conexión.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = design.textSecondary
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 4.dp,
                            bottom = contentBottomPadding + 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${filteredTracks.size} pistas disponibles",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = design.textSecondary
                                )
                                Button(
                                    onClick = { onPlayTracks(filteredTracks, 0) },
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = design.accentGlow.copy(alpha = 0.2f),
                                        contentColor = design.accentGlow
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("play_all_library")
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Reproducir todo", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }

                        itemsIndexed(filteredTracks, key = { _, t -> t.id }) { index, track ->
                            TrackRowCard(
                                track = track,
                                isActive = track.id == activeTrackId,
                                onPlay = { onPlayTracks(filteredTracks, index) },
                                onToggleFavorite = { onToggleFavorite(track) },
                                onToggleOffline = { onToggleOffline(track) },
                                onAddToPlaylist = { trackForPlaylistSheet = track },
                                onDelete = { onDeleteTrack(track) }
                            )
                        }
                    }
                }
            }
        }
    }

    val selectedTrack = trackForPlaylistSheet
    if (selectedTrack != null) {
        val playlists = (playlistsState as? UiState.Success)?.data.orEmpty()
        ModalBottomSheet(
            onDismissRequest = { trackForPlaylistSheet = null },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Añadir '${selectedTrack.title}' a lista",
                    style = MaterialTheme.typography.titleMedium,
                    color = design.textPrimary
                )
                Spacer(Modifier.height(12.dp))
                if (playlists.isEmpty()) {
                    Text(
                        text = "Crea primero una lista en la pestaña Listas.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = design.textSecondary,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    playlists.forEach { playlist ->
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            onClick = {
                                onAddToPlaylist(playlist, selectedTrack)
                                trackForPlaylistSheet = null
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CoverArtThumbnail(
                                    coverKey = playlist.coverKey,
                                    contentDescription = playlist.name,
                                    modifier = Modifier.size(44.dp),
                                    cornerRadius = 10.dp
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(playlist.name, style = MaterialTheme.typography.titleSmall, color = design.textPrimary)
                                    Text("${playlist.trackIds.size} pistas", style = MaterialTheme.typography.bodySmall, color = design.textSecondary)
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

@Composable
private fun TrackRowCard(
    track: AudioTrack,
    isActive: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleOffline: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDelete: () -> Unit
) {
    val design = LocalSonoraDesign.current
    var menuExpanded by remember { mutableStateOf(false) }

    GlassCard(
        elevated = isActive,
        cornerRadius = 20.dp,
        onClick = onPlay,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("track_card_${track.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CoverArtThumbnail(
                coverKey = track.coverKey,
                contentDescription = track.title,
                modifier = Modifier.size(54.dp),
                cornerRadius = 14.dp
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isActive) design.accentGlow else design.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${track.artist} • ${formatDurationMs(track.durationMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = design.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HiResFormatBadge(track = track)
                    if (track.offlineAvailable) {
                        Icon(
                            imageVector = Icons.Rounded.OfflinePin,
                            contentDescription = "Disponible sin conexión",
                            tint = design.cloudBadgeColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (track.playCount > 0) {
                        Text(
                            text = "${track.playCount}x",
                            style = MonoMetricStyle,
                            color = design.textSecondary
                        )
                    }
                }
            }
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("favorite_btn_${track.id}")
            ) {
                Icon(
                    imageVector = if (track.favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Favorita",
                    tint = if (track.favorite) design.secondaryGlow else design.textSecondary
                )
            }
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "Opciones de pista",
                        tint = design.textSecondary
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Añadir a lista de reproducción") },
                        leadingIcon = { Icon(Icons.Rounded.PlaylistAdd, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onAddToPlaylist()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (track.offlineAvailable) "Quitar marca Offline"
                                else "Marcar disponible Offline"
                            )
                        },
                        leadingIcon = { Icon(Icons.Rounded.DownloadDone, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onToggleOffline()
                        }
                    )
                    if (track.resolvedSourceType == AudioSourceType.LOCAL_DEVICE) {
                        DropdownMenuItem(
                            text = { Text("Eliminar de biblioteca") },
                            leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}
