package com.example.ui.components

import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.audio.PlaybackUiState
import com.example.data.model.AudioTrack
import com.example.data.model.SonoraThemeMode
import com.example.ui.theme.LocalSonoraDesign
import com.example.ui.theme.MonoMetricStyle

@Composable
fun SonoraBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val design = LocalSonoraDesign.current
    val transition = rememberInfiniteTransition(label = "liquid_backdrop")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_drift"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(design.backdropColors))
    ) {
        if (design.themeMode != SonoraThemeMode.OBSIDIAN_AMOLED) {
            val orbModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Modifier.fillMaxSize().blur(78.dp)
            } else {
                Modifier.fillMaxSize()
            }
            Canvas(modifier = orbModifier) {
                val w = size.width
                val h = size.height
                val alphaScale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 0.52f else 0.28f

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(design.orbPrimary.copy(alpha = alphaScale), Color.Transparent),
                        center = Offset(w * (0.22f + 0.16f * drift), h * (0.14f + 0.08f * drift)),
                        radius = w * 0.62f
                    ),
                    radius = w * 0.62f,
                    center = Offset(w * (0.22f + 0.16f * drift), h * (0.14f + 0.08f * drift))
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(design.orbSecondary.copy(alpha = alphaScale * 0.9f), Color.Transparent),
                        center = Offset(w * (0.85f - 0.18f * drift), h * (0.44f - 0.08f * drift)),
                        radius = w * 0.58f
                    ),
                    radius = w * 0.58f,
                    center = Offset(w * (0.85f - 0.18f * drift), h * (0.44f - 0.08f * drift))
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(design.orbTertiary.copy(alpha = alphaScale * 0.75f), Color.Transparent),
                        center = Offset(w * (0.35f + 0.22f * drift), h * 0.84f),
                        radius = w * 0.55f
                    ),
                    radius = w * 0.55f,
                    center = Offset(w * (0.35f + 0.22f * drift), h * 0.84f)
                )
            }
        }
        content()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    elevated: Boolean = false,
    cornerRadius: Dp = 24.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val design = LocalSonoraDesign.current
    val shape = RoundedCornerShape(cornerRadius)
    val fillColor = if (elevated) design.glassElevatedFill else design.glassFill
    val sheenBrush = Brush.verticalGradient(
        listOf(
            design.glassHighlightSheen,
            Color.Transparent,
            Color.Transparent
        )
    )

    val baseModifier = modifier
        .clip(shape)
        .background(fillColor)
        .background(sheenBrush)
        .border(
            width = 1.dp,
            brush = design.glassBorderBrush,
            shape = shape
        )
        .let { m ->
            if (onClick != null) m.clickable(onClick = onClick) else m
        }

    Box(modifier = baseModifier, content = content)
}

@Composable
fun CoverArtThumbnail(
    coverKey: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp
) {
    val design = LocalSonoraDesign.current
    val resId = when (coverKey) {
        "midnight_vinyl" -> R.drawable.img_cover_midnight_vinyl
        "prism_synth" -> R.drawable.img_cover_prism_synth
        "botanical_echo" -> R.drawable.img_cover_botanical_echo
        else -> R.drawable.img_cover_liquid_aurora
    }
    val shape = RoundedCornerShape(cornerRadius)
    Image(
        painter = painterResource(id = resId),
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .clip(shape)
            .border(1.dp, design.glassBorderBrush, shape)
    )
}

@Composable
fun HiResFormatBadge(
    track: AudioTrack,
    modifier: Modifier = Modifier
) {
    val design = LocalSonoraDesign.current
    val accent = if (track.isHiResAudio) design.accentGlow else design.textSecondary
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = accent.copy(alpha = 0.14f),
        border = BorderStroke(0.8.dp, accent.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (track.isHiResAudio) {
                Icon(
                    imageVector = Icons.Rounded.HighQuality,
                    contentDescription = "Audio Alta Resolución",
                    tint = accent,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = track.formatBadge,
                style = MonoMetricStyle,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SpectrumVisualizerCanvas(
    bars: List<Float>,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val design = LocalSonoraDesign.current
    Canvas(modifier = modifier) {
        val count = bars.size.coerceAtLeast(1)
        val gap = size.width * 0.015f
        val barWidth = ((size.width - gap * (count - 1)) / count).coerceAtLeast(2f)
        val brush = Brush.verticalGradient(
            listOf(design.accentGlow, design.secondaryGlow)
        )
        bars.forEachIndexed { index, rawLevel ->
            val level = if (isPlaying) rawLevel.coerceIn(0.08f, 1f) else 0.10f
            val barHeight = size.height * level
            val x = index * (barWidth + gap)
            val y = size.height - barHeight
            drawRoundRect(
                brush = brush,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
                alpha = if (isPlaying) 0.92f else 0.35f
            )
        }
    }
}

@Composable
fun FloatingMiniPlayer(
    playbackState: PlaybackUiState,
    onOpenPlayer: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = playbackState.currentTrack ?: return
    val design = LocalSonoraDesign.current
    val progress = if (playbackState.durationMs > 0L) {
        (playbackState.positionMs.toFloat() / playbackState.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("mini_player_bar"),
        elevated = true,
        cornerRadius = 22.dp,
        onClick = onOpenPlayer
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (playbackState.isPlaying) {
                            Icon(
                                imageVector = Icons.Rounded.GraphicEq,
                                contentDescription = "Reproduciendo",
                                tint = design.accentGlow,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = design.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${track.artist} • ${track.codec} ${track.bitDepth}-bit",
                        style = MaterialTheme.typography.bodySmall,
                        color = design.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(CircleShape)
                        .background(design.accentGlow.copy(alpha = 0.18f))
                        .testTag("mini_player_play_pause")
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (playbackState.isPlaying) "Pausar" else "Reproducir",
                        tint = design.accentGlow
                    )
                }
                Spacer(Modifier.width(4.dp))
                IconButton(
                    onClick = onNext,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("mini_player_next")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Siguiente pista",
                        tint = design.textPrimary
                    )
                }
            }
            // Progress hairline
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .background(design.textSecondary.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(2.5.dp)
                        .background(
                            Brush.horizontalGradient(listOf(design.accentGlow, design.secondaryGlow))
                        )
                )
            }
        }
    }
}

fun formatDurationMs(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val mins = totalSec / 60L
    val secs = totalSec % 60L
    return "%02d:%02d".format(mins, secs)
}
