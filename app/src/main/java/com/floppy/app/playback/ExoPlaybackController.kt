package com.floppy.app.playback

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.floppy.app.domain.AudioItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class ExoPlaybackController(context: Context) : PlaybackController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val player = ExoPlayer.Builder(context.applicationContext).build()
    private val playbackState = MutableStateFlow(PlaybackUiState())

    override val playback: StateFlow<PlaybackUiState> = playbackState.asStateFlow()

    private var currentAudio: AudioItem? = null

    init {
        player.addListener(
            object : Player.Listener {
                override fun onPlaybackStateChanged(playbackStateValue: Int) {
                    val nextState = when (playbackStateValue) {
                        Player.STATE_BUFFERING -> PlaybackState.Buffering
                        Player.STATE_READY -> if (player.isPlaying) PlaybackState.Playing else PlaybackState.Paused
                        Player.STATE_ENDED -> PlaybackState.Ended
                        else -> playbackState.value.state
                    }
                    playbackState.update {
                        it.copy(
                            state = nextState,
                            positionMs = player.currentPosition.coerceAtLeast(0),
                            durationMs = player.duration.takeIf { duration -> duration > 0 } ?: it.durationMs,
                            errorMessage = null
                        )
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    playbackState.update {
                        it.copy(state = if (isPlaying) PlaybackState.Playing else PlaybackState.Paused)
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    playbackState.update {
                        it.copy(
                            state = PlaybackState.Failed,
                            errorMessage = "播放失败，请稍后再试"
                        )
                    }
                }
            }
        )

        scope.launch {
            while (isActive) {
                playbackState.update {
                    it.copy(
                        positionMs = player.currentPosition.coerceAtLeast(0),
                        durationMs = player.duration.takeIf { duration -> duration > 0 } ?: it.durationMs
                    )
                }
                delay(500)
            }
        }
    }

    override fun play(audio: AudioItem) {
        currentAudio = audio
        playbackState.value = PlaybackUiState(
            state = PlaybackState.Buffering,
            currentAudio = audio,
            durationMs = audio.durationSeconds * 1000L
        )
        player.setMediaItem(MediaItem.fromUri(audio.streamUrl.toPlayableUri()))
        player.prepare()
        player.playWhenReady = true
    }

    override fun pause() {
        player.pause()
        playbackState.update { it.copy(state = PlaybackState.Paused) }
    }

    override fun resume() {
        if (currentAudio != null) {
            player.play()
            playbackState.update { it.copy(state = PlaybackState.Playing) }
        }
    }

    override fun stop() {
        player.stop()
        playbackState.value = PlaybackUiState()
    }

    override fun release() {
        player.release()
    }

    private fun String.toPlayableUri(): Uri {
        return Uri.parse(toHttpUrlOrNull()?.toString() ?: this)
    }
}
