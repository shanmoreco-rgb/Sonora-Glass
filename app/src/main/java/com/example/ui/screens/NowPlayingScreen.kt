package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.OfflinePin
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.example.audio.PlaybackUiState
import com.example.data.model.AudioTrack
import com.example.ui.components.CoverArtThumbnail
import com.example.ui.components.GlassCard
import com.example.ui.components.HiResFormatBadge
import com.example.ui.components.SonoraBackdrop
import com.example.ui.components.SpectrumVisualizerCanvas
import com.example.ui.components.formatDurationMs
import com.example.ui.theme.LocalSonoraDesign
import com.example.ui.theme.MonoMetricStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    playbackState: PlaybackUiState,
    eqEnabled: Boolean,
    eqPresetName: String,
    onClose: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: (AudioTrack) -> Unit,
    onToggleOffline: (AudioTrack) -> Unit,
    onOpenEqualizer: () -> Unit,
    onPlayQueueItem: (Int) -> Unit
) {
    BackHandler { onClose() }

    val design = LocalSonoraDesign.current
    val track = playbackState.currentTrack
    var showQueueSheet by remember { mutableStateOf(false) }

    val artworkScale by animateFloatAsState(
        targetValue = if (playbackState.isPlaying) 1f else 0.92f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "artwork_scale"
    )

    SonoraBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("close_now_playing")
                ) {
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Cerrar reproductor", tint = design.textPrimary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("REPRODUCIENDO EN HI-RES", style = MaterialTheme.typography.labelSmall, color = design.accentGlow)
                    Text(
                        text = if (eqEnabled) "EQ: $eqPresetName" else "DSP Directo (Bypass)",
                        style = MonoMetricStyle,
                        color = design.textSecondary
                    )
                }
                IconButton(
                    onClick = { showQueueSheet = true },
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("open_queue_btn")
                ) {
                    Icon(Icons.Rounded.QueueMusic, contentDescription = "Cola de reproducción", tint = design.textPrimary)
                }
            }

            if (track == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Selecciona una pista de tu biblioteca", color = design.textSecondary)
                }
                return@Column
            }

            Spacer(Modifier.height(12.dp))

            // Artwork Card
            CoverArtThumbnail(
                coverKey = track.coverKey,
                contentDescription = track.title,
                cornerRadius = 30.dp,
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .fillMaxWidth(0.84f)
                    .aspectRatio(1f)
                    .scale(artworkScale)
            )

            Spacer(Modifier.height(18.dp))

            // Live Spectrum Visualizer
            GlassCard(cornerRadius = 18.dp, modifier = Modifier.fillMaxWidth().height(46.dp)) {
                SpectrumVisualizerCanvas(
                    bars = playbackState.spectrumBars,
                    isPlaying = playbackState.isPlaying,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Track Info & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = design.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${track.artist} • ${track.album}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = design.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = { onToggleOffline(track) }, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(
                        imageVector = Icons.Rounded.OfflinePin,
                        contentDescription = "Disponible Offline",
                        tint = if (track.offlineAvailable) design.cloudBadgeColor else design.textSecondary
                    )
                }
                IconButton(onClick = { onToggleFavorite(track) }, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(
                        imageVector = if (track.favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Favorita",
                        tint = if (track.favorite) design.secondaryGlow else design.textSecondary
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HiResFormatBadge(track = track)
                GlassCard(
                    cornerRadius = 50.dp,
                    onClick = onOpenEqualizer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = design.accentGlow, modifier = Modifier.size(14.dp))
                        Text("Ecualizador", style = MaterialTheme.typography.labelSmall, color = design.textPrimary)
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Seek Slider
            val duration = playbackState.durationMs.coerceAtLeast(1L)
            Slider(
                value = playbackState.positionMs.coerceIn(0L, duration).toFloat(),
                onValueChange = { onSeekTo(it.toLong()) },
                valueRange = 0f..duration.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = design.accentGlow,
                    activeTrackColor = design.accentGlow,
                    inactiveTrackColor = design.textSecondary.copy(alpha = 0.25f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatDurationMs(playbackState.positionMs), style = MonoMetricStyle, color = design.textSecondary)
                Text(formatDurationMs(playbackState.durationMs), style = MonoMetricStyle, color = design.textSecondary)
            }

            Spacer(Modifier.height(10.dp))

            // Transport Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleShuffle, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = "Aleatorio",
                        tint = if (playbackState.shuffleEnabled) design.accentGlow else design.textSecondary
                    )
                }
                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier.size(52.dp).minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Rounded.SkipPrevious, contentDescription = "Anterior", tint = design.textPrimary, modifier = Modifier.size(34.dp))
                }
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(design.accentGlow),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = onTogglePlayPause,
                        modifier = Modifier.fillMaxSize().testTag("now_playing_play_pause")
                    ) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (playbackState.isPlaying) "Pausar" else "Reproducir",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(52.dp).minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = "Siguiente", tint = design.textPrimary, modifier = Modifier.size(34.dp))
                }
                IconButton(onClick = onCycleRepeat, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(
                        imageVector = if (playbackState.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        contentDescription = "Repetir",
                        tint = if (playbackState.repeatMode != Player.REPEAT_MODE_OFF) design.accentGlow else design.textSecondary
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }

    if (showQueueSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQueueSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
                Text("Cola de Reproducción (${playbackState.queue.size})", style = MaterialTheme.typography.titleMedium, color = design.textPrimary)
                Spacer(Modifier.height(10.dp))
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().height(320.dp)
                ) {
                    itemsIndexed(playbackState.queue, key = { idx, t -> "${t.id}_q_$idx" }) { idx, qTrack ->
                        val active = idx == playbackState.currentIndex
                        GlassCard(
                            elevated = active,
                            cornerRadius = 16.dp,
                            onClick = {
                                onPlayQueueItem(idx)
                                showQueueSheet = false
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CoverArtThumbnail(qTrack.coverKey, qTrack.title, Modifier.size(42.dp), 10.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = qTrack.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (active) design.accentGlow else design.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text("${qTrack.artist} • ${qTrack.formatBadge}", style = MaterialTheme.typography.bodySmall, color = design.textSecondary)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}
