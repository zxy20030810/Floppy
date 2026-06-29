package com.floppy.app.data

import android.net.Uri
import com.floppy.app.domain.AgeRange
import com.floppy.app.domain.AppSettings
import com.floppy.app.domain.AudioArtwork
import com.floppy.app.domain.AudioItem
import com.floppy.app.domain.AudioLibrary
import com.floppy.app.domain.AudioSource
import com.floppy.app.domain.CompanionStyle
import com.floppy.app.domain.ContentPreference
import com.floppy.app.domain.Feedback
import com.floppy.app.domain.GenerationStatus
import com.floppy.app.domain.GenerationTask
import com.floppy.app.domain.RecommendationResult
import com.floppy.app.domain.SleepIssue
import com.floppy.app.domain.UploadItem
import com.floppy.app.domain.UploadStatus
import com.floppy.app.domain.UserProfile
import com.floppy.app.domain.VoiceOption
import com.floppy.app.domain.VoicePreference
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

class MockFloppyRepository(
    initialProfile: UserProfile? = null,
    private val onProfileSaved: ((UserProfile) -> Unit)? = null,
    private val textIntentClient: DemoTextIntentClient? = null
) : FloppyRepository {
    private val fallbackAudio = FallbackAudioLibrary.audio()
    private val recommended = listOf(
        fallbackAudio,
        AudioItem(
            id = "rain-window",
            title = "Friday Fever",
            subtitle = "24 Frequency",
            durationSeconds = fallbackAudio.durationSeconds,
            streamUrl = fallbackAudio.streamUrl,
            artwork = AudioArtwork(
                seedColor = 0xFFFF563F,
                prompt = "red cinematic Friday fever album cover",
            ),
            category = "Metal music",
            playbackProgress = 0.85f
        ),
        AudioItem(
            id = "blue-drive",
            title = "Night Drive",
            subtitle = "24 Frequency",
            durationSeconds = fallbackAudio.durationSeconds,
            streamUrl = fallbackAudio.streamUrl,
            artwork = AudioArtwork(
                seedColor = 0xFF00A7FF,
                prompt = "blue chrome sculpture in motion",
            ),
            category = "Metal music",
            playbackProgress = 0.90f
        ),
        AudioItem(
            id = "orange-sunset",
            title = "Solar Rest",
            subtitle = "24 Frequency",
            durationSeconds = fallbackAudio.durationSeconds,
            streamUrl = fallbackAudio.streamUrl,
            artwork = AudioArtwork(
                seedColor = 0xFFFF7A1A,
                prompt = "orange sunset silhouette and quiet dream",
            ),
            category = "Absolute music",
            playbackProgress = 0.75f
        ),
        AudioItem(
            id = "pixel-tree",
            title = "Pixel Bloom",
            subtitle = "24 Frequency",
            durationSeconds = fallbackAudio.durationSeconds,
            streamUrl = fallbackAudio.streamUrl,
            artwork = AudioArtwork(
                seedColor = 0xFFFFB86C,
                prompt = "pixel tree growing from a calm head",
            ),
            category = "Absolute music",
            playbackProgress = 0.55f
        )
    )

    private val defaultProfile = UserProfile(
        ageRange = AgeRange.From25To34,
        sleepIssues = setOf(SleepIssue.RacingThoughts, SleepIssue.Stress),
        contentPreferences = setOf(ContentPreference.WhiteNoise, ContentPreference.PsychologicalHealing),
        companionStyle = CompanionStyle.Gentle
    )

    private val profileState = MutableStateFlow<UserProfile?>(initialProfile)
    private val initialUploads = listOf(
        UploadItem(
            id = "upload-failed-demo",
            fileName = "File name. word",
            fileType = "mp3",
            sizeLabel = "18.6M",
            progress = 0.68f,
            status = UploadStatus.Failed,
            message = "Upload failed-The file is too large"
        ),
        UploadItem(
            id = "upload-complete-1",
            fileName = "File name. word",
            fileType = "word",
            sizeLabel = "2.4M",
            progress = 1f,
            status = UploadStatus.Completed,
            generatedAudio = recommended.first().copy(id = "upload-audio-1", source = AudioSource.Upload)
        ),
        UploadItem(
            id = "upload-complete-2",
            fileName = "File name. word",
            fileType = "word",
            sizeLabel = "2.4M",
            progress = 1f,
            status = UploadStatus.Completed,
            generatedAudio = recommended[1].copy(id = "upload-audio-2", source = AudioSource.Upload)
        ),
        UploadItem(
            id = "upload-complete-3",
            fileName = "File name. word",
            fileType = "word",
            sizeLabel = "2.4M",
            progress = 1f,
            status = UploadStatus.Completed,
            generatedAudio = recommended[2].copy(id = "upload-audio-3", source = AudioSource.Upload)
        )
    )

    private val libraryState = MutableStateFlow(
        AudioLibrary(
            recommended = recommended,
            uploads = initialUploads,
            history = recommended.take(4)
        )
    )
    private val settingsState = MutableStateFlow(AppSettings(profile = initialProfile ?: defaultProfile))
    private val taskPollCounts = mutableMapOf<String, Int>()
    private val taskPrompts = mutableMapOf<String, String>()

    override val profile: Flow<UserProfile?> = profileState.asStateFlow()
    override val library: Flow<AudioLibrary> = libraryState.asStateFlow()
    override val settings: Flow<AppSettings> = settingsState.asStateFlow()

    override suspend fun saveProfile(profile: UserProfile) {
        delay(250)
        profileState.value = profile
        settingsState.update { it.copy(profile = profile) }
        onProfileSaved?.invoke(profile)
    }

    override suspend fun updateSettings(settings: AppSettings) {
        delay(200)
        settingsState.value = settings
        profileState.value = settings.profile
        onProfileSaved?.invoke(settings.profile)
    }

    override suspend fun recommend(profile: UserProfile): RecommendationResult {
        delay(600)
        return if (profile.sleepIssues.contains(SleepIssue.RacingThoughts) && profile.contentPreferences.contains(ContentPreference.WhiteNoise)) {
            RecommendationResult.Ready(recommended.first())
        } else {
            RecommendationResult.NeedsGeneration(
                prompt = "为 ${profile.bedtime} 入睡的用户生成一段 ${profile.companionStyle.label} 的睡前音频"
            )
        }
    }

    override suspend fun createGenerationTask(prompt: String, profile: UserProfile): GenerationTask {
        delay(350)
        val id = "task-${UUID.randomUUID()}"
        taskPollCounts[id] = 0
        taskPrompts[id] = prompt
        return GenerationTask(
            id = id,
            status = GenerationStatus.Pending,
            message = "Floppy 已收到你的睡前音频请求"
        )
    }

    override suspend fun pollGenerationTask(taskId: String): GenerationTask {
        delay(700)
        val pollCount = (taskPollCounts[taskId] ?: 0) + 1
        taskPollCounts[taskId] = pollCount
        return when {
            pollCount == 1 -> GenerationTask(
                id = taskId,
                status = GenerationStatus.Generating,
                message = "正在整理今晚的故事节奏"
            )

            pollCount < 4 -> GenerationTask(
                id = taskId,
                status = GenerationStatus.Generating,
                message = "正在混合人声、呼吸引导和环境音"
            )

            else -> {
                val audio = AudioItem(
                    id = "generated-$taskId",
                    title = "为你生成的晚安片段",
                    subtitle = taskPrompts[taskId] ?: "1 分钟试听片段已准备好",
                    durationSeconds = fallbackAudio.durationSeconds,
                    streamUrl = fallbackAudio.streamUrl,
                    artwork = AudioArtwork(
                        seedColor = 0xFF7D6BFF,
                        prompt = taskPrompts[taskId] ?: "sleep audio generated cover"
                    ),
                    source = AudioSource.Generated,
                    category = "AI generated",
                    isGenerated = true
                )
                GenerationTask(
                    id = taskId,
                    status = GenerationStatus.Success,
                    message = "试听片段已完成",
                    audio = audio
                )
            }
        }
    }

    override suspend fun addToHistory(audio: AudioItem) {
        libraryState.update { library ->
            val nextAudio = audio.copy(playbackProgress = if (audio.playbackProgress > 0f) audio.playbackProgress else 0.08f)
            library.copy(history = listOf(nextAudio) + library.history.filterNot { it.id == audio.id })
        }
    }

    override suspend fun refreshLibrary() {
        delay(100)
    }

    override suspend fun getVoiceOptions(): List<VoiceOption> {
        delay(100)
        return VoicePreference.entries.map { preference ->
            VoiceOption(
                id = preference.toBackendVoiceId(),
                name = preference.label,
                previewAudioUrl = fallbackAudio.streamUrl,
                providerVoiceId = "minimax_${preference.toBackendVoiceId()}",
                provider = "minimax"
            )
        }
    }

    override suspend fun saveVoiceSelection(voiceId: String) {
        delay(120)
        val selectedPreference = VoicePreference.entries.firstOrNull { it.toBackendVoiceId() == voiceId }
            ?: return
        val nextProfile = settingsState.value.profile.copy(voicePreference = selectedPreference)
        settingsState.update { settings ->
            settings.copy(profile = nextProfile)
        }
        profileState.value = settingsState.value.profile
        onProfileSaved?.invoke(nextProfile)
    }

    override suspend fun startUpload(uri: Uri, fileName: String, fileType: String, mimeType: String?) {
        delay(200)
        val isPlayableAudio = fileType.equals("mp3", ignoreCase = true) ||
            fileType.equals("wav", ignoreCase = true) ||
            fileType.equals("m4a", ignoreCase = true) ||
            mimeType?.startsWith("audio/", ignoreCase = true) == true
        val generatedAudio = if (isPlayableAudio) {
            recommended.first().copy(
                id = "audio-upload-${UUID.randomUUID()}",
                title = fileName,
                subtitle = "Uploaded file",
                durationSeconds = 0,
                source = AudioSource.Upload,
                category = "My upload",
                artwork = AudioArtwork(
                    seedColor = 0xFF18D66B,
                    prompt = "Uploaded audio file"
                )
            )
        } else {
            null
        }
        val upload = UploadItem(
            id = "upload-${UUID.randomUUID()}",
            fileName = fileName,
            fileType = fileType,
            sizeLabel = "2.4M",
            progress = 1f,
            status = UploadStatus.Completed,
            message = if (isPlayableAudio) null else "待生成音频",
            generatedAudio = generatedAudio
        )
        libraryState.update { library ->
            library.copy(uploads = listOf(upload) + library.uploads)
        }
    }

    override suspend fun retryUpload(uploadId: String) {
        delay(200)
        libraryState.update { library ->
            library.copy(
                uploads = library.uploads.map { upload ->
                    if (upload.id == uploadId) {
                        upload.copy(status = UploadStatus.Uploading, message = null, progress = 0.68f)
                    } else {
                        upload
                    }
                }
            )
        }
    }

    override suspend fun completeUpload(uploadId: String) {
        delay(250)
        libraryState.update { library ->
            library.copy(
                uploads = library.uploads.map { upload ->
                    if (upload.id == uploadId) {
                        val audio = recommended.first().copy(
                            id = "audio-${upload.id}",
                            title = "Calm",
                            subtitle = "Generated from ${upload.fileName}",
                            source = AudioSource.Upload,
                            category = "My upload",
                            artwork = AudioArtwork(
                                seedColor = 0xFF18D66B,
                                prompt = "AI generated cover from uploaded bedtime audio"
                            )
                        )
                        upload.copy(
                            status = UploadStatus.Completed,
                            progress = 1f,
                            message = null,
                            generatedAudio = audio
                        )
                    } else {
                        upload
                    }
                }
            )
        }
    }

    override suspend fun submitFeedback(feedback: Feedback): FeedbackResponse {
        delay(300)
        return FeedbackResponse(
            accepted = true,
            message = if (feedback.rating >= 4) "谢谢，Floppy 会记住这类睡前内容" else "收到，Floppy 下次会避开这种感觉"
        )
    }

    override suspend fun submitTextIntent(request: TextIntentRequest): TextIntentResponse {
        textIntentClient?.let { client ->
            return client.submitTextIntent(request)
        }

        delay(500)
        val text = request.text.trim()
        val reply = when {
            text.contains("推荐") || text.contains("睡") || text.contains("放松") ->
                "我收到啦。后端联调后，这句话会进入同一个文本意图接口，返回推荐、生成或聊天结果。"
            request.source == TextIntentSources.Voice ->
                "我听到了：“$text”。后面会把这段文本交给后端判断今晚需要什么。"
            else ->
                "我收到：“$text”。后端接上后，我会根据它返回真正的回复或动作。"
        }
        return TextIntentResponse(
            action = "no_match",
            reply = reply,
            clientRequestId = request.clientRequestId,
            turnIndex = request.turnIndex
        )
    }

    override suspend fun transcribeSpeech(uri: Uri, fileName: String, mimeType: String?): String {
        delay(600)
        return "我今晚想听一点放松的内容"
    }
}

private fun VoicePreference.toBackendVoiceId(): String = when (this) {
    VoicePreference.WarmFemale -> "warm_female"
    VoicePreference.CalmMale -> "warm_male"
    VoicePreference.Neutral -> "neutral"
    VoicePreference.Whisper -> "whisper"
    VoicePreference.Story -> "storyteller_female"
    VoicePreference.Radio -> "radio"
    VoicePreference.Ocean -> "ocean_low"
    VoicePreference.Bright -> "bright"
}
