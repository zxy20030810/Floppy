package com.floppy.app.playback

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.floppy.app.domain.AudioItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class ExoPlaybackController(context: Context) : PlaybackController {

    companion object {
        private const val LogTag = "FloppyPlayback"

        @Volatile
        private var sharedInstance: ExoPlaybackController? = null

        /** 进程内单例：旋转重建 Activity 时复用同一个播放器，避免泄漏孤儿 ExoPlayer */
        fun shared(context: Context): ExoPlaybackController {
            return sharedInstance ?: synchronized(this) {
                sharedInstance ?: ExoPlaybackController(context.applicationContext)
                    .also { sharedInstance = it }
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val player = ExoPlayer.Builder(context.applicationContext)
        .setWakeMode(C.WAKE_MODE_NETWORK)  // 锁屏后继续播放（需 manifest WAKE_LOCK 权限）
        .build()
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
                        val nextState = when {
                            isPlaying -> PlaybackState.Playing
                            // 自然播完时 isPlaying 也会变 false，别把 Ended 盖成 Paused
                            player.playbackState == Player.STATE_ENDED -> PlaybackState.Ended
                            else -> PlaybackState.Paused
                        }
                        it.copy(state = nextState)
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    Log.w(LogTag, "ExoPlayer error", error)
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
            // 播完后 ExoPlayer 停在 STATE_ENDED，此时 play() 是空操作 —— 先回到开头再播（重播）
            if (player.playbackState == Player.STATE_ENDED) {
                player.seekTo(0)
            }
            player.play()
            playbackState.update { it.copy(state = PlaybackState.Playing, positionMs = player.currentPosition.coerceAtLeast(0)) }
        }
    }

    override fun stop() {
        player.stop()
        playbackState.value = PlaybackUiState()
    }

    override fun release() {
        synchronized(Companion) {
            if (sharedInstance === this) {
                sharedInstance = null
            }
        }
        scope.cancel()
        player.release()
    }

    private fun String.toPlayableUri(): Uri {
        return Uri.parse(toHttpUrlOrNull()?.toString() ?: this)
    }
}
