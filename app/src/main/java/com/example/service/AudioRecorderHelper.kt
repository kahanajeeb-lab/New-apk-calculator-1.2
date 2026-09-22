package com.example.service

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class AudioRecorderHelper(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    var currentFile: File? = null
        private set
    var isRecording: Boolean = false
        private set
    private var startTime: Long = 0L

    fun startRecording(): Boolean {
        return try {
            val audioDir = File(context.cacheDir, "audio_records").apply { mkdirs() }
            val file = File(audioDir, "rec_${System.currentTimeMillis()}.m4a")
            currentFile = file

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            isRecording = true
            startTime = System.currentTimeMillis()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            isRecording = false
            currentFile = null
            false
        }
    }

    fun stopRecording(): Pair<File?, Long> {
        val duration = System.currentTimeMillis() - startTime
        return try {
            if (isRecording) {
                mediaRecorder?.apply {
                    stop()
                    release()
                }
            }
            mediaRecorder = null
            isRecording = false
            Pair(currentFile, duration)
        } catch (e: Exception) {
            mediaRecorder = null
            isRecording = false
            Pair(null, 0L)
        }
    }

    fun cancelRecording() {
        try {
            if (isRecording) {
                mediaRecorder?.apply {
                    stop()
                    release()
                }
            }
        } catch (_: Exception) {}
        mediaRecorder = null
        isRecording = false
        currentFile?.delete()
        currentFile = null
    }

    fun getMaxAmplitude(): Int {
        return try {
            if (isRecording) mediaRecorder?.maxAmplitude ?: 0 else 0
        } catch (_: Exception) {
            0
        }
    }
}
