package com.example.service

import android.media.AudioAttributes
import android.media.MediaPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AudioPlayerHelper {
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    var currentPlayingUrl: String? = null
        private set

    fun play(
        url: String,
        onProgress: (progress: Float, currentMs: Long, totalMs: Long) -> Unit,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) {
        stop()
        try {
            currentPlayingUrl = url
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(url)
                setOnPreparedListener { mp ->
                    mp.start()
                    val duration = mp.duration.toLong().coerceAtLeast(1L)
                    progressJob = CoroutineScope(Dispatchers.Main).launch {
                        while (isActive && mp.isPlaying) {
                            val current = mp.currentPosition.toLong()
                            onProgress(current.toFloat() / duration.toFloat(), current, duration)
                            delay(100)
                        }
                    }
                }
                setOnCompletionListener {
                    progressJob?.cancel()
                    currentPlayingUrl = null
                    onComplete()
                }
                setOnErrorListener { _, _, _ ->
                    progressJob?.cancel()
                    currentPlayingUrl = null
                    onError("Playback failed")
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            currentPlayingUrl = null
            onError(e.message ?: "Failed to initialize playback")
        }
    }

    fun pause() {
        try {
            mediaPlayer?.pause()
        } catch (_: Exception) {}
    }

    fun resume() {
        try {
            mediaPlayer?.start()
        } catch (_: Exception) {}
    }

    fun isPlaying(): Boolean = mediaPlayer?.isPlaying == true

    fun stop() {
        progressJob?.cancel()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        currentPlayingUrl = null
    }
}
