package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.AudioTrack
import com.example.data.model.EqPreset
import com.example.data.model.Playlist
import com.example.data.model.SonoraCatalog
import com.example.data.model.SonoraThemeMode
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class SonoraRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        createConfiguredFirestore(context.applicationContext)
    )

    companion object {
        private const val TAG = "SonoraRepo"

        fun createConfiguredFirestore(context: Context): FirebaseFirestore {
            val dbId = context.getString(R.string.firestore_database_id)
            val firestore = FirebaseFirestore.getInstance(dbId)
            try {
                val settings = FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(
                        PersistentCacheSettings.newBuilder()
                            .setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                            .build()
                    )
                    .build()
                firestore.firestoreSettings = settings
            } catch (e: IllegalStateException) {
                // Settings already configured on an active instance; safe to ignore.
                Log.d(TAG, "Firestore settings already initialized")
            }
            return firestore
        }
    }

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("El usuario debe iniciar sesión con Google antes de acceder a Firestore.")
    }

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    suspend fun setNetworkEnabled(enabled: Boolean): Result<Unit> {
        return try {
            if (enabled) {
                db.enableNetwork().await()
            } else {
                db.disableNetwork().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Failed to toggle Firestore network state=$enabled", e)
            Result.failure(e)
        }
    }

    suspend fun forceSyncWait(): Result<Unit> {
        return try {
            db.waitForPendingWrites().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Pending writes wait failed (possibly offline)", e)
            Result.failure(e)
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "User mismatch in observeUserProfile" }
        val path = "users/$uid"
        emitAll(
            userDoc(uid).snapshots()
                .map { snapshot ->
                    snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    unwrapAndLog(error, OperationType.GET, path, uid)
                    throw error
                }
        )
    }

    fun observeTracks(userId: String): Flow<List<AudioTrack>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "User mismatch in observeTracks" }
        val path = "users/$uid/tracks"
        emitAll(
            userDoc(uid).collection("tracks").snapshots()
                .map { snapshot ->
                    snapshot.toObjects(AudioTrack::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.title.lowercase() }
                }
                .catch { error ->
                    unwrapAndLog(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observePlaylists(userId: String): Flow<List<Playlist>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "User mismatch in observePlaylists" }
        val path = "users/$uid/playlists"
        emitAll(
            userDoc(uid).collection("playlists").snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Playlist::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.name.lowercase() }
                }
                .catch { error ->
                    unwrapAndLog(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeEqPresets(userId: String): Flow<List<EqPreset>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "User mismatch in observeEqPresets" }
        val path = "users/$uid/eq_presets"
        emitAll(
            userDoc(uid).collection("eq_presets").snapshots()
                .map { snapshot ->
                    snapshot.toObjects(EqPreset::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.name.lowercase() }
                }
                .catch { error ->
                    unwrapAndLog(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    private fun unwrapAndLog(error: Throwable, op: OperationType, path: String, uid: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, op, path)
        }
    }

    suspend fun ensureSeededLibrary(userId: String, displayName: String?): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val existingSnap = userDoc(uid).get().await()
            if (existingSnap.exists()) {
                return Result.success(Unit)
            }

            val safeName = (displayName?.trim()?.take(80)).orEmpty().ifBlank { "Audiófilo Sonora" }
            val batch = db.batch()

            batch.set(
                userDoc(uid),
                mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "selectedTheme" to SonoraThemeMode.LIQUID_GLASS.name,
                    "offlineModeEnabled" to false,
                    "gaplessEnabled" to true,
                    "hiResOutputEnabled" to true,
                    "activeEqPresetId" to "preset_studio_flat",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            SonoraCatalog.defaultBundledTracks(uid).forEach { (docId, track) ->
                batch.set(userDoc(uid).collection("tracks").document(docId), trackToCreateMap(uid, track))
            }

            SonoraCatalog.defaultPlaylists(uid).forEach { (docId, playlist) ->
                batch.set(userDoc(uid).collection("playlists").document(docId), playlistToCreateMap(uid, playlist))
            }

            SonoraCatalog.defaultEqPresets(uid).forEach { (docId, preset) ->
                batch.set(userDoc(uid).collection("eq_presets").document(docId), eqPresetToCreateMap(uid, preset))
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateTheme(userId: String, themeMode: SonoraThemeMode): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            userDoc(uid).update(
                mapOf(
                    "selectedTheme" to themeMode.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateOfflineMode(userId: String, offlineEnabled: Boolean): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            // Update local cache first if disabling network, or enable network first if turning online
            if (!offlineEnabled) {
                db.enableNetwork().await()
            }
            userDoc(uid).update(
                mapOf(
                    "offlineModeEnabled" to offlineEnabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            if (offlineEnabled) {
                db.disableNetwork().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateAudioEnginePrefs(
        userId: String,
        gaplessEnabled: Boolean,
        hiResOutputEnabled: Boolean
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            userDoc(uid).update(
                mapOf(
                    "gaplessEnabled" to gaplessEnabled,
                    "hiResOutputEnabled" to hiResOutputEnabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateActiveEqPresetId(userId: String, presetId: String): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            require(presetId.isNotBlank() && presetId.length <= 128)
            userDoc(uid).update(
                mapOf(
                    "activeEqPresetId" to presetId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun addTrack(userId: String, track: AudioTrack): Result<Unit> {
        val trackId = track.id.ifBlank { "trk_${UUID.randomUUID().toString().replace("-", "").take(16)}" }
        val path = "users/$userId/tracks/$trackId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            userDoc(uid).collection("tracks").document(trackId)
                .set(trackToCreateMap(uid, track))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleFavorite(userId: String, trackId: String, favorite: Boolean): Result<Unit> {
        val path = "users/$userId/tracks/$trackId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            require(trackId.isNotBlank())
            userDoc(uid).collection("tracks").document(trackId).update(
                mapOf(
                    "favorite" to favorite,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleTrackOffline(userId: String, trackId: String, offlineAvailable: Boolean): Result<Unit> {
        val path = "users/$userId/tracks/$trackId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            require(trackId.isNotBlank())
            userDoc(uid).collection("tracks").document(trackId).update(
                mapOf(
                    "offlineAvailable" to offlineAvailable,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun incrementPlayCount(userId: String, trackId: String): Result<Unit> {
        val path = "users/$userId/tracks/$trackId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            require(trackId.isNotBlank())
            userDoc(uid).collection("tracks").document(trackId).update(
                mapOf(
                    "playCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteTrack(userId: String, trackId: String): Result<Unit> {
        val path = "users/$userId/tracks/$trackId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            require(trackId.isNotBlank())
            userDoc(uid).collection("tracks").document(trackId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun createPlaylist(
        userId: String,
        name: String,
        description: String,
        coverKey: String,
        offlinePinned: Boolean,
        initialTrackIds: List<String> = emptyList()
    ): Result<String> {
        val playlistId = "pl_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val path = "users/$userId/playlists/$playlistId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val cleanName = name.trim().take(60)
            require(cleanName.isNotEmpty()) { "El nombre de la lista no puede estar vacío." }
            val cleanDesc = description.trim().take(240)
            val safeCover = coverKey.ifBlank { "liquid_aurora" }.take(64)
            val playlist = Playlist(
                userId = uid,
                name = cleanName,
                description = cleanDesc,
                coverKey = safeCover,
                trackIds = initialTrackIds.take(200),
                offlinePinned = offlinePinned
            )
            userDoc(uid).collection("playlists").document(playlistId)
                .set(playlistToCreateMap(uid, playlist))
                .await()
            Result.success(playlistId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updatePlaylistTracks(
        userId: String,
        playlistId: String,
        trackIds: List<String>
    ): Result<Unit> {
        val path = "users/$userId/playlists/$playlistId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            require(playlistId.isNotBlank())
            userDoc(uid).collection("playlists").document(playlistId).update(
                mapOf(
                    "trackIds" to trackIds.take(200),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun togglePlaylistOffline(
        userId: String,
        playlistId: String,
        offlinePinned: Boolean
    ): Result<Unit> {
        val path = "users/$userId/playlists/$playlistId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            require(playlistId.isNotBlank())
            userDoc(uid).collection("playlists").document(playlistId).update(
                mapOf(
                    "offlinePinned" to offlinePinned,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updatePlaylistDetails(
        userId: String,
        playlistId: String,
        name: String,
        description: String,
        coverKey: String
    ): Result<Unit> {
        val path = "users/$userId/playlists/$playlistId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val cleanName = name.trim().take(60)
            require(cleanName.isNotEmpty())
            userDoc(uid).collection("playlists").document(playlistId).update(
                mapOf(
                    "name" to cleanName,
                    "description" to description.trim().take(240),
                    "coverKey" to coverKey.ifBlank { "liquid_aurora" }.take(64),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deletePlaylist(userId: String, playlistId: String): Result<Unit> {
        val path = "users/$userId/playlists/$playlistId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            require(playlistId.isNotBlank())
            userDoc(uid).collection("playlists").document(playlistId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun saveEqPreset(userId: String, preset: EqPreset, isNew: Boolean): Result<String> {
        val presetId = if (isNew || preset.id.isBlank()) {
            "eq_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        } else {
            preset.id
        }
        val path = "users/$userId/eq_presets/$presetId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val cleanName = preset.name.trim().take(50).ifBlank { "Perfil Personalizado" }
            val normalizedBands = (0 until 10).map { i ->
                (preset.bandGainsDb.getOrNull(i) ?: 0.0).coerceIn(-12.0, 12.0)
            }
            val normalizedPreset = preset.copy(
                id = presetId,
                userId = uid,
                name = cleanName,
                bandGainsDb = normalizedBands,
                bassBoostStrength = preset.bassBoostStrength.coerceIn(0L, 1000L),
                virtualizerStrength = preset.virtualizerStrength.coerceIn(0L, 1000L),
                preampDb = preset.preampDb.coerceIn(-12.0, 12.0),
                loudnessGainMb = preset.loudnessGainMb.coerceIn(0L, 2000L),
                reverbPreset = preset.reverbPreset.coerceIn(0L, 6L)
            )

            val docRef = userDoc(uid).collection("eq_presets").document(presetId)
            if (isNew) {
                docRef.set(eqPresetToCreateMap(uid, normalizedPreset)).await()
            } else {
                docRef.update(
                    mapOf(
                        "name" to normalizedPreset.name,
                        "bandGainsDb" to normalizedPreset.bandGainsDb,
                        "bassBoostStrength" to normalizedPreset.bassBoostStrength,
                        "virtualizerStrength" to normalizedPreset.virtualizerStrength,
                        "preampDb" to normalizedPreset.preampDb,
                        "loudnessGainMb" to normalizedPreset.loudnessGainMb,
                        "reverbPreset" to normalizedPreset.reverbPreset,
                        "enabled" to normalizedPreset.enabled,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(presetId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isNew) OperationType.CREATE else OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteEqPreset(userId: String, presetId: String): Result<Unit> {
        val path = "users/$userId/eq_presets/$presetId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            require(presetId.isNotBlank())
            userDoc(uid).collection("eq_presets").document(presetId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    private fun trackToCreateMap(uid: String, track: AudioTrack): Map<String, Any> = mapOf(
        "userId" to uid,
        "title" to track.title.ifBlank { "Pista Sin Título" }.take(120),
        "artist" to track.artist.ifBlank { "Artista Desconocido" }.take(120),
        "album" to track.album.take(120),
        "durationMs" to track.durationMs.coerceIn(0L, 86_400_000L),
        "formatBadge" to track.formatBadge.ifBlank { "PCM Audio" }.take(50),
        "sampleRateHz" to track.sampleRateHz.coerceIn(0L, 768_000L),
        "bitDepth" to track.bitDepth.coerceIn(0L, 64L),
        "codec" to track.codec.ifBlank { "AUDIO" }.take(20),
        "sourceType" to track.sourceType,
        "localFileName" to track.localFileName.take(255),
        "coverKey" to track.coverKey.ifBlank { "liquid_aurora" }.take(64),
        "favorite" to track.favorite,
        "offlineAvailable" to track.offlineAvailable,
        "playCount" to track.playCount.coerceAtLeast(0L),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun playlistToCreateMap(uid: String, playlist: Playlist): Map<String, Any> = mapOf(
        "userId" to uid,
        "name" to playlist.name.ifBlank { "Mi Lista" }.take(60),
        "description" to playlist.description.take(240),
        "coverKey" to playlist.coverKey.ifBlank { "liquid_aurora" }.take(64),
        "trackIds" to playlist.trackIds.take(200),
        "offlinePinned" to playlist.offlinePinned,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun eqPresetToCreateMap(uid: String, preset: EqPreset): Map<String, Any> = mapOf(
        "userId" to uid,
        "name" to preset.name.ifBlank { "Perfil EQ" }.take(50),
        "bandGainsDb" to (0 until 10).map { i -> (preset.bandGainsDb.getOrNull(i) ?: 0.0).coerceIn(-12.0, 12.0) },
        "bassBoostStrength" to preset.bassBoostStrength.coerceIn(0L, 1000L),
        "virtualizerStrength" to preset.virtualizerStrength.coerceIn(0L, 1000L),
        "preampDb" to preset.preampDb.coerceIn(-12.0, 12.0),
        "loudnessGainMb" to preset.loudnessGainMb.coerceIn(0L, 2000L),
        "reverbPreset" to preset.reverbPreset.coerceIn(0L, 6L),
        "enabled" to preset.enabled,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}
