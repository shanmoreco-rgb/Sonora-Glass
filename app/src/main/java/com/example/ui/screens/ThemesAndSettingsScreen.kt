package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.SonoraThemeMode
import com.example.data.model.UserProfile
import com.example.ui.components.GlassCard
import com.example.ui.theme.LocalSonoraDesign
import com.example.ui.theme.MonoMetricStyle
import com.example.ui.theme.tokensForTheme
import java.util.Locale

@Composable
fun ThemesAndSettingsScreen(
    activeTheme: SonoraThemeMode,
    offlineModeEnabled: Boolean,
    isDeviceOnline: Boolean,
    userProfile: UserProfile?,
    userEmail: String?,
    offlineTracksCount: Int,
    totalTracksCount: Int,
    offlineCacheMb: Double,
    isSyncing: Boolean,
    onSelectTheme: (SonoraThemeMode) -> Unit,
    onToggleOfflineMode: (Boolean) -> Unit,
    onUpdateAudioPrefs: (Boolean, Boolean) -> Unit,
    onForceSync: () -> Unit,
    onSignOut: () -> Unit,
    contentBottomPadding: Dp
) {
    val design = LocalSonoraDesign.current
    val gapless = userProfile?.gaplessEnabled ?: true
    val hiResOutput = userProfile?.hiResOutputEnabled ?: true

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = contentBottomPadding + 20.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Themes Section
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.Palette, contentDescription = null, tint = design.accentGlow)
                Text("Personalización y Temas", style = MaterialTheme.typography.titleLarge, color = design.textPrimary)
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SonoraThemeMode.entries.forEach { mode ->
                    val preview = tokensForTheme(mode)
                    val selected = mode == activeTheme
                    GlassCard(
                        elevated = selected,
                        cornerRadius = 22.dp,
                        onClick = { onSelectTheme(mode) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("theme_option_${mode.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Live Mini Theme Swatch
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Brush.linearGradient(preview.backdropColors))
                                    .border(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) preview.accentGlow else preview.glassBorderColors.first(),
                                        shape = RoundedCornerShape(16.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(preview.accentGlow)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(preview.secondaryGlow)
                                    )
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(mode.titleEs, style = MaterialTheme.typography.titleMedium, color = design.textPrimary)
                                Spacer(Modifier.height(2.dp))
                                Text(mode.subtitleEs, style = MaterialTheme.typography.bodySmall, color = design.textSecondary)
                            }
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Tema activo",
                                    tint = design.accentGlow
                                )
                            }
                        }
                    }
                }
            }
        }

        // Offline Mode & Storage Section
        item {
            GlassCard(elevated = true, cornerRadius = 24.dp, modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (offlineModeEnabled) Icons.Rounded.CloudOff else Icons.Rounded.CloudDone,
                                contentDescription = null,
                                tint = if (offlineModeEnabled) design.offlineBadgeColor else design.cloudBadgeColor
                            )
                            Column {
                                Text("Modo Offline Funcional", style = MaterialTheme.typography.titleMedium, color = design.textPrimary)
                                Text(
                                    text = if (offlineModeEnabled) "Red Firestore deshabilitada • Reproducción local"
                                    else "Sincronización automática activa",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = design.textSecondary
                                )
                            }
                        }
                        Switch(
                            checked = offlineModeEnabled,
                            onCheckedChange = onToggleOfflineMode,
                            modifier = Modifier.testTag("offline_mode_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GlassCard(cornerRadius = 16.dp, modifier = Modifier.weight(1f)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Icon(Icons.Rounded.Storage, contentDescription = null, tint = design.accentGlow, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.height(4.dp))
                                Text("$offlineTracksCount / $totalTracksCount pistas", style = MaterialTheme.typography.titleSmall, color = design.textPrimary)
                                Text("Disponibles sin red", style = MaterialTheme.typography.bodySmall, color = design.textSecondary)
                            }
                        }
                        GlassCard(cornerRadius = 16.dp, modifier = Modifier.weight(1f)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Icon(Icons.Rounded.HighQuality, contentDescription = null, tint = design.secondaryGlow, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = String.format(Locale.US, "%.1f MB", offlineCacheMb),
                                    style = MonoMetricStyle.copy(fontSize = MaterialTheme.typography.titleSmall.fontSize),
                                    color = design.textPrimary
                                )
                                Text("Caché local importada", style = MaterialTheme.typography.bodySmall, color = design.textSecondary)
                            }
                        }
                    }
                }
            }
        }

        // Hi-Res Engine Settings
        item {
            GlassCard(cornerRadius = 24.dp, modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Motor de Audio Hi-Res", style = MaterialTheme.typography.titleMedium, color = design.textPrimary)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Salida PCM 32-bit Float", style = MaterialTheme.typography.bodyLarge, color = design.textPrimary)
                            Text("Preserva el rango dinámico completo en FLAC y WAV", style = MaterialTheme.typography.bodySmall, color = design.textSecondary)
                        }
                        Switch(
                            checked = hiResOutput,
                            onCheckedChange = { onUpdateAudioPrefs(gapless, it) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Reproducción sin pausas (Gapless)", style = MaterialTheme.typography.bodyLarge, color = design.textPrimary)
                            Text("Transición continua entre pistas de álbum", style = MaterialTheme.typography.bodySmall, color = design.textSecondary)
                        }
                        Switch(
                            checked = gapless,
                            onCheckedChange = { onUpdateAudioPrefs(it, hiResOutput) }
                        )
                    }
                }
            }
        }

        // Cloud Sync & Account
        item {
            GlassCard(cornerRadius = 24.dp, modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Cuenta y Sincronización en la Nube", style = MaterialTheme.typography.titleMedium, color = design.textPrimary)
                    Text(
                        text = "${userProfile?.displayName ?: "Usuario"} (${userEmail ?: "Google"})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = design.textSecondary
                    )
                    Text(
                        text = when {
                            offlineModeEnabled -> "Estado: Modo Offline manual activo (escrituras en cola local)"
                            !isDeviceOnline -> "Estado: Sin conexión de red (caché local operando)"
                            else -> "Estado: Conectado y sincronizado en tiempo real"
                        },
                        style = MonoMetricStyle,
                        color = if (!offlineModeEnabled && isDeviceOnline) design.cloudBadgeColor else design.offlineBadgeColor
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onForceSync,
                            enabled = !isSyncing && !offlineModeEnabled,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = design.accentGlow,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.weight(1f).testTag("force_sync_button")
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Rounded.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Sincronizar")
                            }
                        }

                        OutlinedButton(
                            onClick = onSignOut,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f).testTag("sign_out_button")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Cerrar sesión")
                        }
                    }
                }
            }
        }
    }
}
