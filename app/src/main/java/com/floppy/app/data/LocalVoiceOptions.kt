package com.floppy.app.data

import com.floppy.app.R
import com.floppy.app.domain.VoiceOption
import com.floppy.app.domain.VoicePreference

object LocalVoiceOptions {
    fun all(): List<VoiceOption> = VoicePreference.entries.map { preference ->
        VoiceOption(
            id = idFor(preference),
            name = preference.label,
            previewAudioUrl = "android.resource://com.floppy.app/${preference.previewRawRes()}",
            providerVoiceId = idFor(preference),
            provider = "local"
        )
    }

    fun idFor(preference: VoicePreference): String = preference.backendVoiceId()

    private fun VoicePreference.backendVoiceId(): String = when (this) {
        VoicePreference.WarmFemale -> "warm_female"
        VoicePreference.CalmMale -> "warm_male"
        VoicePreference.Neutral -> "neutral"
        VoicePreference.Whisper -> "whisper"
        VoicePreference.Story -> "storyteller_female"
        VoicePreference.Radio -> "radio"
        VoicePreference.Ocean -> "ocean_low"
        VoicePreference.Bright -> "bright"
    }

    private fun VoicePreference.previewRawRes(): Int = when (this) {
        VoicePreference.WarmFemale -> R.raw.voice_warm_female
        VoicePreference.CalmMale -> R.raw.voice_calm_male
        VoicePreference.Neutral -> R.raw.voice_neutral
        VoicePreference.Whisper -> R.raw.voice_whisper
        VoicePreference.Story -> R.raw.voice_story
        VoicePreference.Radio -> R.raw.voice_radio
        VoicePreference.Ocean -> R.raw.voice_ocean
        VoicePreference.Bright -> R.raw.voice_bright
    }
}
