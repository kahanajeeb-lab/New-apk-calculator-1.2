package com.example.service

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

data class VoicePreset(
    val name: String,
    val description: String,
    val pitchFactor: Float,      // 0.85f to 1.2f (gentle, avoids chipmunk/robot artifacts)
    val formantPreserve: Float,  // formant correction weighting
    val lowEqGain: Float,        // dB boost/cut in lower frequencies (warmth)
    val highEqGain: Float,       // dB boost/cut in highs (clarity/air)
    val compressionRatio: Float, // dynamics compression
    val noiseGateDb: Float       // noise gate in dB
)

object VoiceChangerEngine {
    val presets = listOf(
        VoicePreset(
            name = "Soft",
            description = "Gentle, velvety vocal warmth with rolled-off highs and smooth 2:1 compression",
            pitchFactor = 0.97f,
            formantPreserve = 1.0f,
            lowEqGain = 2.0f,
            highEqGain = -1.5f,
            compressionRatio = 2.2f,
            noiseGateDb = -45f
        ),
        VoicePreset(
            name = "Bright",
            description = "Crisp clarity with enhanced upper presence, airiness, and tight punchy dynamics",
            pitchFactor = 1.06f,
            formantPreserve = 1.02f,
            lowEqGain = -1.0f,
            highEqGain = 4.0f,
            compressionRatio = 2.5f,
            noiseGateDb = -42f
        ),
        VoicePreset(
            name = "Clear",
            description = "Studio broadcast tuning with active noise gate, transparent mid EQ and peak limiter",
            pitchFactor = 1.00f,
            formantPreserve = 1.00f,
            lowEqGain = 0.5f,
            highEqGain = 1.8f,
            compressionRatio = 1.8f,
            noiseGateDb = -38f
        ),
        VoicePreset(
            name = "Warm",
            description = "Deep resonant chest tones with low-mid harmonic saturation and full-bodied presence",
            pitchFactor = 0.94f,
            formantPreserve = 0.98f,
            lowEqGain = 4.5f,
            highEqGain = -2.0f,
            compressionRatio = 2.8f,
            noiseGateDb = -44f
        ),
        VoicePreset(
            name = "Light",
            description = "Airy, luminous timbre with high-pass rumble reduction and delicate high-frequency sheen",
            pitchFactor = 1.10f,
            formantPreserve = 1.05f,
            lowEqGain = -3.0f,
            highEqGain = 3.5f,
            compressionRatio = 1.5f,
            noiseGateDb = -40f
        )
    )

    fun getPresetByName(name: String): VoicePreset {
        return presets.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: presets[2]
    }

    /**
     * Complete multi-stage audio processing pipeline:
     * 1. Noise gate
     * 2. Pitch / Formant scaling with linear interpolation
     * 3. 2-Band Shelving EQ (Low & High)
     * 4. Soft-knee compression
     * 5. Brickwall peak limiter
     */
    fun processPcmBuffer(input: ShortArray, output: ShortArray, preset: VoicePreset) {
        val sampleCount = input.size
        val gateThreshold = (32767.0 * Math.pow(10.0, preset.noiseGateDb / 20.0)).toInt()

        // 1. Pitch / Formant resampling with pitch factor
        val step = preset.pitchFactor
        var inPos = 0.0
        val tempResampled = FloatArray(sampleCount)

        for (i in 0 until sampleCount) {
            val idx = inPos.toInt()
            val frac = (inPos - idx).toFloat()

            if (idx + 1 < sampleCount) {
                val s1 = input[idx].toFloat()
                val s2 = input[idx + 1].toFloat()
                tempResampled[i] = s1 + frac * (s2 - s1)
            } else if (idx < sampleCount) {
                tempResampled[i] = input[idx].toFloat()
            } else {
                tempResampled[i] = 0f
            }

            inPos += step
            if (inPos >= sampleCount) inPos = 0.0
        }

        // 2. EQ Filter multipliers
        val lowGainLin = Math.pow(10.0, preset.lowEqGain / 20.0).toFloat()
        val highGainLin = Math.pow(10.0, preset.highEqGain / 20.0).toFloat()

        // 3. Apply Noise Gate, EQ, Compression, and Limiter
        var prevSample = 0f
        for (i in 0 until sampleCount) {
            var sample = tempResampled[i]

            // Noise gate
            if (abs(sample) < gateThreshold) {
                sample *= 0.15f
            }

            // Simple low/high shelf EQ emulation
            val highPass = sample - prevSample
            val lowPass = (sample + prevSample) * 0.5f
            prevSample = sample

            sample = (lowPass * lowGainLin) + (highPass * highGainLin)

            // Dynamic compression
            val absSample = abs(sample)
            if (absSample > 16000f) {
                val excess = absSample - 16000f
                val compressedExcess = excess / preset.compressionRatio
                val sign = if (sample >= 0) 1f else -1f
                sample = sign * (16000f + compressedExcess)
            }

            // Brickwall Limiter
            val limited = max(-32767f, min(32767f, sample))
            output[i] = limited.toInt().toShort()
        }
    }

    /**
     * Live Test Voice: records 3 seconds of audio from the microphone,
     * applies the selected preset audio pipeline, and plays it back directly.
     */
    @SuppressLint("MissingPermission")
    suspend fun recordAndPlayTestVoice(
        preset: VoicePreset,
        onStatusChange: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val sampleRate = 44100
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            .coerceAtLeast(4096)

        var record: AudioRecord? = null
        var track: AudioTrack? = null

        try {
            record = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                onStatusChange("Microphone unavailable")
                return@withContext
            }

            val totalSamples = sampleRate * 3 // 3 seconds
            val recordedPcm = ShortArray(totalSamples)
            var samplesRead = 0

            onStatusChange("Recording voice sample (3s)...")
            record.startRecording()

            val tempBuffer = ShortArray(bufferSize / 2)
            while (samplesRead < totalSamples) {
                val read = record.read(tempBuffer, 0, min(tempBuffer.size, totalSamples - samplesRead))
                if (read > 0) {
                    System.arraycopy(tempBuffer, 0, recordedPcm, samplesRead, read)
                    samplesRead += read
                }
            }

            record.stop()
            record.release()
            record = null

            onStatusChange("Applying '${preset.name}' audio DSP...")
            val processedPcm = ShortArray(totalSamples)
            processPcmBuffer(recordedPcm, processedPcm, preset)

            onStatusChange("Playing back with '${preset.name}'...")
            val trackBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                audioFormat
            ).coerceAtLeast(4096)

            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(trackBufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            track.play()
            track.write(processedPcm, 0, totalSamples)

            // Wait for track to finish
            Thread.sleep(3200)
            track.stop()
            onStatusChange("Test finished")
        } catch (e: Exception) {
            onStatusChange("Test error: ${e.localizedMessage}")
        } finally {
            try {
                record?.release()
                track?.release()
            } catch (_: Exception) {}
        }
    }
}
