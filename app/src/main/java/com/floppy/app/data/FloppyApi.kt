package com.floppy.app.data

import com.floppy.app.domain.AppSettings
import com.floppy.app.domain.AudioItem
import com.floppy.app.domain.AudioLibrary
import com.floppy.app.domain.Feedback
import com.floppy.app.domain.GenerationTask
import com.floppy.app.domain.UploadItem
import com.floppy.app.domain.UserProfile
import com.floppy.app.domain.VoiceOption
import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface FloppyApi {
    @PUT("users/{userId}/questionnaire")
    suspend fun saveQuestionnaire(
        @Path("userId") userId: String,
        @Body questionnaire: UserQuestionnaireRequest
    ): UserQuestionnaireResponse

    @PUT("users/{userId}/profile")
    suspend fun saveProfile(
        @Path("userId") userId: String,
        @Body profile: BackendProfileRequest
    ): BackendProfileResponse

    @POST("users/{userId}/events")
    suspend fun recordEvent(
        @Path("userId") userId: String,
        @Body event: BackendEventRequest
    ): BackendEventResponse

    @POST("v1/recommendations")
    suspend fun recommend(@Body profile: UserProfile): RecommendationResponse

    @POST("v1/generation-tasks")
    suspend fun createGenerationTask(@Body request: GenerationRequest): GenerationTask

    @GET("v1/generation-tasks/{taskId}")
    suspend fun getGenerationTask(@Path("taskId") taskId: String): GenerationTask

    @GET("v1/audio/{audioId}")
    suspend fun getAudio(@Path("audioId") audioId: String): AudioItem

    @GET("users/{userId}/audio-library")
    suspend fun getAudioLibrary(
        @Path("userId") userId: String,
        @Query("limit") limit: Int = 10
    ): AudioLibrary

    @Multipart
    @POST("users/{userId}/uploads")
    suspend fun uploadFile(
        @Path("userId") userId: String,
        @Part file: MultipartBody.Part
    ): UploadItem

    @GET("users/{userId}/uploads")
    suspend fun listUploads(@Path("userId") userId: String): List<UploadItem>

    @POST("users/{userId}/uploads/{uploadId}/retry")
    suspend fun retryUpload(
        @Path("userId") userId: String,
        @Path("uploadId") uploadId: String
    ): UploadItem

    @POST("users/{userId}/uploads/{uploadId}/complete")
    suspend fun completeUpload(
        @Path("userId") userId: String,
        @Path("uploadId") uploadId: String
    ): UploadItem

    @POST("users/{userId}/playback")
    suspend fun startPlayback(
        @Path("userId") userId: String,
        @Body request: PlaybackStartRequest
    ): PlaybackRecordResponse

    @POST("users/{userId}/playback/{recordId}/feedback")
    suspend fun submitPlaybackFeedback(
        @Path("userId") userId: String,
        @Path("recordId") recordId: String,
        @Body request: PlaybackFeedbackRequest
    ): FeedbackResponse

    @POST("v1/feedback")
    suspend fun submitFeedback(@Body feedback: Feedback): FeedbackResponse

    @POST("voice/intent")
    suspend fun submitTextIntent(@Body request: TextIntentRequest): TextIntentResponse

    @Multipart
    @POST("v1/speech/transcriptions")
    suspend fun transcribeSpeech(
        @Part file: MultipartBody.Part,
        @Part("locale") locale: RequestBody,
        @Part("source") source: RequestBody
    ): SpeechTranscriptionResponse

    @POST("demo/chat")
    suspend fun submitDemoChat(@Body request: DemoChatRequest): DemoChatResponse

    @GET("v1/settings")
    suspend fun getSettings(): AppSettings

    @POST("v1/settings")
    suspend fun updateSettings(@Body settings: AppSettings): AppSettings

    @GET("api/ai-companion/voices")
    suspend fun getVoiceOptions(): VoiceOptionsResponse

    @POST("api/ai-companion/voice")
    suspend fun saveVoiceSelection(@Body request: VoiceSelectionRequest)
}

data class GenerationRequest(
    val prompt: String,
    val profile: UserProfile
)

data class RecommendationResponse(
    val action: String? = null,
    val type: String? = null,
    val audio: AudioItem? = null,
    @SerializedName("asset")
    val asset: AudioItem? = null,
    @SerializedName("audio_url")
    val audioUrl: String? = null,
    val prompt: String? = null,
    @SerializedName("generation_prompt")
    val generationPrompt: String? = null,
    val message: String? = null
)

data class UserQuestionnaireRequest(
    val gender: String?,
    @SerializedName("age_range")
    val ageRange: String?,
    val occupation: String?,
    val bedtime: String?,
    @SerializedName("main_sleep_problem")
    val mainSleepProblem: String?,
    @SerializedName("bedtime_habits")
    val bedtimeHabits: List<String>,
    @SerializedName("favorite_content_types")
    val favoriteContentTypes: List<String>,
    @SerializedName("preferred_companion_style")
    val preferredCompanionStyle: String?,
    @SerializedName("voice_preferences")
    val voicePreferences: List<String>
)

data class UserQuestionnaireResponse(
    @SerializedName("user_id")
    val userId: String,
    val gender: String?,
    @SerializedName("age_range")
    val ageRange: String?,
    val occupation: String?,
    val bedtime: String?,
    @SerializedName("main_sleep_problem")
    val mainSleepProblem: String?,
    @SerializedName("bedtime_habits")
    val bedtimeHabits: List<String>,
    @SerializedName("favorite_content_types")
    val favoriteContentTypes: List<String>,
    @SerializedName("preferred_companion_style")
    val preferredCompanionStyle: String?,
    @SerializedName("voice_preferences")
    val voicePreferences: List<String>,
    @SerializedName("completed_at")
    val completedAt: String?,
    @SerializedName("updated_at")
    val updatedAt: String
)

data class BackendProfileRequest(
    @SerializedName("audio_type_preferences")
    val audioTypePreferences: List<String>,
    @SerializedName("voice_preferences")
    val voicePreferences: List<String>,
    @SerializedName("background_preferences")
    val backgroundPreferences: List<String>,
    @SerializedName("duration_preference_min")
    val durationPreferenceMin: Int,
    @SerializedName("stress_level")
    val stressLevel: String,
    @SerializedName("anxiety_level")
    val anxietyLevel: String,
    @SerializedName("avg_sleep_latency_min")
    val avgSleepLatencyMin: Int,
    @SerializedName("mood_tags")
    val moodTags: List<String>
)

data class BackendProfileResponse(
    @SerializedName("user_id")
    val userId: String,
    val segment: String,
    @SerializedName("algo_segment")
    val algoSegment: String?,
    @SerializedName("audio_type_preferences")
    val audioTypePreferences: List<String>,
    @SerializedName("voice_preferences")
    val voicePreferences: List<String>,
    @SerializedName("background_preferences")
    val backgroundPreferences: List<String>,
    @SerializedName("duration_preference_min")
    val durationPreferenceMin: Int,
    @SerializedName("stress_level")
    val stressLevel: String,
    @SerializedName("anxiety_level")
    val anxietyLevel: String,
    @SerializedName("avg_sleep_latency_min")
    val avgSleepLatencyMin: Int,
    @SerializedName("mood_tags")
    val moodTags: List<String>,
    @SerializedName("tonight_mood")
    val tonightMood: String?,
    @SerializedName("tonight_stress")
    val tonightStress: String?,
    @SerializedName("profile_version")
    val profileVersion: Int,
    @SerializedName("updated_at")
    val updatedAt: String
)

data class BackendEventRequest(
    @SerializedName("event_type")
    val eventType: String,
    @SerializedName("asset_id")
    val assetId: String? = null,
    val payload: Map<String, Any> = emptyMap()
)

data class BackendEventResponse(
    @SerializedName("event_id")
    val eventId: String
)

data class FeedbackResponse(
    val accepted: Boolean,
    val message: String
)

data class PlaybackStartRequest(
    val audioId: String,
    @SerializedName("asset_id")
    val assetId: String = audioId,
    val source: String,
    val positionSeconds: Int = 0,
    val durationSeconds: Int = 0,
    val playbackProgress: Float = 0f,
    val event: String = "start"
)

data class PlaybackRecordResponse(
    @SerializedName(value = "record_id", alternate = ["recordId"])
    val recordId: String? = null,
    val accepted: Boolean? = null
)

data class PlaybackFeedbackRequest(
    val rating: Int,
    val reason: String? = null,
    val positionSeconds: Int = 0,
    val durationSeconds: Int = 0,
    val playbackProgress: Float = 0f
)

data class TextIntentRequest(
    val text: String,
    val source: String,
    @Transient
    val profile: UserProfile?,
    @Transient
    val locale: String,
    val conversationId: String? = null,
    val clientRequestId: String? = null,
    val turnIndex: Int? = null,
    val supersedesRequestId: String? = null,
    @SerializedName("user_id")
    val userId: String? = null
)

data class TextIntentResponse(
    val action: String? = null,
    val reply: String = "",
    val audio: AudioItem? = null,
    @SerializedName("asset")
    val asset: AudioItem? = null,
    @SerializedName("audio_url")
    val audioUrl: String? = null,
    val conversationId: String? = null,
    val clientRequestId: String? = null,
    val turnIndex: Int? = null,
    @SerializedName("job_id")
    val jobId: String? = null,
    val hit: Boolean? = null,
    @SerializedName("best_score")
    val bestScore: Double? = null,
    val reasons: List<String>? = null
)

data class SpeechTranscriptionResponse(
    val text: String = "",
    val language: String? = null,
    @SerializedName("duration_ms")
    val durationMs: Long? = null
)

data class VoiceOptionsResponse(
    val voices: List<VoiceOption> = emptyList()
)

data class VoiceSelectionRequest(
    @SerializedName("user_id")
    val userId: String,
    val voiceId: String
)

object TextIntentSources {
    const val Chat = "chat"
    const val Voice = "voice"
}

object TextIntentActions {
    const val Superseded = "superseded"
    const val PlayAsset = "play_asset"
    const val GenerateJob = "generate_job"
    const val NoMatch = "no_match"
}

data class DemoChatRequest(
    @SerializedName("request_text")
    val requestText: String
)

data class DemoChatResponse(
    val action: String? = null,
    @SerializedName("reply_text")
    val replyText: String? = null,
    @SerializedName("audio_url")
    val audioUrl: String? = null,
    val asset: DemoAudioAsset? = null,
    @SerializedName("job_id")
    val jobId: String? = null,
    @SerializedName("job_status")
    val jobStatus: String? = null,
    @SerializedName("best_score")
    val bestScore: Double? = null,
    val hit: Boolean? = null,
    val threshold: Double? = null,
    val reasons: List<String>? = null,
    @SerializedName("planner_meta")
    val plannerMeta: DemoPlannerMeta? = null
)

data class DemoAudioAsset(
    val id: String? = null,
    val type: String? = null,
    val title: String? = null,
    @SerializedName("duration_sec")
    val durationSec: Int? = null,
    @SerializedName("playback_url")
    val playbackUrl: String? = null
)

data class DemoPlannerMeta(
    @SerializedName("planner_source")
    val plannerSource: String? = null,
    @SerializedName("planner_confidence")
    val plannerConfidence: Double? = null,
    @SerializedName("planner_latency_ms")
    val plannerLatencyMs: Long? = null,
    @SerializedName("fallback_reason")
    val fallbackReason: String? = null
)
