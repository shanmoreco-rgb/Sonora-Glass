package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.EqPreset
import com.example.data.model.SonoraCatalog
import com.example.data.model.UiState
import com.example.ui.components.GlassCard
import com.example.ui.theme.LocalSonoraDesign
import com.example.ui.theme.MonoMetricStyle
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun EqualizerScreen(
    livePreset: EqPreset,
    presetsState: UiState<List<EqPreset>>,
    dspAttached: Boolean,
    hardwareBandCount: Int,
    onSelectPreset: (EqPreset) -> Unit,
    onUpdateLiveEq: ((EqPreset) -> EqPreset) -> Unit,
    onPersistPreset: (String?) -> Unit,
    onDeletePreset: (EqPreset) -> Unit,
    contentBottomPadding: Dp
) {
    val design = LocalSonoraDesign.current
    var showSaveDialog by remember { mutableStateOf(false) }

    val presets = (presetsState as? UiState.Success)?.data.orEmpty()

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 6.dp,
            bottom = contentBottomPadding + 20.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Header + Master Switch
        item {
            GlassCard(elevated = true, cornerRadius = 24.dp, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(design.accentGlow.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = design.accentGlow)
                        }
                        Column {
                            Text("Ecualizador Avanzado", style = MaterialTheme.typography.titleMedium, color = design.textPrimary)
                            Text(
                                text = if (dspAttached) "DSP Activo • 10 Bandas (${hardwareBandCount} HW)"
                                else "Motor DSP 10 Bandas listo al reproducir",
                                style = MonoMetricStyle,
                                color = if (livePreset.enabled) design.accentGlow else design.textSecondary
                            )
                        }
                    }
                    Switch(
                        checked = livePreset.enabled,
                        onCheckedChange = { enabled ->
                            onUpdateLiveEq { it.copy(enabled = enabled) }
                            onPersistPreset(null)
                        },
                        modifier = Modifier.testTag("eq_master_switch")
                    )
                }
            }
        }

        // Preset Selector Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Perfiles en la Nube", style = MaterialTheme.typography.titleSmall, color = design.textPrimary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = {
                                onUpdateLiveEq {
                                    it.copy(
                                        bandGainsDb = List(10) { 0.0 },
                                        bassBoostStrength = 0L,
                                        virtualizerStrength = 0L,
                                        preampDb = 0.0,
                                        loudnessGainMb = 0L,
                                        reverbPreset = 0L
                                    )
                                }
                            },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Rounded.RestartAlt, contentDescription = "Reiniciar plano", tint = design.textSecondary)
                        }
                        Button(
                            onClick = { showSaveDialog = true },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = design.accentGlow.copy(alpha = 0.2f),
                                contentColor = design.accentGlow
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("save_eq_preset_btn")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Guardar", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(presets, key = { it.id }) { preset ->
                        val selected = preset.id == livePreset.id
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectPreset(preset) },
                            label = { Text(preset.name) },
                            trailingIcon = if (!preset.id.startsWith("preset_")) {
                                {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = "Eliminar perfil",
                                        modifier = Modifier
                                            .size(16.dp)
                                            .pointerInput(preset.id) {
                                                detectTapGestures { onDeletePreset(preset) }
                                            }
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = design.accentGlow.copy(alpha = 0.24f),
                                selectedLabelColor = design.accentGlow,
                                containerColor = design.glassFill,
                                labelColor = design.textSecondary
                            )
                        )
                    }
                }
            }
        }

        // Smooth Frequency Response Curve Canvas
        item {
            GlassCard(elevated = true, cornerRadius = 24.dp, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Curva de Respuesta (dB)", style = MaterialTheme.typography.labelLarge, color = design.textPrimary)
                        Text(
                            text = "${livePreset.name} • +12 / -12 dB",
                            style = MonoMetricStyle,
                            color = design.textSecondary
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    EqFrequencyCurveCanvas(
                        bandGainsDb = livePreset.bandGainsDb,
                        enabled = livePreset.enabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                    )
                }
            }
        }

        // 10-Band Vertical Faders
        item {
            GlassCard(cornerRadius = 24.dp, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (bandIdx in 0 until 10) {
                            val gain = (livePreset.bandGainsDb.getOrNull(bandIdx) ?: 0.0).toFloat()
                            EqBandFader(
                                label = SonoraCatalog.eqBandLabels[bandIdx],
                                valueDb = gain,
                                enabled = livePreset.enabled,
                                onValueChange = { newDb ->
                                    onUpdateLiveEq { curr ->
                                        val updated = curr.bandGainsDb.toMutableList()
                                        while (updated.size < 10) updated.add(0.0)
                                        updated[bandIdx] = ((newDb * 2f).roundToInt() / 2.0).coerceIn(-12.0, 12.0)
                                        curr.copy(bandGainsDb = updated)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("eq_band_$bandIdx")
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { onPersistPreset(null) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("sync_eq_changes_btn")
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Sincronizar curva en '${livePreset.name}'")
                    }
                }
            }
        }

        // DSP Effects Rack (Preamp, BassBoost, Virtualizer, Loudness, Reverb)
        item {
            GlassCard(cornerRadius = 24.dp, modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Procesamiento DSP Avanzado", style = MaterialTheme.typography.titleMedium, color = design.textPrimary)

                    DspSliderRow(
                        title = "Preamplificador",
                        valueLabel = String.format(Locale.US, "%+.1f dB", livePreset.preampDb),
                        value = livePreset.preampDb.toFloat(),
                        valueRange = -12f..12f,
                        enabled = livePreset.enabled,
                        onValueChange = { v ->
                            onUpdateLiveEq { it.copy(preampDb = ((v * 2f).roundToInt() / 2.0)) }
                        }
                    )

                    DspSliderRow(
                        title = "Refuerzo de Graves (BassBoost)",
                        valueLabel = "${(livePreset.bassBoostStrength / 10)}%",
                        value = livePreset.bassBoostStrength.toFloat(),
                        valueRange = 0f..1000f,
                        enabled = livePreset.enabled,
                        onValueChange = { v ->
                            onUpdateLiveEq { it.copy(bassBoostStrength = v.toLong().coerceIn(0L, 1000L)) }
                        }
                    )

                    DspSliderRow(
                        title = "Espacializador 3D (Virtualizer)",
                        valueLabel = "${(livePreset.virtualizerStrength / 10)}%",
                        value = livePreset.virtualizerStrength.toFloat(),
                        valueRange = 0f..1000f,
                        enabled = livePreset.enabled,
                        onValueChange = { v ->
                            onUpdateLiveEq { it.copy(virtualizerStrength = v.toLong().coerceIn(0L, 1000L)) }
                        }
                    )

                    DspSliderRow(
                        title = "Realce Dinámico (Loudness)",
                        valueLabel = "${livePreset.loudnessGainMb} mB",
                        value = livePreset.loudnessGainMb.toFloat(),
                        valueRange = 0f..2000f,
                        enabled = livePreset.enabled,
                        onValueChange = { v ->
                            onUpdateLiveEq { it.copy(loudnessGainMb = v.toLong().coerceIn(0L, 2000L)) }
                        }
                    )

                    Text("Reverberación Acústica", style = MaterialTheme.typography.labelLarge, color = design.textPrimary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(SonoraCatalog.reverbNames.indices.toList()) { rvIndex ->
                            val selected = livePreset.reverbPreset.toInt() == rvIndex
                            FilterChip(
                                selected = selected,
                                enabled = livePreset.enabled,
                                onClick = {
                                    onUpdateLiveEq { it.copy(reverbPreset = rvIndex.toLong()) }
                                },
                                label = { Text(SonoraCatalog.reverbNames[rvIndex]) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        var presetName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Guardar Perfil de Ecualizador") },
            text = {
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it.take(50) },
                    label = { Text("Nombre del perfil") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onPersistPreset(presetName)
                        showSaveDialog = false
                    },
                    enabled = presetName.trim().isNotEmpty()
                ) {
                    Text("Guardar en Nube")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun EqFrequencyCurveCanvas(
    bandGainsDb: List<Double>,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val design = LocalSonoraDesign.current
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val midY = h / 2f

        // Zero dB reference line & grid
        drawLine(
            color = design.textSecondary.copy(alpha = 0.28f),
            start = Offset(0f, midY),
            end = Offset(w, midY),
            strokeWidth = 1.5f
        )
        drawLine(
            color = design.textSecondary.copy(alpha = 0.14f),
            start = Offset(0f, h * 0.25f),
            end = Offset(w, h * 0.25f),
            strokeWidth = 1f
        )
        drawLine(
            color = design.textSecondary.copy(alpha = 0.14f),
            start = Offset(0f, h * 0.75f),
            end = Offset(w, h * 0.75f),
            strokeWidth = 1f
        )

        val points = (0 until 10).map { i ->
            val gain = (bandGainsDb.getOrNull(i) ?: 0.0).toFloat().coerceIn(-12f, 12f)
            val x = (i / 9f) * (w - 16f) + 8f
            val y = midY - (gain / 12f) * (h * 0.42f)
            Offset(x, y)
        }

        val curvePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 0 until points.lastIndex) {
                val p0 = points[i]
                val p1 = points[i + 1]
                val cx = (p0.x + p1.x) / 2f
                cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
            }
        }

        val fillPath = Path().apply {
            addPath(curvePath)
            lineTo(points.last().x, h)
            lineTo(points.first().x, h)
            close()
        }

        val lineColor = if (enabled) design.accentGlow else design.textSecondary.copy(alpha = 0.4f)
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                listOf(lineColor.copy(alpha = if (enabled) 0.32f else 0.10f), Color.Transparent)
            )
        )
        drawPath(
            path = curvePath,
            brush = Brush.horizontalGradient(listOf(lineColor, if (enabled) design.secondaryGlow else lineColor)),
            style = Stroke(width = 4.5f, cap = StrokeCap.Round)
        )

        points.forEach { pt ->
            drawCircle(color = lineColor, radius = 5.5f, center = pt)
        }
    }
}

@Composable
private fun EqBandFader(
    label: String,
    valueDb: Float,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val design = LocalSonoraDesign.current
    Column(
        modifier = modifier.padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = String.format(Locale.US, "%+.0f", valueDb),
            style = MonoMetricStyle,
            color = if (enabled) design.accentGlow else design.textSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .width(30.dp)
                .height(145.dp)
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectTapGestures { offset ->
                        val ratio = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                        onValueChange(-12f + ratio * 24f)
                    }
                }
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectDragGestures { change, _ ->
                        change.consume()
                        val ratio = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                        onValueChange(-12f + ratio * 24f)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val trackW = 6.dp.toPx()
                val left = (size.width - trackW) / 2f
                drawRoundRect(
                    color = design.textSecondary.copy(alpha = 0.22f),
                    topLeft = Offset(left, 0f),
                    size = Size(trackW, size.height),
                    cornerRadius = CornerRadius(trackW / 2f, trackW / 2f)
                )
                val normalized = ((valueDb + 12f) / 24f).coerceIn(0f, 1f)
                val thumbY = size.height * (1f - normalized)
                val midY = size.height / 2f
                val activeTop = minOf(thumbY, midY)
                val activeHeight = kotlin.math.abs(thumbY - midY)
                val activeColor = if (enabled) design.accentGlow else design.textSecondary.copy(alpha = 0.4f)

                drawRoundRect(
                    color = activeColor,
                    topLeft = Offset(left, activeTop),
                    size = Size(trackW, activeHeight),
                    cornerRadius = CornerRadius(trackW / 2f, trackW / 2f)
                )
                drawCircle(
                    color = Color.White,
                    radius = 7.dp.toPx(),
                    center = Offset(size.width / 2f, thumbY)
                )
                drawCircle(
                    color = activeColor,
                    radius = 5.dp.toPx(),
                    center = Offset(size.width / 2f, thumbY)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = design.textSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DspSliderRow(
    title: String,
    valueLabel: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    onValueChange: (Float) -> Unit
) {
    val design = LocalSonoraDesign.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = design.textPrimary)
            Text(valueLabel, style = MonoMetricStyle, color = design.accentGlow)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = design.accentGlow,
                activeTrackColor = design.accentGlow
            )
        )
    }
}
