package com.floppy.app.ui.video

import android.content.ContentResolver
import android.net.Uri
import androidx.annotation.OptIn
import androidx.annotation.RawRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

@OptIn(UnstableApi::class)
@Composable
fun Mp4VideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = false,
    showControls: Boolean = true,
    loop: Boolean = false,
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT,
    onPlaybackEnded: () -> Unit = {},
    onPlaybackError: (PlaybackException) -> Unit = {}
) {
    Mp4VideoPlayer(
        videoUri = Uri.parse(videoUrl),
        modifier = modifier,
        autoPlay = autoPlay,
        showControls = showControls,
        loop = loop,
        resizeMode = resizeMode,
        onPlaybackEnded = onPlaybackEnded,
        onPlaybackError = onPlaybackError
    )
}

@OptIn(UnstableApi::class)
@Composable
fun Mp4VideoPlayer(
    videoUri: Uri,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = false,
    showControls: Boolean = true,
    loop: Boolean = false,
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT,
    onPlaybackEnded: () -> Unit = {},
    onPlaybackError: (PlaybackException) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestErrorHandler by rememberUpdatedState(onPlaybackError)
    val latestEndedHandler by rememberUpdatedState(onPlaybackEnded)
    val mediaItem = remember(videoUri) { MediaItem.fromUri(videoUri) }
    val player = remember(context) {
        ExoPlayer.Builder(context).build()
    }
    var resumeOnForeground by remember { mutableStateOf(false) }

    LaunchedEffect(player, mediaItem, loop) {
        player.playWhenReady = autoPlay
        player.setMediaItem(mediaItem)
        player.repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        player.prepare()
    }

    LaunchedEffect(player, autoPlay) {
        player.playWhenReady = autoPlay
    }

    DisposableEffect(player) {
        onDispose {
            player.release()
        }
    }

    DisposableEffect(player, lifecycleOwner) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                latestErrorHandler(error)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    latestEndedHandler()
                }
            }
        }
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    resumeOnForeground = player.playWhenReady
                    player.pause()
                }

                Lifecycle.Event.ON_RESUME -> {
                    if (resumeOnForeground) {
                        player.play()
                    }
                }

                else -> Unit
            }
        }

        player.addListener(listener)
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            player.removeListener(listener)
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            PlayerView(viewContext).apply {
                this.player = player
                useController = showControls
                this.resizeMode = resizeMode
                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        },
        update = { playerView ->
            playerView.player = player
            playerView.useController = showControls
            playerView.resizeMode = resizeMode
        }
    )
}

@Composable
fun rememberRawMp4Uri(@RawRes resId: Int): Uri {
    val context = LocalContext.current
    return remember(context, resId) {
        Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(context.packageName)
            .appendPath(resId.toString())
            .build()
    }
}
