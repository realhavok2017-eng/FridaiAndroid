package com.fridai.app.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * AudioRecorder - Handles voice recording with Voice Activity Detection (VAD)
 */
@Singleton
class AudioRecorder @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel

    companion object {
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val SILENCE_THRESHOLD = 1000
        private const val SILENCE_DURATION_MS = 1500
        private const val MAX_RECORDING_MS = 30000
    }

    /**
     * Start recording with automatic silence detection
     * Returns recorded audio as WAV bytes when done
     */
    suspend fun recordWithVAD(): ByteArray? = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext null

        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        if (bufferSize <= 0) return@withContext null

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize * 2
            )
        } catch (e: SecurityException) {
            return@withContext null
        } catch (e: Exception) {
            return@withContext null
        }

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            audioRecord?.release()
            audioRecord = null
            return@withContext null
        }

        val audioData = ByteArrayOutputStream()
        val buffer = ShortArray(bufferSize / 2)
        var silenceStart = 0L
        var hasVoice = false
        val startTime = System.currentTimeMillis()

        try {
            audioRecord?.startRecording()
            _isRecording.value = true

            while (isActive) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    // Calculate audio level
                    val level = buffer.take(read).map { abs(it.toInt()) }.average().toFloat()
                    _audioLevel.value = level / Short.MAX_VALUE

                    // Convert to bytes and store
                    for (i in 0 until read) {
                        audioData.write(buffer[i].toInt() and 0xFF)
                        audioData.write((buffer[i].toInt() shr 8) and 0xFF)
                    }

                    // Voice activity detection
                    if (level > SILENCE_THRESHOLD) {
                        hasVoice = true
                        silenceStart = 0L
                    } else if (hasVoice) {
                        if (silenceStart == 0L) {
                            silenceStart = System.currentTimeMillis()
                        } else if (System.currentTimeMillis() - silenceStart > SILENCE_DURATION_MS) {
                            // Silence detected after voice - stop recording
                            break
                        }
                    }

                    // Max recording time
                    if (System.currentTimeMillis() - startTime > MAX_RECORDING_MS) {
                        break
                    }
                }
            }
        } finally {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            _isRecording.value = false
            _audioLevel.value = 0f
        }

        if (!hasVoice) return@withContext null

        // Convert PCM to WAV
        val pcmData = audioData.toByteArray()
        createWavFile(pcmData)
    }

    /**
     * Stop recording immediately
     */
    fun stopRecording() {
        recordingJob?.cancel()
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        _isRecording.value = false
    }

    private fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun createWavFile(pcmData: ByteArray): ByteArray {
        val totalDataLen = pcmData.size + 36
        val byteRate = SAMPLE_RATE * 1 * 16 / 8  // mono, 16-bit

        val header = ByteArray(44)

        // RIFF header
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()

        // fmt chunk
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16  // chunk size
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1  // PCM
        header[21] = 0
        header[22] = 1  // mono
        header[23] = 0
        header[24] = (SAMPLE_RATE and 0xff).toByte()
        header[25] = ((SAMPLE_RATE shr 8) and 0xff).toByte()
        header[26] = ((SAMPLE_RATE shr 16) and 0xff).toByte()
        header[27] = ((SAMPLE_RATE shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = 2  // block align
        header[33] = 0
        header[34] = 16  // bits per sample
        header[35] = 0

        // data chunk
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (pcmData.size and 0xff).toByte()
        header[41] = ((pcmData.size shr 8) and 0xff).toByte()
        header[42] = ((pcmData.size shr 16) and 0xff).toByte()
        header[43] = ((pcmData.size shr 24) and 0xff).toByte()

        return header + pcmData
    }
}
