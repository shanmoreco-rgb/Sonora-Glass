package com.example.audio

import android.content.ComponentName
import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.data.local.LocalAudioStorage
import com.example.data.model.AudioTrack
import com.example.data.model.EqPreset
import com.example.data.model.SonoraCatalog
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin

data class PlaybackUiState(
    val currentTrack: AudioTrack? = null,
    val queue: List<AudioTrack> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_ALL,
    val spectrumBars: List<Float> = List(24) { 0.15f },
    val dspAttached: Boolean = false,
    val hardwareBandCount: Int = 0
)

class SonoraAudioEngine(
    private val context: Context,
    private val localAudioStorage: LocalAudioStorage
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var positionJob: Job? = null

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var presetReverb: PresetReverb? = null

    private var currentEqPreset: EqPreset = SonoraCatalog.defaultEqPresets("local").first().second
    private var hiResOutputPreferred: Boolean = true

    private val _playbackState = MutableStateFlow(PlaybackUiState())
    val playbackState: StateFlow<PlaybackUiState> = _playbackState.asStateFlow()

    private val _onTrackStarted = MutableStateFlow<String?>(null)
    val onTrackStarted: StateFlow<String?> = _onTrackStarted.asStateFlow()

    private var lastAttachedSessionId = 0
    private var pendingQueue: List<AudioTrack>? = null
    private var pendingStartIndex: Int = 0
    private var pendingAutoPlay: Boolean = false

    init {
        connectController()
        observeAudioSession()
    }

    private fun connectController() {
        val token = SessionToken(context, ComponentName(context, SonoraPlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                try {
                    val controller = future.get()
                    mediaController = controller
                    controller.addListener(playerListener)
                    syncPlayerMetadata()

                    val queued = pendingQueue
                    if (queued != null) {
                        pendingQueue = null
                        playQueue(queued, pendingStartIndex, pendingAutoPlay)
                    }
                } catch (e: Exception) {
                    Log.e("SonoraAudio", "Failed to connect MediaController", e)
                }
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    private fun observeAudioSession() {
        scope.launch {
            SonoraPlaybackService.audioSessionIdFlow.collect { sessionId ->
                if (sessionId > 0 && sessionId != lastAttachedSessionId) {
                    attachDspEffects(sessionId)
                }
            }
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playbackState.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) {
                startProgressLoop()
            } else {
                stopProgressLoop()
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            syncPlayerMetadata()
            mediaItem?.mediaId?.let { id ->
                _onTrackStarted.value = id
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            syncPlayerMetadata()
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _playbackState.update { it.copy(shuffleEnabled = shuffleModeEnabled) }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _playbackState.update { it.copy(repeatMode = repeatMode) }
        }
    }

    private fun syncPlayerMetadata() {
        val controller = mediaController ?: return
        val queue = _playbackState.value.queue
        val currentId = controller.currentMediaItem?.mediaId
        val matchedTrack = queue.firstOrNull { it.id == currentId } ?: _playbackState.value.currentTrack
        val idx = if (matchedTrack != null) queue.indexOfFirst { it.id == matchedTrack.id } else controller.currentMediaItemIndex
        val duration = controller.duration.takeIf { it > 0 } ?: matchedTrack?.durationMs ?: 0L
        _playbackState.update {
            it.copy(
                currentTrack = matchedTrack,
                currentIndex = idx,
                isPlaying = controller.isPlaying,
                positionMs = controller.currentPosition.coerceAtLeast(0L),
                durationMs = duration,
                shuffleEnabled = controller.shuffleModeEnabled,
                repeatMode = controller.repeatMode
            )
        }
    }

    fun playQueue(tracks: List<AudioTrack>, startIndex: Int = 0, autoPlay: Boolean = true) {
        if (tracks.isEmpty()) return
        val controller = mediaController
        if (controller == null) {
            pendingQueue = tracks
            pendingStartIndex = startIndex.coerceIn(0, tracks.lastIndex)
            pendingAutoPlay = autoPlay
            _playbackState.update {
                it.copy(
                    queue = tracks,
                    currentIndex = pendingStartIndex,
                    currentTrack = tracks[pendingStartIndex]
                )
            }
            return
        }

        val resolvedItems = mutableListOf<MediaItem>()
        val playableTracks = mutableListOf<AudioTrack>()
        var mappedStartIndex = 0

        tracks.forEachIndexed { index, track ->
            val uri = localAudioStorage.resolvePlayableUri(track)
            if (uri != null) {
                if (index == startIndex) {
                    mappedStartIndex = playableTracks.size
                }
                playableTracks.add(track)
                val metadata = MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setAlbumTitle(track.album)
                    .build()
                resolvedItems.add(
                    MediaItem.Builder()
                        .setMediaId(track.id)
                        .setUri(uri)
                        .setMediaMetadata(metadata)
                        .build()
                )
            }
        }

        if (resolvedItems.isEmpty()) return

        _playbackState.update {
            it.copy(
                queue = playableTracks,
                currentIndex = mappedStartIndex,
                currentTrack = playableTracks[mappedStartIndex],
                durationMs = playableTracks[mappedStartIndex].durationMs
            )
        }

        controller.setMediaItems(resolvedItems, mappedStartIndex, 0L)
        controller.prepare()
        if (autoPlay) {
            controller.play()
        }
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            if (controller.playbackState == Player.STATE_IDLE && _playbackState.value.queue.isNotEmpty()) {
                playQueue(_playbackState.value.queue, _playbackState.value.currentIndex.coerceAtLeast(0), true)
            } else {
                controller.play()
            }
        }
    }

    fun next() {
        mediaController?.seekToNextMediaItem()
    }

    fun previous() {
        val controller = mediaController ?: return
        if (controller.currentPosition > 3000L) {
            controller.seekTo(0L)
        } else {
            controller.seekToPreviousMediaItem()
        }
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs.coerceAtLeast(0L))
        _playbackState.update { it.copy(positionMs = positionMs.coerceAtLeast(0L)) }
    }

    fun toggleShuffle() {
        val controller = mediaController ?: return
        controller.shuffleModeEnabled = !controller.shuffleModeEnabled
    }

    fun cycleRepeatMode() {
        val controller = mediaController ?: return
        val nextMode = when (controller.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        controller.repeatMode = nextMode
    }

    fun setEnginePreferences(hiResEnabled: Boolean) {
        hiResOutputPreferred = hiResEnabled
        applyEqPreset(currentEqPreset)
    }

    fun applyEqPreset(preset: EqPreset) {
        currentEqPreset = preset
        val active = preset.enabled

        try {
            equalizer?.let { eq ->
                eq.enabled = active
                val bandCount = eq.numberOfBands.toInt()
                val range = eq.bandLevelRange // [minMb, maxMb], e.g. -1500..1500
                val minMb = range[0].toDouble()
                val maxMb = range[1].toDouble()

                for (hwBand in 0 until bandCount) {
                    val centerHz = eq.getCenterFreq(hwBand.toShort()) / 1000.0
                    val interpolatedDb = interpolateUiGainForHz(centerHz, preset.bandGainsDb)
                    val totalDb = (interpolatedDb + preset.preampDb * 0.4).coerceIn(-12.0, 12.0)
                    val targetMb = (totalDb * 100.0).coerceIn(minMb, maxMb).toInt().toShort()
                    eq.setBandLevel(hwBand.toShort(), targetMb)
                }
            }
        } catch (e: Throwable) {
            Log.w("SonoraDSP", "Equalizer apply failed", e)
        }

        try {
            bassBoost?.let { bb ->
                if (bb.strengthSupported) {
                    bb.enabled = active && preset.bassBoostStrength > 0L
                    bb.setStrength(preset.bassBoostStrength.coerceIn(0L, 1000L).toInt().toShort())
                }
            }
        } catch (e: Throwable) {
            Log.w("SonoraDSP", "BassBoost apply failed", e)
        }

        try {
            virtualizer?.let { virt ->
                if (virt.strengthSupported) {
                    virt.enabled = active && preset.virtualizerStrength > 0L
                    virt.setStrength(preset.virtualizerStrength.coerceIn(0L, 1000L).toInt().toShort())
                }
            }
        } catch (e: Throwable) {
            Log.w("SonoraDSP", "Virtualizer apply failed", e)
        }

        try {
            loudnessEnhancer?.let { le ->
                val preampMb = ((preset.preampDb.coerceAtLeast(0.0) * 100.0) + preset.loudnessGainMb).toInt()
                le.enabled = active && preampMb > 0
                le.setTargetGain(preampMb.coerceIn(0, 2500))
            }
        } catch (e: Throwable) {
            Log.w("SonoraDSP", "LoudnessEnhancer apply failed", e)
        }

        try {
            presetReverb?.let { rv ->
                val rvIdx = preset.reverbPreset.coerceIn(0L, 6L).toInt().toShort()
                rv.enabled = active && rvIdx > 0
                if (rvIdx > 0) {
                    rv.preset = rvIdx
                }
            }
        } catch (e: Throwable) {
            Log.w("SonoraDSP", "PresetReverb apply failed", e)
        }

        // Apply negative preamp smoothly via ExoPlayer volume attenuation when preamp < 0
        val volGain = if (active && preset.preampDb < 0.0) {
            10.0.pow(preset.preampDb / 20.0).toFloat().coerceIn(0.25f, 1.0f)
        } else {
            1.0f
        }
        mediaController?.volume = volGain
    }

    private fun interpolateUiGainForHz(targetHz: Double, uiBands: List<Double>): Double {
        val freqs = SonoraCatalog.eqBandFrequenciesHz
        if (uiBands.size < freqs.size) return 0.0
        if (targetHz <= freqs.first()) return uiBands.first()
        if (targetHz >= freqs.last()) return uiBands.last()

        for (i in 0 until freqs.lastIndex) {
            val f0 = freqs[i].toDouble()
            val f1 = freqs[i + 1].toDouble()
            if (targetHz in f0..f1) {
                val ratio = (targetHz - f0) / (f1 - f0)
                return uiBands[i] + (uiBands[i + 1] - uiBands[i]) * ratio
            }
        }
        return 0.0
    }

    private fun attachDspEffects(sessionId: Int) {
        releaseDspEffects()
        lastAttachedSessionId = sessionId
        var attachedCount = 0
        var hwBands = 0

        try {
            equalizer = Equalizer(0, sessionId).also {
                hwBands = it.numberOfBands.toInt()
                attachedCount++
            }
        } catch (e: Throwable) {
            Log.w("SonoraDSP", "Equalizer unavailable on session $sessionId", e)
        }

        try {
            bassBoost = BassBoost(0, sessionId).also { attachedCount++ }
        } catch (e: Throwable) {
            Log.w("SonoraDSP", "BassBoost unavailable", e)
        }

        try {
            virtualizer = Virtualizer(0, sessionId).also { attachedCount++ }
        } catch (e: Throwable) {
            Log.w("SonoraDSP", "Virtualizer unavailable", e)
        }

        try {
            loudnessEnhancer = LoudnessEnhancer(sessionId).also { attachedCount++ }
        } catch (e: Throwable) {
            Log.w("SonoraDSP", "LoudnessEnhancer unavailable", e)
        }

        try {
            presetReverb = PresetReverb(0, sessionId).also { attachedCount++ }
        } catch (e: Throwable) {
            Log.w("SonoraDSP", "PresetReverb unavailable", e)
        }

        _playbackState.update {
            it.copy(dspAttached = attachedCount > 0, hardwareBandCount = hwBands)
        }
        applyEqPreset(currentEqPreset)
    }

    private fun startProgressLoop() {
        positionJob?.cancel()
        positionJob = scope.launch {
            var tick = 0
            while (isActive) {
                val controller = mediaController
                if (controller != null && controller.isPlaying) {
                    val pos = controller.currentPosition.coerceAtLeast(0L)
                    val dur = controller.duration.takeIf { it > 0 } ?: _playbackState.value.durationMs
                    val bars = computeSpectrumEnvelope(tick, currentEqPreset)
                    _playbackState.update {
                        it.copy(
                            positionMs = pos,
                            durationMs = dur,
                            spectrumBars = bars
                        )
                    }
                    tick++
                }
                delay(75L)
            }
        }
    }

    private fun stopProgressLoop() {
        positionJob?.cancel()
        positionJob = null
        _playbackState.update { state ->
            state.copy(spectrumBars = List(24) { 0.12f })
        }
    }

    private fun computeSpectrumEnvelope(tick: Int, eq: EqPreset): List<Float> {
        val t = tick * 0.22
        return List(24) { idx ->
            val eqIdx = (idx * 10) / 24
            val eqBoost = if (eq.enabled) {
                ((eq.bandGainsDb.getOrNull(eqIdx) ?: 0.0) / 24.0).toFloat()
            } else 0f
            val bassMod = if (eq.enabled && idx < 6) (eq.bassBoostStrength / 3500f) else 0f
            val wave = 0.42f +
                0.28f * abs(sin(t * (0.9 + idx * 0.08) + idx * 0.45)).toFloat() +
                0.15f * sin((t * 1.7 + idx * 0.9) * PI * 0.25).toFloat()
            (wave + eqBoost + bassMod).coerceIn(0.10f, 1.0f)
        }
    }

    private fun releaseDspEffects() {
        try { equalizer?.release() } catch (_: Throwable) {}
        try { bassBoost?.release() } catch (_: Throwable) {}
        try { virtualizer?.release() } catch (_: Throwable) {}
        try { loudnessEnhancer?.release() } catch (_: Throwable) {}
        try { presetReverb?.release() } catch (_: Throwable) {}
        equalizer = null
        bassBoost = null
        virtualizer = null
        loudnessEnhancer = null
        presetReverb = null
    }

    fun release() {
        positionJob?.cancel()
        releaseDspEffects()
        mediaController?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
    }
}
