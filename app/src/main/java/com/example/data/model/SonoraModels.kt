package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class SonoraThemeMode(
    val titleEs: String,
    val subtitleEs: String
) {
    LIQUID_GLASS(
        titleEs = "Liquid Glass",
        subtitleEs = "Cristal líquido translúcido y refracción aurora (Predeterminado)"
    ),
    OBSIDIAN_AMOLED(
        titleEs = "Obsidian AMOLED",
        subtitleEs = "Negro puro OLED con acentos cian y esmeralda neón"
    ),
    AURORA_NORD(
        titleEs = "Aurora Nord",
        subtitleEs = "Cristal glaciar luminoso de alto contraste diurno"
    ),
    WARM_VINYL(
        titleEs = "Warm Vinyl",
        subtitleEs = "Estética analógica cálida en ámbar y nogal"
    );

    companion object {
        fun fromRaw(raw: String?): SonoraThemeMode =
            entries.firstOrNull { it.name == raw } ?: LIQUID_GLASS
    }
}

enum class AudioSourceType {
    BUNDLED_HIRES,
    LOCAL_DEVICE;

    companion object {
        fun fromRaw(raw: String?): AudioSourceType =
            entries.firstOrNull { it.name == raw } ?: BUNDLED_HIRES
    }
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Audiófilo",
    val selectedTheme: String = SonoraThemeMode.LIQUID_GLASS.name,
    val offlineModeEnabled: Boolean = false,
    val gaplessEnabled: Boolean = true,
    val hiResOutputEnabled: Boolean = true,
    val activeEqPresetId: String = "preset_studio_flat",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @get:Exclude
    val themeMode: SonoraThemeMode
        get() = SonoraThemeMode.fromRaw(selectedTheme)
}

@Keep
@IgnoreExtraProperties
data class AudioTrack(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val durationMs: Long = 0L,
    val formatBadge: String = "WAV 24-bit / 48 kHz",
    val sampleRateHz: Long = 48000L,
    val bitDepth: Long = 24L,
    val codec: String = "WAV",
    val sourceType: String = AudioSourceType.BUNDLED_HIRES.name,
    val localFileName: String = "",
    val coverKey: String = "liquid_aurora",
    val favorite: Boolean = false,
    val offlineAvailable: Boolean = true,
    val playCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @get:Exclude
    val isHiResAudio: Boolean
        get() = bitDepth >= 24L || sampleRateHz > 44100L || codec in setOf("FLAC", "WAV", "ALAC", "AIFF", "DSD")

    @get:Exclude
    val resolvedSourceType: AudioSourceType
        get() = AudioSourceType.fromRaw(sourceType)
}

@Keep
@IgnoreExtraProperties
data class Playlist(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val coverKey: String = "liquid_aurora",
    val trackIds: List<String> = emptyList(),
    val offlinePinned: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class EqPreset(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "Plano Estudio",
    val bandGainsDb: List<Double> = List(10) { 0.0 },
    val bassBoostStrength: Long = 0L,
    val virtualizerStrength: Long = 0L,
    val preampDb: Double = 0.0,
    val loudnessGainMb: Long = 0L,
    val reverbPreset: Long = 0L,
    val enabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

object SonoraCatalog {
    val eqBandFrequenciesHz = listOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)
    val eqBandLabels = listOf("31", "62", "125", "250", "500", "1K", "2K", "4K", "8K", "16K")

    val reverbNames = listOf(
        "Sin Reverb",
        "Sala Pequeña",
        "Sala Media",
        "Sala Grande",
        "Auditorio",
        "Estudio Vivo",
        "Placa Cristal"
    )

    val coverKeys = listOf(
        "liquid_aurora",
        "midnight_vinyl",
        "prism_synth",
        "botanical_echo"
    )

    fun defaultBundledTracks(userId: String): List<Pair<String, AudioTrack>> = listOf(
        "track_liquid_aurora" to AudioTrack(
            id = "track_liquid_aurora",
            userId = userId,
            title = "Aurora Líquida (Master 24-bit)",
            artist = "Sonora Ensemble",
            album = "Refracciones de Cristal",
            durationMs = 10000L,
            formatBadge = "WAV 24-bit / 48 kHz",
            sampleRateHz = 48000L,
            bitDepth = 24L,
            codec = "WAV",
            sourceType = AudioSourceType.BUNDLED_HIRES.name,
            localFileName = "track_liquid_aurora",
            coverKey = "liquid_aurora",
            favorite = true,
            offlineAvailable = true,
            playCount = 0L
        ),
        "track_midnight_vinyl" to AudioTrack(
            id = "track_midnight_vinyl",
            userId = userId,
            title = "Medianoche en Vinilo",
            artist = "Clara Valdés Trio",
            album = "Sesiones Analógicas",
            durationMs = 10000L,
            formatBadge = "WAV 24-bit / 48 kHz",
            sampleRateHz = 48000L,
            bitDepth = 24L,
            codec = "WAV",
            sourceType = AudioSourceType.BUNDLED_HIRES.name,
            localFileName = "track_midnight_vinyl",
            coverKey = "midnight_vinyl",
            favorite = false,
            offlineAvailable = true,
            playCount = 0L
        ),
        "track_prism_synth" to AudioTrack(
            id = "track_prism_synth",
            userId = userId,
            title = "Prisma Armónico",
            artist = "Nébula Digital",
            album = "Espectro Puro",
            durationMs = 10000L,
            formatBadge = "WAV 24-bit / 48 kHz",
            sampleRateHz = 48000L,
            bitDepth = 24L,
            codec = "WAV",
            sourceType = AudioSourceType.BUNDLED_HIRES.name,
            localFileName = "track_prism_synth",
            coverKey = "prism_synth",
            favorite = true,
            offlineAvailable = true,
            playCount = 0L
        ),
        "track_botanical_echo" to AudioTrack(
            id = "track_botanical_echo",
            userId = userId,
            title = "Eco Botánico",
            artist = "Taller Acústico Sur",
            album = "Resonancia Natural",
            durationMs = 10000L,
            formatBadge = "WAV 24-bit / 48 kHz",
            sampleRateHz = 48000L,
            bitDepth = 24L,
            codec = "WAV",
            sourceType = AudioSourceType.BUNDLED_HIRES.name,
            localFileName = "track_botanical_echo",
            coverKey = "botanical_echo",
            favorite = false,
            offlineAvailable = true,
            playCount = 0L
        )
    )

    fun defaultPlaylists(userId: String): List<Pair<String, Playlist>> = listOf(
        "playlist_hires_showcase" to Playlist(
            id = "playlist_hires_showcase",
            userId = userId,
            name = "Colección Hi-Res 24-Bit",
            description = "Pistas maestras sin compresión listas para escuchar sin conexión.",
            coverKey = "liquid_aurora",
            trackIds = listOf("track_liquid_aurora", "track_prism_synth", "track_midnight_vinyl"),
            offlinePinned = true
        ),
        "playlist_nocturne_focus" to Playlist(
            id = "playlist_nocturne_focus",
            userId = userId,
            name = "Enfoque Nocturno",
            description = "Paisajes sonoros inmersivos y armónicos cálidos.",
            coverKey = "botanical_echo",
            trackIds = listOf("track_botanical_echo", "track_midnight_vinyl"),
            offlinePinned = true
        )
    )

    fun defaultEqPresets(userId: String): List<Pair<String, EqPreset>> = listOf(
        "preset_studio_flat" to EqPreset(
            id = "preset_studio_flat",
            userId = userId,
            name = "Plano Estudio",
            bandGainsDb = listOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
            bassBoostStrength = 0L,
            virtualizerStrength = 0L,
            preampDb = 0.0,
            loudnessGainMb = 0L,
            reverbPreset = 0L,
            enabled = true
        ),
        "preset_liquid_bass" to EqPreset(
            id = "preset_liquid_bass",
            userId = userId,
            name = "Liquid Bass",
            bandGainsDb = listOf(5.5, 4.8, 3.2, 1.0, -0.5, 0.0, 1.2, 2.5, 3.2, 2.8),
            bassBoostStrength = 550L,
            virtualizerStrength = 350L,
            preampDb = 1.5,
            loudnessGainMb = 300L,
            reverbPreset = 1L,
            enabled = true
        ),
        "preset_vocal_crystal" to EqPreset(
            id = "preset_vocal_crystal",
            userId = userId,
            name = "Vocal Cristal",
            bandGainsDb = listOf(-2.0, -1.0, 0.0, 1.5, 3.0, 4.2, 3.8, 2.5, 1.8, 1.0),
            bassBoostStrength = 100L,
            virtualizerStrength = 450L,
            preampDb = 1.0,
            loudnessGainMb = 200L,
            reverbPreset = 2L,
            enabled = true
        ),
        "preset_hires_acoustic" to EqPreset(
            id = "preset_hires_acoustic",
            userId = userId,
            name = "Hi-Res Acústico",
            bandGainsDb = listOf(2.0, 1.5, 0.5, 0.0, 0.5, 1.0, 2.0, 3.2, 4.0, 4.5),
            bassBoostStrength = 180L,
            virtualizerStrength = 620L,
            preampDb = 0.5,
            loudnessGainMb = 150L,
            reverbPreset = 5L,
            enabled = true
        ),
        "preset_electronic_club" to EqPreset(
            id = "preset_electronic_club",
            userId = userId,
            name = "Club Electrónica",
            bandGainsDb = listOf(6.0, 5.0, 2.5, -1.0, -1.5, 0.5, 2.2, 4.0, 4.8, 4.0),
            bassBoostStrength = 700L,
            virtualizerStrength = 500L,
            preampDb = 2.0,
            loudnessGainMb = 450L,
            reverbPreset = 3L,
            enabled = true
        )
    )
}
