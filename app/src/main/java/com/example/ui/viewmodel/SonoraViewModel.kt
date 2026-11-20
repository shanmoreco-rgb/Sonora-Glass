package com.example.ui.viewmodel

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.PlaybackUiState
import com.example.audio.SonoraAudioEngine
import com.example.data.local.LocalAudioStorage
import com.example.data.local.SonoraPreferencesStore
import com.example.data.model.AudioTrack
import com.example.data.model.EqPreset
import com.example.data.model.Playlist
import com.example.data.model.SonoraCatalog
import com.example.data.model.SonoraThemeMode
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.repository.SonoraRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SonoraViewModel(
    private val repository: SonoraRepository,
    private val prefsStore: SonoraPreferencesStore,
    private val localAudioStorage: LocalAudioStorage,
    private val audioEngine: SonoraAudioEngine,
    private val currentUserId: String,
    private val currentDisplayName: String?,
    private val appContext: Context
) : ViewModel() {

    private val _isDeviceOnline = MutableStateFlow(checkNetworkConnected())
    val isDeviceOnline: StateFlow<Boolean> = _isDeviceOnline.asStateFlow()

    private val _offlineCacheMb = MutableStateFlow(localAudioStorage.getOfflineCacheSizeMb())
    val offlineCacheMb: StateFlow<Double> = _offlineCacheMb.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    private val _liveEqPreset = MutableStateFlow(SonoraCatalog.defaultEqPresets(currentUserId).first().second)
    val liveEqPreset: StateFlow<EqPreset> = _liveEqPreset.asStateFlow()

    val playbackState: StateFlow<PlaybackUiState> = audioEngine.playbackState

    val userProfileState: StateFlow<UiState<UserProfile?>> = repository.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing user profile", rootError)
            emit(UiState.Error(rootError.message ?: "Error al sincronizar perfil"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val tracksState: StateFlow<UiState<List<AudioTrack>>> = repository.observeTracks(currentUserId)
        .map<List<AudioTrack>, UiState<List<AudioTrack>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing tracks", rootError)
            emit(UiState.Error(rootError.message ?: "Error al cargar pistas"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val playlistsState: StateFlow<UiState<List<Playlist>>> = repository.observePlaylists(currentUserId)
        .map<List<Playlist>, UiState<List<Playlist>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing playlists", rootError)
            emit(UiState.Error(rootError.message ?: "Error al cargar listas"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val eqPresetsState: StateFlow<UiState<List<EqPreset>>> = repository.observeEqPresets(currentUserId)
        .map<List<EqPreset>, UiState<List<EqPreset>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing eq presets", rootError)
            emit(UiState.Error(rootError.message ?: "Error al cargar perfiles de ecualizador"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val activeThemeMode: StateFlow<SonoraThemeMode> = combine(
        prefsStore.prefsFlow,
        userProfileState
    ) { localPrefs, profileUi ->
        when (profileUi) {
            is UiState.Success -> profileUi.data?.themeMode ?: localPrefs.themeMode
            else -> localPrefs.themeMode
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = SonoraThemeMode.LIQUID_GLASS
    )

    val offlineModeEnabled: StateFlow<Boolean> = combine(
        prefsStore.prefsFlow,
        userProfileState
    ) { localPrefs, profileUi ->
        when (profileUi) {
            is UiState.Success -> profileUi.data?.offlineModeEnabled ?: localPrefs.offlineModeEnabled
            else -> localPrefs.offlineModeEnabled
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = false
    )

    private var connectivityCallback: ConnectivityManager.NetworkCallback? = null
    private var initialQueuePrepared = false

    init {
        registerConnectivityListener()
        viewModelScope.launch {
            repository.ensureSeededLibrary(currentUserId, currentDisplayName)
        }
        observeProfileAndSyncLocalPrefs()
        observeActiveEqPreset()
        observeTrackStarts()
    }

    private fun observeProfileAndSyncLocalPrefs() {
        viewModelScope.launch {
            userProfileState.collect { ui ->
                if (ui is UiState.Success && ui.data != null) {
                    val profile = ui.data
                    prefsStore.setThemeMode(profile.themeMode)
                    prefsStore.setOfflineMode(profile.offlineModeEnabled)
                    prefsStore.setGaplessEnabled(profile.gaplessEnabled)
                    prefsStore.setHiResOutputEnabled(profile.hiResOutputEnabled)
                    prefsStore.setActiveEqPresetId(profile.activeEqPresetId)
                    audioEngine.setEnginePreferences(profile.hiResOutputEnabled)
                    repository.setNetworkEnabled(!profile.offlineModeEnabled)
                }
            }
        }
        viewModelScope.launch {
            tracksState.collect { ui ->
                if (!initialQueuePrepared && ui is UiState.Success && ui.data.isNotEmpty() && playbackState.value.currentTrack == null) {
                    initialQueuePrepared = true
                    audioEngine.playQueue(ui.data, startIndex = 0, autoPlay = false)
                }
            }
        }
    }

    private fun observeActiveEqPreset() {
        viewModelScope.launch {
            combine(userProfileState, eqPresetsState) { profileUi, presetsUi ->
                val activeId = (profileUi as? UiState.Success)?.data?.activeEqPresetId ?: "preset_studio_flat"
                val presets = (presetsUi as? UiState.Success)?.data.orEmpty()
                presets.firstOrNull { it.id == activeId } ?: presets.firstOrNull()
            }.collect { matched ->
                if (matched != null) {
                    _liveEqPreset.value = matched
                    audioEngine.applyEqPreset(matched)
                }
            }
        }
    }

    private fun observeTrackStarts() {
        viewModelScope.launch {
            var lastCountedId: String? = null
            audioEngine.onTrackStarted.collect { trackId ->
                if (!trackId.isNullOrBlank() && trackId != lastCountedId) {
                    lastCountedId = trackId
                    repository.incrementPlayCount(currentUserId, trackId)
                }
            }
        }
    }

    private fun checkNetworkConnected(): Boolean {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val active = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(active) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun registerConnectivityListener() {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isDeviceOnline.value = true
            }

            override fun onLost(network: Network) {
                _isDeviceOnline.value = checkNetworkConnected()
            }
        }
        connectivityCallback = callback
        try {
            cm.registerDefaultNetworkCallback(callback)
        } catch (_: Exception) {
        }
    }

    fun playTracks(tracks: List<AudioTrack>, startIndex: Int = 0) {
        val strictOffline = offlineModeEnabled.value
        val playable = if (strictOffline) {
            tracks.filter { localAudioStorage.isPlayableOffline(it) }
        } else {
            tracks.filter { localAudioStorage.resolvePlayableUri(it) != null }
        }
        if (playable.isEmpty()) {
            _snackbarEvents.tryEmit("No hay pistas disponibles en modo offline dentro de esta selección.")
            return
        }
        val targetTrack = tracks.getOrNull(startIndex)
        val mappedIdx = if (targetTrack != null) playable.indexOfFirst { it.id == targetTrack.id }.coerceAtLeast(0) else 0
        audioEngine.playQueue(playable, mappedIdx, autoPlay = true)
    }

    fun togglePlayPause() = audioEngine.togglePlayPause()
    fun nextTrack() = audioEngine.next()
    fun previousTrack() = audioEngine.previous()
    fun seekTo(ms: Long) = audioEngine.seekTo(ms)
    fun toggleShuffle() = audioEngine.toggleShuffle()
    fun cycleRepeatMode() = audioEngine.cycleRepeatMode()

    fun isTrackPlayableNow(track: AudioTrack): Boolean {
        return if (offlineModeEnabled.value) {
            localAudioStorage.isPlayableOffline(track)
        } else {
            localAudioStorage.resolvePlayableUri(track) != null
        }
    }

    fun importAudioUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _isImporting.value = true
            var importedCount = 0
            for (uri in uris) {
                val track = localAudioStorage.importLocalAudioFile(uri, currentUserId)
                if (track != null) {
                    val res = repository.addTrack(currentUserId, track)
                    if (res.isSuccess) importedCount++
                }
            }
            _offlineCacheMb.value = localAudioStorage.getOfflineCacheSizeMb()
            _isImporting.value = false
            if (importedCount > 0) {
                _snackbarEvents.tryEmit("$importedCount pista(s) Hi-Res importadas y guardadas para modo offline.")
            } else {
                _snackbarEvents.tryEmit("No se pudieron importar los archivos de audio seleccionados.")
            }
        }
    }

    fun toggleFavorite(track: AudioTrack) {
        viewModelScope.launch {
            repository.toggleFavorite(currentUserId, track.id, !track.favorite)
        }
    }

    fun toggleTrackOffline(track: AudioTrack) {
        viewModelScope.launch {
            val next = !track.offlineAvailable
            repository.toggleTrackOffline(currentUserId, track.id, next)
            _snackbarEvents.tryEmit(
                if (next) "'${track.title}' disponible sin conexión."
                else "'${track.title}' marcada como solo en línea."
            )
        }
    }

    fun deleteTrack(track: AudioTrack) {
        viewModelScope.launch {
            localAudioStorage.deleteLocalCacheFile(track)
            repository.deleteTrack(currentUserId, track.id)
            _offlineCacheMb.value = localAudioStorage.getOfflineCacheSizeMb()
            _snackbarEvents.tryEmit("Pista eliminada de la biblioteca.")
        }
    }

    fun createPlaylist(name: String, description: String, coverKey: String, offlinePinned: Boolean) {
        viewModelScope.launch {
            val res = repository.createPlaylist(currentUserId, name, description, coverKey, offlinePinned)
            if (res.isSuccess) {
                _snackbarEvents.tryEmit("Lista '$name' creada y sincronizada.")
            } else {
                _snackbarEvents.tryEmit("No se pudo crear la lista.")
            }
        }
    }

    fun addTrackToPlaylist(playlist: Playlist, trackId: String) {
        if (playlist.trackIds.contains(trackId)) {
            _snackbarEvents.tryEmit("La pista ya está en '${playlist.name}'.")
            return
        }
        viewModelScope.launch {
            val updated = (playlist.trackIds + trackId).take(200)
            repository.updatePlaylistTracks(currentUserId, playlist.id, updated)
            _snackbarEvents.tryEmit("Añadida a '${playlist.name}'.")
        }
    }

    fun removeTrackFromPlaylist(playlist: Playlist, index: Int) {
        if (index !in playlist.trackIds.indices) return
        viewModelScope.launch {
            val updated = playlist.trackIds.toMutableList().apply { removeAt(index) }
            repository.updatePlaylistTracks(currentUserId, playlist.id, updated)
        }
    }

    fun movePlaylistTrack(playlist: Playlist, fromIndex: Int, toIndex: Int) {
        if (fromIndex !in playlist.trackIds.indices || toIndex !in playlist.trackIds.indices) return
        viewModelScope.launch {
            val updated = playlist.trackIds.toMutableList()
            val moved = updated.removeAt(fromIndex)
            updated.add(toIndex, moved)
            repository.updatePlaylistTracks(currentUserId, playlist.id, updated)
        }
    }

    fun togglePlaylistOffline(playlist: Playlist) {
        viewModelScope.launch {
            val next = !playlist.offlinePinned
            repository.togglePlaylistOffline(currentUserId, playlist.id, next)
            if (next) {
                val tracks = (tracksState.value as? UiState.Success)?.data.orEmpty()
                playlist.trackIds.forEach { tid ->
                    tracks.firstOrNull { it.id == tid }?.let { t ->
                        if (!t.offlineAvailable) {
                            repository.toggleTrackOffline(currentUserId, t.id, true)
                        }
                    }
                }
            }
            _snackbarEvents.tryEmit(
                if (next) "Lista '${playlist.name}' fijada para modo offline."
                else "Lista '${playlist.name}' desfijada de offline."
            )
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            repository.deletePlaylist(currentUserId, playlist.id)
            _snackbarEvents.tryEmit("Lista '${playlist.name}' eliminada.")
        }
    }

    fun selectTheme(themeMode: SonoraThemeMode) {
        viewModelScope.launch {
            prefsStore.setThemeMode(themeMode)
            repository.updateTheme(currentUserId, themeMode)
        }
    }

    fun toggleOfflineMode(enabled: Boolean) {
        viewModelScope.launch {
            prefsStore.setOfflineMode(enabled)
            repository.updateOfflineMode(currentUserId, enabled)
            _snackbarEvents.tryEmit(
                if (enabled) "Modo Offline activado: reproducción local sin red."
                else "Sincronización en la nube reactivada."
            )
        }
    }

    fun updateAudioEnginePrefs(gapless: Boolean, hiResOutput: Boolean) {
        viewModelScope.launch {
            prefsStore.setGaplessEnabled(gapless)
            prefsStore.setHiResOutputEnabled(hiResOutput)
            audioEngine.setEnginePreferences(hiResOutput)
            repository.updateAudioEnginePrefs(currentUserId, gapless, hiResOutput)
        }
    }

    fun selectEqPreset(preset: EqPreset) {
        _liveEqPreset.value = preset
        audioEngine.applyEqPreset(preset)
        viewModelScope.launch {
            prefsStore.setActiveEqPresetId(preset.id)
            repository.updateActiveEqPresetId(currentUserId, preset.id)
        }
    }

    fun updateLiveEq(transform: (EqPreset) -> EqPreset) {
        val updated = transform(_liveEqPreset.value)
        _liveEqPreset.value = updated
        audioEngine.applyEqPreset(updated)
    }

    fun persistCurrentEqPreset(asNewName: String? = null) {
        val current = _liveEqPreset.value
        viewModelScope.launch {
            val isNew = !asNewName.isNullOrBlank()
            val toSave = if (isNew) {
                current.copy(id = "", name = asNewName!!.trim().take(50))
            } else {
                current
            }
            val result = repository.saveEqPreset(currentUserId, toSave, isNew = isNew)
            result.onSuccess { savedId ->
                prefsStore.setActiveEqPresetId(savedId)
                repository.updateActiveEqPresetId(currentUserId, savedId)
                _snackbarEvents.tryEmit(
                    if (isNew) "Perfil '${toSave.name}' guardado y sincronizado en la nube."
                    else "Ajustes de ecualizador sincronizados."
                )
            }
        }
    }

    fun deleteEqPreset(preset: EqPreset) {
        viewModelScope.launch {
            repository.deleteEqPreset(currentUserId, preset.id)
            _snackbarEvents.tryEmit("Perfil '${preset.name}' eliminado.")
        }
    }

    fun forceCloudSync() {
        if (offlineModeEnabled.value) {
            _snackbarEvents.tryEmit("Desactiva el Modo Offline para sincronizar con la nube.")
            return
        }
        viewModelScope.launch {
            _isSyncing.value = true
            repository.setNetworkEnabled(true)
            val res = repository.forceSyncWait()
            _isSyncing.value = false
            _snackbarEvents.tryEmit(
                if (res.isSuccess) "Biblioteca, listas y ecualizador sincronizados con la nube."
                else "Cambios en cola local; se sincronizarán al recuperar conexión."
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        connectivityCallback?.let { cb ->
            val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            try { cm?.unregisterNetworkCallback(cb) } catch (_: Exception) {}
        }
    }

    companion object {
        private const val TAG = "SonoraVM"
        private const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
