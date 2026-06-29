package com.floppy.app.data

import com.floppy.app.R
import com.floppy.app.domain.AudioArtwork
import com.floppy.app.domain.AudioItem
import com.floppy.app.domain.AudioLibrary

object FallbackAudioLibrary {
    private const val APP_PACKAGE_NAME = "com.floppy.app"
    private const val FALLBACK_DURATION_SECONDS = 31

    val streamUrl: String = "android.resource://$APP_PACKAGE_NAME/${R.raw.floppy_fallback_audio}"

    fun audio(): AudioItem = AudioItem(
        id = "local-fallback-calm",
        title = "Calm",
        subtitle = "24 Frequency",
        durationSeconds = FALLBACK_DURATION_SECONDS,
        streamUrl = streamUrl,
        artwork = AudioArtwork(
            seedColor = 0xFF7D8CFF,
            prompt = "local fallback sleep audio"
        ),
        category = "My upload",
        playbackProgress = 0.70f
    )

    fun library(): AudioLibrary {
        val audio = audio()
        return AudioLibrary(
            recommended = listOf(audio),
            uploads = emptyList(),
            history = emptyList()
        )
    }

    fun withFallbackIfEmpty(library: AudioLibrary): AudioLibrary {
        val hasPlayableAudio = library.recommended.any { it.streamUrl.isNotBlank() } ||
            library.uploads.any { it.generatedAudio?.streamUrl?.isNotBlank() == true } ||
            library.history.any { it.streamUrl.isNotBlank() }
        return if (hasPlayableAudio) library else library()
    }
}
