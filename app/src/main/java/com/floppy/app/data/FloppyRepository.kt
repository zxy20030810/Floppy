package com.floppy.app.data

import android.net.Uri
import com.floppy.app.domain.AppSettings
import com.floppy.app.domain.AudioItem
import com.floppy.app.domain.AudioLibrary
import com.floppy.app.domain.Feedback
import com.floppy.app.domain.GenerationTask
import com.floppy.app.domain.RecommendationResult
import com.floppy.app.domain.UserProfile
import com.floppy.app.domain.VoiceOption
import kotlinx.coroutines.flow.Flow

interface FloppyRepository {
    val profile: Flow<UserProfile?>
    val library: Flow<AudioLibrary>
    val settings: Flow<AppSettings>
    val streamingSpeechClient: StreamingSpeechClient?
        get() = null

    suspend fun saveProfile(profile: UserProfile)
    suspend fun updateSettings(settings: AppSettings)
    suspend fun recommend(profile: UserProfile): RecommendationResult
    suspend fun createGenerationTask(prompt: String, profile: UserProfile): GenerationTask
    suspend fun pollGenerationTask(taskId: String): GenerationTask
    suspend fun addToHistory(audio: AudioItem)
    suspend fun refreshLibrary()
    suspend fun startUpload(uri: Uri, fileName: String, fileType: String, mimeType: String?)
    suspend fun retryUpload(uploadId: String)
    suspend fun completeUpload(uploadId: String)
    suspend fun submitFeedback(feedback: Feedback): FeedbackResponse
    suspend fun submitTextIntent(request: TextIntentRequest): TextIntentResponse
    suspend fun transcribeSpeech(uri: Uri, fileName: String, mimeType: String?): String
    suspend fun getVoiceOptions(): List<VoiceOption>
    suspend fun saveVoiceSelection(voiceId: String)
}
