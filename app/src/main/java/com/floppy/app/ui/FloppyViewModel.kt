package com.floppy.app.ui

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.floppy.app.data.FloppyRepository
import com.floppy.app.data.TextIntentActions
import com.floppy.app.data.TextIntentRequest
import com.floppy.app.data.TextIntentSources
import com.floppy.app.data.StreamingSpeechSession
import com.floppy.app.domain.AgentState
import com.floppy.app.domain.AppSettings
import com.floppy.app.domain.AudioItem
import com.floppy.app.domain.AudioLibrary
import com.floppy.app.domain.Feedback
import com.floppy.app.domain.GenerationStatus
import com.floppy.app.domain.RecommendationResult
import com.floppy.app.domain.UserProfile
import com.floppy.app.domain.VoiceOption
import com.floppy.app.playback.PlaybackController
import com.floppy.app.playback.PlaybackState
import com.floppy.app.playback.PlaybackUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

private const val SpeechLogTag = "FloppySpeech"

data class ChatMessage(
    val body: String,
    val outgoing: Boolean,
    val audio: AudioItem? = null
)

data class FloppyUiState(
    val profile: UserProfile? = null,
    val settings: AppSettings = AppSettings(UserProfile()),
    val library: AudioLibrary = AudioLibrary(recommended = emptyList(), uploads = emptyList(), history = emptyList()),
    val playback: PlaybackUiState = PlaybackUiState(),
    val agentState: AgentState = AgentState.Idle,
    val activeAudio: AudioItem? = null,
    val generationMessage: String? = null,
    val feedbackMessage: String? = null,
    val errorMessage: String? = null,
    val isSubmittingProfile: Boolean = false,
    val isListening: Boolean = false,
    val voiceTranscript: String? = null,
    val voicePartialTranscript: String? = null,
    val chatMessages: List<ChatMessage> = listOf(
        ChatMessage(
            body = "Hi there!\nWhat's on the agenda before bed?",
            outgoing = false
        )
    ),
    val isSubmittingTextIntent: Boolean = false,
    val isSubmittingVoiceIntent: Boolean = false,
    val voiceOptions: List<VoiceOption> = emptyList(),
    val isLoadingVoiceOptions: Boolean = false,
    val isSavingVoiceSelection: Boolean = false,
    val selectedVoiceId: String? = null
) {
    val hasProfile: Boolean = profile != null
    val currentVoiceTranscript: String? = voicePartialTranscript ?: voiceTranscript
}

class FloppyViewModel(
    private val repository: FloppyRepository,
    private val playbackController: PlaybackController
) : ViewModel() {
    private val mutableState = MutableStateFlow(FloppyUiState())
    val uiState: StateFlow<FloppyUiState> = mutableState.asStateFlow()
    private val conversationId = "android-home-${UUID.randomUUID()}"
    private var activeTextIntentRequestId: String? = null
    private var textIntentTurnIndex: Int = 0
    private var streamingSpeechSession: StreamingSpeechSession? = null

    private var generationJob: Job? = null

    init {
        viewModelScope.launch {
            combine(
                repository.profile,
                repository.settings,
                repository.library,
                playbackController.playback
            ) { profile, settings, library, playback ->
                mutableState.value.copy(
                    profile = profile,
                    settings = settings,
                    library = library,
                    playback = playback,
                    agentState = deriveAgentState(mutableState.value.agentState, playback),
                    activeAudio = playback.currentAudio ?: mutableState.value.activeAudio,
                    errorMessage = playback.errorMessage ?: mutableState.value.errorMessage
                )
            }.collect { next ->
                mutableState.value = next
            }
        }
        viewModelScope.launch {
            runCatching { repository.refreshLibrary() }
                .onFailure { error ->
                    mutableState.update { it.copy(errorMessage = error.message ?: "音频列表加载失败") }
                }
        }
    }

    fun submitProfile(profile: UserProfile) {
        viewModelScope.launch {
            mutableState.update { it.copy(isSubmittingProfile = true, errorMessage = null) }
            runCatching { repository.saveProfile(profile) }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(errorMessage = error.message ?: "画像保存失败，请稍后再试")
                    }
                }
            mutableState.update { it.copy(isSubmittingProfile = false) }
        }
    }

    fun updateSettings(settings: AppSettings) {
        viewModelScope.launch {
            runCatching { repository.updateSettings(settings) }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(errorMessage = error.message ?: "设置更新失败")
                    }
                }
        }
    }

    fun refreshVoiceOptions() {
        if (uiState.value.isLoadingVoiceOptions || uiState.value.voiceOptions.isNotEmpty()) return

        viewModelScope.launch {
            mutableState.update { it.copy(isLoadingVoiceOptions = true, errorMessage = null) }
            runCatching { repository.getVoiceOptions() }
                .onSuccess { options ->
                    mutableState.update {
                        it.copy(
                            voiceOptions = options,
                            isLoadingVoiceOptions = false,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(
                            isLoadingVoiceOptions = false,
                            voiceOptions = emptyList(),
                            errorMessage = null
                        )
                    }
                }
        }
    }

    fun saveVoiceSelection(voiceId: String) {
        viewModelScope.launch {
            mutableState.update {
                it.copy(
                    selectedVoiceId = voiceId,
                    isSavingVoiceSelection = true,
                    errorMessage = null
                )
            }
            runCatching { repository.saveVoiceSelection(voiceId) }
                .onSuccess {
                    mutableState.update {
                        it.copy(
                            isSavingVoiceSelection = false,
                            feedbackMessage = "音色已保存",
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(
                            isSavingVoiceSelection = false,
                            errorMessage = error.message ?: "音色保存失败"
                        )
                    }
                }
        }
    }

    fun startSpeechListening() {
        mutableState.update {
            it.copy(
                isListening = true,
                agentState = AgentState.Listening,
                generationMessage = "Floppy 正在听你今晚想要什么感觉",
                voiceTranscript = null,
                voicePartialTranscript = null,
                isSubmittingVoiceIntent = false,
                errorMessage = null
            )
        }
    }

    fun stopSpeechListening() {
        mutableState.update {
            it.copy(
                isListening = false,
                agentState = AgentState.Idle,
                isSubmittingVoiceIntent = false,
                generationMessage = it.currentVoiceTranscript?.let { transcript -> "听到：$transcript" }
                    ?: "Floppy 正在整理你刚才说的话"
            )
        }
    }

    fun updateSpeechPartial(transcript: String) {
        val cleanTranscript = transcript.trim()
        if (cleanTranscript.isEmpty()) return

        mutableState.update {
            it.copy(
                isListening = true,
                agentState = AgentState.Listening,
                voicePartialTranscript = cleanTranscript,
                generationMessage = "听到：$cleanTranscript",
                errorMessage = null
            )
        }
    }

    fun completeSpeechListening(transcript: String?) {
        val cleanTranscript = transcript?.trim().takeUnless { it.isNullOrEmpty() }
            ?: mutableState.value.voicePartialTranscript

        mutableState.update {
            it.copy(
                isListening = false,
                agentState = AgentState.Idle,
                voiceTranscript = cleanTranscript,
                voicePartialTranscript = null,
                isSubmittingVoiceIntent = cleanTranscript != null,
                generationMessage = if (cleanTranscript != null) {
                    "Floppy 正在理解你说的话"
                } else {
                    "没有听清楚，可以再点一次 Floppy"
                },
                errorMessage = null
            )
        }

        if (cleanTranscript != null) {
            submitTextIntent(cleanTranscript, TextIntentSources.Voice)
        }
    }

    fun transcribeSpeechRecording(uri: Uri, fileName: String, mimeType: String?) {
        viewModelScope.launch {
            mutableState.update {
                it.copy(
                    isListening = false,
                    agentState = AgentState.Idle,
                    isSubmittingVoiceIntent = true,
                    generationMessage = "Floppy 正在把你的声音转成文字",
                    errorMessage = null
                )
            }
            runCatching { repository.transcribeSpeech(uri, fileName, mimeType) }
                .onSuccess { transcript ->
                    val cleanTranscript = transcript.trim()
                    if (cleanTranscript.isEmpty()) {
                        Log.d(SpeechLogTag, "ASR returned empty transcript")
                        completeSpeechListening(null)
                    } else {
                        Log.d(SpeechLogTag, "ASR transcript: $cleanTranscript")
                        completeSpeechListening(cleanTranscript)
                    }
                }
                .onFailure { error ->
                    Log.d(SpeechLogTag, "ASR transcription failed", error)
                    mutableState.update {
                        it.copy(
                            isSubmittingVoiceIntent = false,
                            generationMessage = null,
                            errorMessage = error.message ?: "语音转文字失败，请再试一次或使用文字输入"
                        )
                    }
                }
        }
    }

    fun startStreamingSpeech(
        onUnavailable: () -> Unit
    ): Boolean {
        val client = repository.streamingSpeechClient ?: return false
        stopStreamingSpeech()
        return runCatching {
            startSpeechListening()
            streamingSpeechSession = client.start(
                onPartial = { transcript ->
                    viewModelScope.launch {
                        updateSpeechPartial(transcript)
                    }
                },
                onFinal = { transcript ->
                    viewModelScope.launch {
                        streamingSpeechSession = null
                        completeSpeechListening(transcript)
                    }
                },
                onError = { message ->
                    viewModelScope.launch {
                        streamingSpeechSession?.cancel()
                        streamingSpeechSession = null
                        mutableState.update {
                            it.copy(
                                isListening = false,
                                agentState = AgentState.Idle,
                                voicePartialTranscript = null,
                                isSubmittingVoiceIntent = false,
                                generationMessage = null
                            )
                        }
                        onUnavailable()
                        Log.d(SpeechLogTag, "Streaming ASR unavailable: $message")
                    }
                }
            )
        }.onFailure { error ->
            Log.d(SpeechLogTag, "Streaming ASR start failed", error)
        }.isSuccess
    }

    fun stopStreamingSpeech() {
        streamingSpeechSession?.stop()
        if (streamingSpeechSession != null) {
            mutableState.update {
                it.copy(
                    isListening = false,
                    agentState = AgentState.Idle,
                    isSubmittingVoiceIntent = true,
                    generationMessage = "Floppy 正在理解你说的话"
                )
            }
        }
    }

    fun failSpeechListening(message: String) {
        mutableState.update {
            it.copy(
                isListening = false,
                agentState = AgentState.Idle,
                voicePartialTranscript = null,
                isSubmittingVoiceIntent = false,
                generationMessage = it.voiceTranscript?.let { transcript -> "听到：$transcript" },
                errorMessage = message
            )
        }
    }

    fun denySpeechPermission() {
        failSpeechListening("需要麦克风权限，才能把你说的话转成文字")
    }

    fun requestRecommendation() {
        val profile = uiState.value.profile ?: return
        generationJob?.cancel()
        viewModelScope.launch {
            mutableState.update {
                it.copy(
                    agentState = AgentState.Recommending,
                    generationMessage = "Floppy 正在挑选今晚最合适的音频",
                    errorMessage = null,
                    feedbackMessage = null,
                    isListening = false
                )
            }
            runCatching { repository.recommend(profile) }
                .onSuccess { result ->
                    when (result) {
                        is RecommendationResult.Ready -> {
                            mutableState.update {
                                it.copy(
                                    agentState = AgentState.Ready,
                                    activeAudio = result.audio,
                                    generationMessage = "已找到适合今晚的睡前音频"
                                )
                            }
                        }

                        is RecommendationResult.NeedsGeneration -> {
                            createAndPollGeneration(result.prompt, profile)
                        }
                    }
                }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(
                            agentState = AgentState.Failed,
                            generationMessage = null,
                            errorMessage = error.message ?: "推荐失败，请稍后再试"
                        )
                    }
                }
        }
    }

    fun generateCustomAudio() {
        val profile = uiState.value.profile ?: return
        val prompt = "按照 ${profile.companionStyle.label} 和 ${profile.voicePreference.label} 生成 1 分钟睡前试听"
        createAndPollGeneration(prompt, profile)
    }

    fun play(audio: AudioItem) {
        if (audio.streamUrl.isBlank()) {
            mutableState.update { it.copy(errorMessage = "这个音频暂时没有可播放地址") }
            return
        }
        playbackController.play(audio)
        viewModelScope.launch {
            repository.addToHistory(audio)
        }
        mutableState.update {
            it.copy(
                agentState = AgentState.Playing,
                activeAudio = audio,
                feedbackMessage = null,
                errorMessage = null
            )
        }
    }

    fun pauseOrResume() {
        when (uiState.value.playback.state) {
            PlaybackState.Playing, PlaybackState.Buffering -> {
                playbackController.pause()
                mutableState.update { it.copy(agentState = AgentState.Paused) }
            }

            PlaybackState.Paused, PlaybackState.Ended, PlaybackState.Idle, PlaybackState.Failed -> {
                val playingAudio = uiState.value.playback.currentAudio
                val latestAudio = uiState.value.activeAudio
                val audio = latestAudio ?: playingAudio
                if (audio != null) {
                    val shouldPlayFresh = playingAudio == null ||
                        uiState.value.playback.state == PlaybackState.Failed ||
                        (latestAudio != null && latestAudio.id != playingAudio?.id)
                    if (shouldPlayFresh) {
                        play(audio)
                    } else {
                        playbackController.resume()
                        mutableState.update { it.copy(agentState = AgentState.Playing) }
                    }
                }
            }
        }
    }

    fun submitFeedback(rating: Int, reason: String? = null) {
        val audio = uiState.value.activeAudio ?: return
        viewModelScope.launch {
            runCatching {
                repository.submitFeedback(Feedback(audioId = audio.id, rating = rating, reason = reason))
            }.onSuccess { response ->
                mutableState.update {
                    it.copy(
                        feedbackMessage = response.message,
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                mutableState.update {
                    it.copy(errorMessage = error.message ?: "反馈提交失败，请重试")
                }
            }
        }
    }

    fun submitChatMessage(text: String) {
        submitTextIntent(text, TextIntentSources.Chat)
    }

    private fun submitTextIntent(text: String, source: String) {
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return

        val previousRequestId = activeTextIntentRequestId
        val requestId = UUID.randomUUID().toString()
        val turnIndex = ++textIntentTurnIndex
        activeTextIntentRequestId = requestId

        viewModelScope.launch {
            val userMessage = ChatMessage(body = cleanText, outgoing = true)
            val isVoiceRequest = source == TextIntentSources.Voice
            mutableState.update {
                it.copy(
                    chatMessages = it.chatMessages + userMessage,
                    isSubmittingTextIntent = true,
                    isSubmittingVoiceIntent = isVoiceRequest,
                    errorMessage = null
                )
            }

            val request = TextIntentRequest(
                text = cleanText,
                source = source,
                profile = mutableState.value.profile,
                locale = Locale.getDefault().toLanguageTag(),
                conversationId = conversationId,
                clientRequestId = requestId,
                turnIndex = turnIndex,
                supersedesRequestId = previousRequestId
            )

            runCatching { repository.submitTextIntent(request) }
                .onSuccess { response ->
                    if (!isActiveTextIntent(requestId, turnIndex)) {
                        return@onSuccess
                    }
                    if (response.action == TextIntentActions.Superseded) {
                        clearActiveTextIntent(requestId)
                        mutableState.update {
                            it.copy(
                                isSubmittingTextIntent = false,
                                isSubmittingVoiceIntent = false
                            )
                        }
                        return@onSuccess
                    }

                    val reply = response.reply.trim()
                    val audio = response.audio
                    clearActiveTextIntent(requestId)
                    mutableState.update {
                        it.copy(
                            chatMessages = it.chatMessages.withAssistantReply(reply, audio),
                            isSubmittingTextIntent = false,
                            isSubmittingVoiceIntent = false,
                            activeAudio = audio ?: it.activeAudio,
                            agentState = if (audio != null) AgentState.Ready else it.agentState,
                            generationMessage = when {
                                source == TextIntentSources.Voice && reply.isNotEmpty() -> reply
                                else -> it.generationMessage
                            },
                            feedbackMessage = if (source == TextIntentSources.Voice) "已把语音内容交给 Floppy" else null
                        )
                    }
                }
                .onFailure { error ->
                    if (!isActiveTextIntent(requestId, turnIndex)) {
                        return@onFailure
                    }

                    clearActiveTextIntent(requestId)
                    mutableState.update {
                        it.copy(
                            isSubmittingTextIntent = false,
                            isSubmittingVoiceIntent = false,
                            errorMessage = error.message ?: "消息发送失败，请稍后再试"
                        )
                    }
                }
        }
    }

    private fun isActiveTextIntent(requestId: String, turnIndex: Int): Boolean {
        return activeTextIntentRequestId == requestId && textIntentTurnIndex == turnIndex
    }

    private fun clearActiveTextIntent(requestId: String) {
        if (activeTextIntentRequestId == requestId) {
            activeTextIntentRequestId = null
        }
    }

    private fun List<ChatMessage>.withAssistantReply(reply: String, audio: AudioItem?): List<ChatMessage> {
        if (reply.isEmpty() && audio == null) return this
        return this + ChatMessage(body = reply, outgoing = false, audio = audio)
    }

    fun startUpload(uri: Uri, fileName: String = "File name. word", fileType: String = "word", mimeType: String? = null) {
        if (!isSupportedUpload(fileType, mimeType)) {
            mutableState.update {
                it.copy(errorMessage = "目前只支持上传 mp3、wav、m4a、pdf、txt")
            }
            return
        }
        viewModelScope.launch {
            runCatching { repository.startUpload(uri, fileName, fileType, mimeType) }
                .onFailure { error ->
                    mutableState.update { it.copy(errorMessage = error.message ?: "上传失败，请重试") }
            }
        }
    }

    private fun isSupportedUpload(fileType: String, mimeType: String?): Boolean {
        val normalizedType = fileType.lowercase(Locale.getDefault())
        val normalizedMime = mimeType?.lowercase(Locale.getDefault()).orEmpty()
        return normalizedType in setOf("mp3", "wav", "m4a", "pdf", "txt") ||
            normalizedMime.startsWith("audio/") ||
            normalizedMime == "application/pdf" ||
            normalizedMime.startsWith("text/")
    }

    fun retryUpload(uploadId: String) {
        viewModelScope.launch {
            runCatching { repository.retryUpload(uploadId) }
                .onFailure { error ->
                    mutableState.update { it.copy(errorMessage = error.message ?: "重试上传失败") }
                }
        }
    }

    fun completeUpload(uploadId: String) {
        viewModelScope.launch {
            runCatching { repository.completeUpload(uploadId) }
                .onFailure { error ->
                    mutableState.update { it.copy(errorMessage = error.message ?: "上传完成状态同步失败") }
                }
        }
    }

    fun clearMessages() {
        mutableState.update { it.copy(errorMessage = null, feedbackMessage = null) }
    }

    private fun createAndPollGeneration(prompt: String, profile: UserProfile) {
        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            mutableState.update {
                it.copy(
                    agentState = AgentState.Generating,
                    generationMessage = "Floppy 正在生成 1 分钟试听",
                    errorMessage = null
                )
            }
            runCatching { repository.createGenerationTask(prompt, profile) }
                .onSuccess { task ->
                    mutableState.update { it.copy(generationMessage = task.message) }
                    repeat(8) {
                        val nextTask = repository.pollGenerationTask(task.id)
                        mutableState.update { it.copy(generationMessage = nextTask.message) }
                        when (nextTask.status) {
                            GenerationStatus.Success -> {
                                val audio = nextTask.audio
                                mutableState.update {
                                    it.copy(
                                        agentState = AgentState.Ready,
                                        activeAudio = audio,
                                        generationMessage = "试听片段已准备好"
                                    )
                                }
                                return@launch
                            }

                            GenerationStatus.Failed -> {
                                mutableState.update {
                                    it.copy(
                                        agentState = AgentState.Failed,
                                        errorMessage = nextTask.message
                                    )
                                }
                                return@launch
                            }

                            GenerationStatus.Pending,
                            GenerationStatus.Generating -> delay(350)
                        }
                    }
                    mutableState.update {
                        it.copy(
                            agentState = AgentState.Failed,
                            generationMessage = null,
                            errorMessage = "生成超时，请重新试一次"
                        )
                    }
                }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(
                            agentState = AgentState.Failed,
                            generationMessage = null,
                            errorMessage = error.message ?: "生成失败，请稍后再试"
                        )
                    }
                }
        }
    }

    private fun deriveAgentState(current: AgentState, playback: PlaybackUiState): AgentState {
        return when (playback.state) {
            PlaybackState.Playing, PlaybackState.Buffering -> AgentState.Playing
            PlaybackState.Paused -> if (playback.currentAudio != null) AgentState.Paused else current
            PlaybackState.Failed -> AgentState.Failed
            PlaybackState.Ended -> AgentState.Ready
            PlaybackState.Idle -> current
        }
    }

    override fun onCleared() {
        stopStreamingSpeech()
        playbackController.release()
    }

    class Factory(
        private val repository: FloppyRepository,
        private val playbackController: PlaybackController
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FloppyViewModel(repository, playbackController) as T
        }
    }
}
