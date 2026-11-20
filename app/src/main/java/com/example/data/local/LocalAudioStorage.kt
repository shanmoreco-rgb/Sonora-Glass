package com.example.data.local

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import com.example.data.model.AudioSourceType
import com.example.data.model.AudioTrack
import com.example.data.model.SonoraCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.util.Locale
import java.util.UUID

class LocalAudioStorage(private val context: Context) {

    private val offlineDir: File
        get() = File(context.filesDir, "offline_audio").apply {
            if (!exists()) mkdirs()
        }

    fun resolvePlayableUri(track: AudioTrack): Uri? {
        return when (track.resolvedSourceType) {
            AudioSourceType.BUNDLED_HIRES -> {
                val resId = context.resources.getIdentifier(
                    track.localFileName,
                    "raw",
                    context.packageName
                )
                if (resId != 0) {
                    Uri.parse("android.resource://${context.packageName}/$resId")
                } else null
            }
            AudioSourceType.LOCAL_DEVICE -> {
                if (track.localFileName.isBlank()) return null
                val file = File(offlineDir, track.localFileName)
                if (file.exists() && file.length() > 0L) Uri.fromFile(file) else null
            }
        }
    }

    fun isPlayableOffline(track: AudioTrack): Boolean {
        return resolvePlayableUri(track) != null && track.offlineAvailable
    }

    fun getOfflineCacheSizeMb(): Double {
        val bytes = offlineDir.listFiles()?.sumOf { it.length() } ?: 0L
        return bytes / (1024.0 * 1024.0)
    }

    suspend fun importLocalAudioFile(uri: Uri, userId: String): AudioTrack? = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val displayName = queryDisplayName(uri) ?: "Audio_${System.currentTimeMillis()}"
            val ext = displayName.substringAfterLast('.', "").lowercase(Locale.ROOT)
                .ifBlank { mimeToExt(resolver.getType(uri)) }
            val safeId = "local_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val targetFileName = "$safeId.$ext"
            val destFile = File(offlineDir, targetFileName)

            resolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null

            if (!destFile.exists() || destFile.length() == 0L) {
                return@withContext null
            }

            val meta = extractMetadata(destFile, displayName, ext)
            val coverIdx = Math.abs(displayName.hashCode()) % SonoraCatalog.coverKeys.size

            AudioTrack(
                id = safeId,
                userId = userId,
                title = meta.title.take(120),
                artist = meta.artist.take(120),
                album = meta.album.take(120),
                durationMs = meta.durationMs.coerceIn(0L, 86_400_000L),
                formatBadge = meta.formatBadge.take(50),
                sampleRateHz = meta.sampleRateHz.coerceIn(0L, 768_000L),
                bitDepth = meta.bitDepth.coerceIn(0L, 64L),
                codec = meta.codec.take(20),
                sourceType = AudioSourceType.LOCAL_DEVICE.name,
                localFileName = targetFileName,
                coverKey = SonoraCatalog.coverKeys[coverIdx],
                favorite = false,
                offlineAvailable = true,
                playCount = 0L
            )
        } catch (_: Exception) {
            null
        }
    }

    fun deleteLocalCacheFile(track: AudioTrack) {
        if (track.resolvedSourceType == AudioSourceType.LOCAL_DEVICE && track.localFileName.isNotBlank()) {
            val file = File(offlineDir, track.localFileName)
            if (file.exists()) {
                file.delete()
            }
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        return context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) cursor.getString(idx) else null
            } else null
        }
    }

    private fun mimeToExt(mime: String?): String = when {
        mime == null -> "flac"
        mime.contains("flac") -> "flac"
        mime.contains("wav") || mime.contains("wave") -> "wav"
        mime.contains("mp4") || mime.contains("m4a") -> "m4a"
        mime.contains("ogg") -> "ogg"
        mime.contains("opus") -> "opus"
        mime.contains("mpeg") || mime.contains("mp3") -> "mp3"
        mime.contains("aiff") -> "aiff"
        else -> "audio"
    }

    private data class ParsedAudioMeta(
        val title: String,
        val artist: String,
        val album: String,
        val durationMs: Long,
        val sampleRateHz: Long,
        val bitDepth: Long,
        val codec: String,
        val formatBadge: String
    )

    private fun extractMetadata(file: File, fallbackName: String, ext: String): ParsedAudioMeta {
        val retriever = MediaMetadataRetriever()
        var title = fallbackName.substringBeforeLast('.').ifBlank { "Pista Sin Título" }
        var artist = "Artista Desconocido"
        var album = "Importación Local"
        var durationMs = 0L
        var sampleRate = 48000L
        var bitDepth = 16L
        val codec = ext.uppercase(Locale.ROOT).ifBlank { "AUDIO" }

        try {
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.takeIf { it.isNotBlank() }?.let { title = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.takeIf { it.isNotBlank() }?.let { artist = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                ?.takeIf { it.isNotBlank() }?.let { album = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()?.let { durationMs = it }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
                    ?.toLongOrNull()?.let { sampleRate = it }
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITS_PER_SAMPLE)
                    ?.toLongOrNull()?.let { bitDepth = it }
            }
        } catch (_: Exception) {
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }

        // Direct binary header probe for WAV / FLAC Hi-Res precision on all API levels
        if (ext == "wav") {
            probeWavHeader(file)?.let { (sr, bd) ->
                sampleRate = sr
                bitDepth = bd
            }
        } else if (ext == "flac") {
            probeFlacHeader(file)?.let { (sr, bd) ->
                sampleRate = sr
                bitDepth = bd
            } ?: run {
                if (bitDepth < 24L) bitDepth = 24L
            }
        }

        val srKhz = String.format(Locale.US, "%.1f kHz", sampleRate / 1000.0).replace(".0 ", " ")
        val badge = if (codec in setOf("FLAC", "WAV", "ALAC", "AIFF")) {
            "$codec ${bitDepth}-bit / $srKhz"
        } else {
            "$codec • $srKhz"
        }

        return ParsedAudioMeta(
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs,
            sampleRateHz = sampleRate,
            bitDepth = bitDepth,
            codec = codec,
            formatBadge = badge
        )
    }

    private fun probeWavHeader(file: File): Pair<Long, Long>? {
        return try {
            FileInputStream(file).use { fis ->
                val header = ByteArray(44)
                if (fis.read(header) < 36) return null
                if (String(header, 0, 4) != "RIFF" || String(header, 8, 4) != "WAVE") return null
                val sr = ((header[24].toInt() and 0xFF) or
                        ((header[25].toInt() and 0xFF) shl 8) or
                        ((header[26].toInt() and 0xFF) shl 16) or
                        ((header[27].toInt() and 0xFF) shl 24)).toLong()
                val bits = ((header[34].toInt() and 0xFF) or ((header[35].toInt() and 0xFF) shl 8)).toLong()
                if (sr > 0 && bits > 0) sr to bits else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun probeFlacHeader(file: File): Pair<Long, Long>? {
        return try {
            FileInputStream(file).use { fis ->
                val header = ByteArray(26)
                if (fis.read(header) < 26) return null
                if (String(header, 0, 4) != "fLaC") return null
                // STREAMINFO starts at byte 8; sample rate (20 bits) + channels (3 bits) + bps-1 (5 bits) at bytes 18..21
                val b18 = header[18].toInt() and 0xFF
                val b19 = header[19].toInt() and 0xFF
                val b20 = header[20].toInt() and 0xFF
                val b21 = header[21].toInt() and 0xFF
                val sr = ((b18 shl 12) or (b19 shl 4) or (b20 ushr 4)).toLong()
                val bpsMinus1 = ((b20 and 0x01) shl 4) or (b21 ushr 4)
                val bits = (bpsMinus1 + 1).toLong()
                if (sr > 0 && bits > 0) sr to bits else null
            }
        } catch (_: Exception) {
            null
        }
    }
}
