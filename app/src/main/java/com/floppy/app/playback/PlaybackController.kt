package com.floppy.app.playback

import com.floppy.app.domain.AudioItem
import kotlinx.coroutines.flow.StateFlow

enum class PlaybackState {
    Idle,
    Buffering,
    Playing,
    Paused,
    Ended,
    Failed
}

data class PlaybackUiState(
    val state: PlaybackState = PlaybackState.Idle,
    val currentAudio: AudioItem? = null,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val errorMessage: String? = null
)

interface PlaybackController {
    val playback: StateFlow<PlaybackUiState>

    fun play(audio: AudioItem)
    fun pause()
    fun resume()
    fun stop()
    fun release()
}
