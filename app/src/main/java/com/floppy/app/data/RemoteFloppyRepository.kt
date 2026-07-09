package com.floppy.app.data

import android.content.Context
import android.net.Uri
import com.floppy.app.domain.AppSettings
import com.floppy.app.domain.AudioArtwork
import com.floppy.app.domain.AudioItem
import com.floppy.app.domain.AudioLibrary
import com.floppy.app.domain.AudioSource
import com.floppy.app.domain.AgeRange
import com.floppy.app.domain.CareerChoice
import com.floppy.app.domain.CompanionStyle
import com.floppy.app.domain.ContentPreference
import com.floppy.app.domain.Feedback
import com.floppy.app.domain.Gender
import com.floppy.app.domain.GenerationTask
import com.floppy.app.domain.RecommendationResult
import com.floppy.app.domain.SleepIssue
import com.floppy.app.domain.UploadItem
import com.floppy.app.domain.UploadStatus
import com.floppy.app.domain.UserProfile
import com.floppy.app.domain.VoiceOption
import com.floppy.app.domain.VoicePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import java.io.File
import java.io.IOException
import java.util.Locale
import java.util.UUID

private val RecommendationGenerationActions = setOf("generate", "generation", "generate_job", "needs_generation")
private val RecommendationReadyActions = setOf("ready", "play", "play_asset", "recommendation", "audio")

class RemoteFloppyRepository(
    context: Context,
    private val api: FloppyApi,
    override val streamingSpeechClient: StreamingSpeechClient?,
    override val realtimeCallClient: RealtimeCallClient? = null,
    override val userId: String,
    private val baseUrl: String,
    initialProfile: UserProfile? = null,
    private val onProfileSaved: ((UserProfile) -> Unit)? = null
) : FloppyRepository {
    private val appContext = context.applicationContext
    private val profileState = MutableStateFlow<UserProfile?>(initialProfile)
    private val libraryState = MutableStateFlow(FallbackAudioLibrary.library())
    private val settingsState = MutableStateFlow(AppSettings(profile = initialProfile ?: UserProfile()))
    private val playbackRecordIdsByAudioId = mutableMapOf<String, String>()

    override val profile: Flow<UserProfile?> = profileState.asStateFlow()
    override val library: Flow<AudioLibrary> = libraryState.asStateFlow()
    override val settings: Flow<AppSettings> = settingsState.asStateFlow()

    override suspend fun saveProfile(profile: UserProfile) {
        profileState.value = profile
        settingsState.update { it.copy(profile = profile) }
        onProfileSaved?.invoke(profile)

        api.saveQuestionnaire(userId, profile.toQuestionnaireRequest())
        api.saveProfile(userId, profile.toBackendProfileRequest())

        profile.companionPrompt.trim().takeIf { it.isNotEmpty() }?.let { prompt ->
            runCatching {
                api.recordEvent(
                    userId,
                    BackendEventRequest(
                        eventType = "conversation_signal",
                        payload = mapOf(
                            "signal_type" to "companion_prompt",
                            "value" to prompt,
                            "confidence" to 1.0,
                            "source" to "onboarding"
                        )
                    )
                )
            }
        }
    }

    override suspend fun updateSettings(settings: AppSettings) {
        settingsState.value = api.updateSettings(settings)
        profileState.value = settingsState.value.profile
        onProfileSaved?.invoke(settingsState.value.profile)
    }

    override suspend fun recommend(profile: UserProfile): RecommendationResult = api.recommend(profile).toRecommendationResult()

    override suspend fun createGenerationTask(prompt: String, profile: UserProfile): GenerationTask =
        api.createGenerationTask(GenerationRequest(prompt, profile))

    override suspend fun pollGenerationTask(taskId: String): GenerationTask {
        val task = api.getGenerationTask(taskId)
        // 成品音频与"完成提示音"可能是相对路径，补全成完整地址才播得出来
        return task.copy(
            audio = task.audio?.withPlayableUrl(),
            notifyAudioUrl = task.notifyAudioUrl?.absoluteBackendUrl()
        )
    }

    override suspend fun addToHistory(audio: AudioItem) {
        val reported = audio.withPlayableUrl()
        runCatching {
            api.startPlayback(
                userId,
                PlaybackStartRequest(
                    audioId = audio.id,
                    source = audio.source.name,
                    durationSeconds = audio.durationSeconds,
                    playbackProgress = audio.playbackProgress
                )
            )
        }.onSuccess { response ->
            response.recordId?.takeIf { it.isNotBlank() }?.let { recordId ->
                playbackRecordIdsByAudioId[audio.id] = recordId
            }
        }
        libraryState.update { library ->
            library.copy(history = listOf(reported) + library.history.filterNot { it.id == reported.id })
        }
    }

    override suspend fun refreshLibrary() {
        libraryState.value = FallbackAudioLibrary.withFallbackIfEmpty(api.getAudioLibrary(userId).withPlayableUrls())
    }

    override suspend fun getVoiceOptions(): List<VoiceOption> {
        return LocalVoiceOptions.all()
    }

    override suspend fun saveVoiceSelection(voiceId: String) {
        val selectedPreference = VoicePreference.entries.firstOrNull { LocalVoiceOptions.idFor(it) == voiceId }
            ?: return
        val nextSettings = settingsState.value.copy(
            profile = settingsState.value.profile.copy(voicePreference = selectedPreference)
        )
        settingsState.value = nextSettings
        profileState.value = nextSettings.profile
        onProfileSaved?.invoke(nextSettings.profile)
    }

    override suspend fun startUpload(uri: Uri, fileName: String, fileType: String, mimeType: String?) {
        val pendingId = "remote-pending-${UUID.randomUUID()}"
        libraryState.update { library ->
            library.copy(
                uploads = listOf(
                    UploadItem(
                        id = pendingId,
                        fileName = fileName,
                        fileType = fileType,
                        sizeLabel = "",
                        progress = 0.12f,
                        status = UploadStatus.Uploading
                    )
                ) + library.uploads
            )
        }

        try {
            val body = appContext.uriRequestBody(uri, mimeType)
            val part = MultipartBody.Part.createFormData("file", fileName, body)
            val uploaded = api.uploadFile(userId, part).withPlayableUrls()
            libraryState.update { library ->
                library.copy(
                    uploads = listOf(uploaded) + library.uploads.filterNot { it.id == pendingId || it.id == uploaded.id }
                )
            }
        } catch (error: Exception) {
            libraryState.update { library ->
                library.copy(
                    uploads = library.uploads.map { upload ->
                        if (upload.id == pendingId) {
                            upload.copy(
                                status = UploadStatus.Failed,
                                progress = 0f,
                                message = "Upload failed"
                            )
                        } else {
                            upload
                        }
                    }
                )
            }
            throw error
        }
    }

    override suspend fun retryUpload(uploadId: String) {
        val retried = api.retryUpload(userId, uploadId).withPlayableUrls()
        libraryState.update { library ->
            library.copy(
                uploads = library.uploads.map { upload ->
                    if (upload.id == uploadId) retried else upload
                }
            )
        }
    }

    override suspend fun completeUpload(uploadId: String) {
        val completed = api.completeUpload(userId, uploadId).withPlayableUrls()
        libraryState.update { library ->
            library.copy(
                uploads = library.uploads.map { upload ->
                    if (upload.id == uploadId) completed else upload
                }
            )
        }
    }

    override suspend fun submitFeedback(feedback: Feedback): FeedbackResponse {
        val recordId = playbackRecordIdsByAudioId[feedback.audioId]
        return if (recordId != null) {
            api.submitPlaybackFeedback(
                userId = userId,
                recordId = recordId,
                request = PlaybackFeedbackRequest(
                    rating = feedback.rating,
                    reason = feedback.reason
                )
            )
        } else {
            api.submitFeedback(feedback)
        }
    }

    override suspend fun submitTextIntent(request: TextIntentRequest): TextIntentResponse {
        val response = api.submitTextIntent(request.copy(userId = userId))
        val responseAudio = (response.audio ?: response.asset)?.withPlayableUrl()
        val fallbackAudio = responseAudio?.let { audio ->
            if (audio.streamUrl.isBlank()) {
                audio.copy(streamUrl = response.audioUrl.orEmpty().absoluteBackendUrl())
            } else {
                audio
            }
        }
        return response.copy(
            audio = fallbackAudio,
            asset = fallbackAudio,
            audioUrl = response.audioUrl?.absoluteBackendUrl(),
            replyAudioUrl = response.replyAudioUrl?.absoluteBackendUrl(),
            notifyAudioUrl = response.notifyAudioUrl?.absoluteBackendUrl()
        )
    }

    override suspend fun transcribeSpeech(uri: Uri, fileName: String, mimeType: String?): String {
        val body = appContext.uriRequestBody(uri, mimeType)
        val part = MultipartBody.Part.createFormData("file", fileName, body)
        val formMediaType = "text/plain".toMediaTypeOrNull()
        val response = api.transcribeSpeech(
            file = part,
            locale = Locale.SIMPLIFIED_CHINESE.toLanguageTag().toRequestBody(formMediaType),
            source = "android_home".toRequestBody(formMediaType)
        )
        return response.text.trim()
    }

    private fun AudioLibrary.withPlayableUrls(): AudioLibrary {
        val fixedRecommended = recommended.map { it.withPlayableUrl() }
        val fixedUploads = uploads.map { it.withPlayableUrls() }
        val audioById = (fixedRecommended + fixedUploads.mapNotNull { it.generatedAudio })
            .associateBy { it.id }
        return copy(
            recommended = fixedRecommended,
            uploads = fixedUploads,
            history = history.map { historyAudio ->
                historyAudio.withPlayableUrl().mergePlayableFields(audioById[historyAudio.id])
            }
        )
    }

    private fun UploadItem.withPlayableUrls(): UploadItem =
        copy(generatedAudio = generatedAudio?.withPlayableUrl())

    private fun AudioItem.withPlayableUrl(): AudioItem {
        val fixedArtwork = artwork?.let { itemArtwork ->
            itemArtwork.copy(imageUrl = itemArtwork.imageUrl?.absoluteBackendUrl())
        }
        return copy(
            streamUrl = streamUrl.absoluteBackendUrl(),
            coverUrl = coverUrl?.absoluteBackendUrl(),
            artwork = fixedArtwork
        )
    }

    private fun RecommendationResponse.toRecommendationResult(): RecommendationResult {
        val responseAudio = (audio ?: asset)?.withPlayableUrl() ?: audioUrlAudioItem()
        val playableAudio = responseAudio?.let { item ->
            if (item.streamUrl.isBlank()) {
                item.copy(streamUrl = audioUrl.orEmpty().absoluteBackendUrl())
            } else {
                item
            }
        }
        if (playableAudio != null && playableAudio.streamUrl.isNotBlank()) {
            return RecommendationResult.Ready(playableAudio)
        }

        val nextPrompt = generationPrompt
            ?: prompt
            ?: message
            ?: "生成一段适合今晚入睡的 Floppy 睡前音频"
        val normalizedAction = action.orEmpty().lowercase(Locale.US)
        val normalizedType = type.orEmpty().lowercase(Locale.US)
        return when {
            normalizedAction in RecommendationGenerationActions -> RecommendationResult.NeedsGeneration(nextPrompt)
            normalizedType in RecommendationGenerationActions -> RecommendationResult.NeedsGeneration(nextPrompt)
            normalizedAction in RecommendationReadyActions && playableAudio != null -> RecommendationResult.Ready(playableAudio)
            normalizedType in RecommendationReadyActions && playableAudio != null -> RecommendationResult.Ready(playableAudio)
            else -> RecommendationResult.NeedsGeneration(nextPrompt)
        }
    }

    private fun RecommendationResponse.audioUrlAudioItem(): AudioItem? {
        val playableUrl = audioUrl?.absoluteBackendUrl()?.takeIf { it.isNotBlank() } ?: return null
        val title = message?.takeIf { it.isNotBlank() } ?: "今晚推荐音频"
        return AudioItem(
            id = "recommendation-${playableUrl.hashCode()}",
            title = title,
            subtitle = action ?: type ?: "Floppy 推荐",
            durationSeconds = 60,
            streamUrl = playableUrl,
            artwork = AudioArtwork(
                seedColor = 0xFF7D6BFF,
                prompt = title
            ),
            source = AudioSource.Library,
            category = "Recommendation"
        )
    }

    private fun AudioItem.mergePlayableFields(sourceAudio: AudioItem?): AudioItem {
        if (sourceAudio == null || streamUrl.isNotBlank()) return this
        return copy(
            title = title.ifBlank { sourceAudio.title },
            subtitle = subtitle.ifBlank { sourceAudio.subtitle },
            durationSeconds = if (durationSeconds > 0) durationSeconds else sourceAudio.durationSeconds,
            streamUrl = sourceAudio.streamUrl,
            coverUrl = coverUrl ?: sourceAudio.coverUrl,
            artwork = artwork ?: sourceAudio.artwork,
            category = category.ifBlank { sourceAudio.category },
            isGenerated = isGenerated || sourceAudio.isGenerated
        )
    }

    private fun String.absoluteBackendUrl(): String {
        if (startsWith("http://") || startsWith("https://") || startsWith("content://") || startsWith("file://")) {
            return this
        }
        if (startsWith("/")) {
            return baseUrl.trimEnd('/') + this
        }
        return this
    }

    private fun Context.uriRequestBody(uri: Uri, mimeType: String?): RequestBody {
        val resolver = contentResolver
        val contentType = mimeType?.toMediaTypeOrNull()
        return object : RequestBody() {
            override fun contentType() = contentType

            override fun writeTo(sink: BufferedSink) {
                val input = if (uri.scheme == "file") {
                    uri.path?.let { File(it).inputStream() }
                } else {
                    resolver.openInputStream(uri)
                }
                    ?: throw IOException("Cannot open selected file")
                input.use { stream ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val read = stream.read(buffer)
                        if (read == -1) break
                        sink.write(buffer, 0, read)
                    }
                }
            }
        }
    }
}

private fun UserProfile.toQuestionnaireRequest(): UserQuestionnaireRequest {
    return UserQuestionnaireRequest(
        gender = gender.toBackendValue(),
        ageRange = ageRange.toBackendValue(),
        occupation = career.toBackendValue(),
        bedtime = bedtime,
        mainSleepProblem = sleepIssues.firstOrNull()?.toBackendValue(),
        bedtimeHabits = buildBedtimeHabits(),
        favoriteContentTypes = contentPreferences.map { it.toBackendValue() },
        preferredCompanionStyle = companionStyle.toBackendValue(),
        voicePreferences = listOf(voicePreference.toBackendValue())
    )
}

private fun UserProfile.toBackendProfileRequest(): BackendProfileRequest {
    val primaryIssue = sleepIssues.firstOrNull()
    val stressLevel = when {
        SleepIssue.Stress in sleepIssues || SleepIssue.RacingThoughts in sleepIssues -> "high"
        SleepIssue.IrregularSchedule in sleepIssues || SleepIssue.LightSleep in sleepIssues -> "medium"
        else -> "low"
    }
    val anxietyLevel = when {
        SleepIssue.RacingThoughts in sleepIssues -> "high"
        SleepIssue.Stress in sleepIssues || SleepIssue.Loneliness in sleepIssues -> "medium"
        else -> "low"
    }

    return BackendProfileRequest(
        audioTypePreferences = contentPreferences.map { it.toAudioTypeValue() }.distinct(),
        voicePreferences = listOf(voicePreference.toBackendValue()),
        backgroundPreferences = inferBackgroundPreferences(),
        durationPreferenceMin = 15,
        stressLevel = stressLevel,
        anxietyLevel = anxietyLevel,
        avgSleepLatencyMin = if (primaryIssue == SleepIssue.RacingThoughts || primaryIssue == SleepIssue.Stress) 35 else 25,
        moodTags = buildMoodTags()
    )
}

private fun UserProfile.buildBedtimeHabits(): List<String> {
    val habits = mutableListOf<String>()
    if (ContentPreference.PopularKnowledge in contentPreferences) habits += "podcast"
    if (ContentPreference.Meditation in contentPreferences) habits += "meditation"
    if (ContentPreference.Story in contentPreferences) habits += "reading"
    return habits.ifEmpty { listOf("nothing") }
}

private fun UserProfile.inferBackgroundPreferences(): List<String> {
    val backgrounds = mutableListOf<String>()
    if (ContentPreference.WhiteNoise in contentPreferences) backgrounds += "rain_soft"
    if (ContentPreference.Meditation in contentPreferences) backgrounds += "piano_soft"
    if (ContentPreference.Story in contentPreferences) backgrounds += "forest_night"
    return backgrounds.distinct().take(3)
}

private fun UserProfile.buildMoodTags(): List<String> {
    val tags = mutableListOf("calm")
    if (SleepIssue.RacingThoughts in sleepIssues || SleepIssue.Stress in sleepIssues) tags += "anxiety_relief"
    if (SleepIssue.Loneliness in sleepIssues || companionStyle == CompanionStyle.Storyteller) tags += "companionship"
    if (ContentPreference.WhiteNoise in contentPreferences) tags += "environmental_sleep"
    return tags.distinct()
}

private fun Gender.toBackendValue(): String = when (this) {
    Gender.Female -> "female"
    Gender.Male -> "male"
    Gender.NonBinary -> "other"
    Gender.PreferNotToSay -> "prefer_not_to_say"
}

private fun AgeRange.toBackendValue(): String = when (this) {
    AgeRange.Under18 -> "under_18"
    AgeRange.From18To24 -> "18-24"
    AgeRange.From25To34 -> "25-34"
    AgeRange.From35To44 -> "35-44"
    AgeRange.From45Plus -> "45+"
}

private fun CareerChoice.toBackendValue(): String = when (this) {
    CareerChoice.Students -> "student"
    CareerChoice.OfficeWorkers -> "office_worker"
    CareerChoice.FreelanceEntrepreneurs -> "freelancer"
    CareerChoice.FamilyCaregivers -> "caregiver"
    CareerChoice.Others -> "other"
}

private fun SleepIssue.toBackendValue(): String = when (this) {
    SleepIssue.RacingThoughts -> "racing_thoughts"
    SleepIssue.Stress -> "stress"
    SleepIssue.LightSleep -> "light_sleep"
    SleepIssue.IrregularSchedule -> "irregular_schedule"
    SleepIssue.Loneliness -> "loneliness"
}

private fun ContentPreference.toBackendValue(): String = when (this) {
    ContentPreference.Story -> "story"
    ContentPreference.Asmr -> "asmr"
    ContentPreference.WhiteNoise -> "white_noise"
    ContentPreference.Meditation -> "meditation"
    ContentPreference.PsychologicalHealing -> "psychological_healing"
    ContentPreference.PopularKnowledge -> "popular_knowledge"
}

private fun ContentPreference.toAudioTypeValue(): String = when (this) {
    ContentPreference.Story -> "story"
    ContentPreference.Asmr -> "asmr"
    ContentPreference.WhiteNoise -> "white_noise"
    ContentPreference.Meditation -> "meditation"
    ContentPreference.PsychologicalHealing -> "meditation"
    ContentPreference.PopularKnowledge -> "podcast_digest"
}

private fun CompanionStyle.toBackendValue(): String = when (this) {
    CompanionStyle.Gentle -> "warm"
    CompanionStyle.Patient -> "patient"
    CompanionStyle.Reassuring -> "reassuring"
    CompanionStyle.Playful -> "playful"
    CompanionStyle.Quiet -> "quiet"
    CompanionStyle.Coaching -> "guided_relaxation"
    CompanionStyle.Storyteller -> "story_immersion"
}

private fun VoicePreference.toBackendValue(): String = when (this) {
    VoicePreference.WarmFemale -> "warm_female"
    VoicePreference.CalmMale -> "warm_male"
    VoicePreference.Neutral -> "neutral"
    VoicePreference.Whisper -> "whisper"
    VoicePreference.Story -> "storyteller_female"
    VoicePreference.Radio -> "radio"
    VoicePreference.Ocean -> "ocean_low"
    VoicePreference.Bright -> "bright"
}
