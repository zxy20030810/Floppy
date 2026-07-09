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
import android.media.MediaPlayer
import com.floppy.app.data.RealtimeCallSession
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
import java.io.IOException
import java.util.Locale
import java.util.UUID

private const val SpeechLogTag = "FloppySpeech"
private const val ViewModelLogTag = "FloppyViewModel"
private const val GenerationPollTimeoutMillis = 90_000L
private const val GenerationPollIntervalMillis = 500L
private const val GenerationPollMaxConsecutiveFailures = 4
private const val SpeechStopWatchdogMillis = 6_000L

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
    val selectedVoiceId: String? = null,
    // 「和 Floppy 打电话」（豆包端到端实时语音）
    val isInCall: Boolean = false,
    val callStatus: String? = null,
    val callUserText: String? = null,
    val callFloppyText: String? = null
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
    private var activeSpeechSessionId: Long? = null
    private var speechSessionCounter: Long = 0L

    private var generationJob: Job? = null

    // 只在播放错误「变化」时弹一次，避免用户关掉后又被无关的 combine 重新弹出来
    private var lastShownPlaybackError: String? = null

    init {
        viewModelScope.launch {
            combine(
                repository.profile,
                repository.settings,
                repository.library,
                playbackController.playback
            ) { profile, settings, library, playback ->
                val playbackErrorChanged = playback.errorMessage != lastShownPlaybackError
                lastShownPlaybackError = playback.errorMessage
                mutableState.value.copy(
                    profile = profile,
                    settings = settings,
                    library = library,
                    playback = playback,
                    agentState = deriveAgentState(mutableState.value.agentState, playback),
                    activeAudio = playback.currentAudio ?: mutableState.value.activeAudio,
                    errorMessage = if (playbackErrorChanged) {
                        playback.errorMessage.toUserFacingMessage() ?: mutableState.value.errorMessage
                    } else {
                        mutableState.value.errorMessage
                    }
                )
            }.collect { next ->
                mutableState.value = next
            }
        }
        viewModelScope.launch {
            runCatching { repository.refreshLibrary() }
                .onFailure { error ->
                    mutableState.update { it.copy(errorMessage = error.toUserFacingMessage("音频列表加载失败")) }
                }
        }
    }

    fun submitProfile(profile: UserProfile) {
        viewModelScope.launch {
            mutableState.update { it.copy(isSubmittingProfile = true, errorMessage = null) }
            runCatching { repository.saveProfile(profile) }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(errorMessage = error.toUserFacingMessage("画像保存失败，请稍后再试"))
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
                        it.copy(errorMessage = error.toUserFacingMessage("设置更新失败"))
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
                            errorMessage = error.toUserFacingMessage("音色保存失败")
                        )
                    }
                }
        }
    }

    fun startSpeechListening(): Long {
        val sessionId = nextSpeechSessionId()
        // 用户开口对话 = 打断：正在播的音频立刻暂停（否则旧音频继续响，
        // 还会被麦克风录进去当成用户说的话）
        pausePlaybackForDialog()
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
        return sessionId
    }

    fun stopSpeechListening(sessionId: Long) {
        if (!isActiveSpeechSession(sessionId)) return
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

    fun updateSpeechPartial(sessionId: Long, transcript: String) {
        if (!isActiveSpeechSession(sessionId)) return
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

    fun completeSpeechListening(sessionId: Long, transcript: String?) {
        if (!isActiveSpeechSession(sessionId)) return
        val cleanTranscript = transcript?.trim().takeUnless { it.isNullOrEmpty() }
            ?: mutableState.value.voicePartialTranscript
        clearSpeechSession(sessionId)

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

    fun transcribeSpeechRecording(sessionId: Long, uri: Uri, fileName: String, mimeType: String?) {
        if (!isActiveSpeechSession(sessionId)) return
        viewModelScope.launch {
            if (!isActiveSpeechSession(sessionId)) return@launch
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
                        completeSpeechListening(sessionId, null)
                    } else {
                        Log.d(SpeechLogTag, "ASR transcript: $cleanTranscript")
                        completeSpeechListening(sessionId, cleanTranscript)
                    }
                }
                .onFailure { error ->
                    if (!isActiveSpeechSession(sessionId)) {
                        Log.d(SpeechLogTag, "Ignoring stale ASR transcription failure", error)
                        return@onFailure
                    }
                    Log.d(SpeechLogTag, "ASR transcription failed", error)
                    clearSpeechSession(sessionId)
                    mutableState.update {
                        it.copy(
                            isSubmittingVoiceIntent = false,
                            generationMessage = null,
                            errorMessage = error.toUserFacingMessage("语音转文字失败，请再试一次或使用文字输入")
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
        val sessionId = startSpeechListening()
        return runCatching {
            streamingSpeechSession = client.start(
                onPartial = { transcript ->
                    viewModelScope.launch {
                        updateSpeechPartial(sessionId, transcript)
                    }
                },
                onFinal = { transcript ->
                    viewModelScope.launch {
                        if (!isActiveSpeechSession(sessionId)) {
                            Log.d(SpeechLogTag, "Ignoring stale streaming final")
                            return@launch
                        }
                        streamingSpeechSession = null
                        completeSpeechListening(sessionId, transcript)
                    }
                },
                onError = { message ->
                    viewModelScope.launch {
                        if (!isActiveSpeechSession(sessionId)) {
                            Log.d(SpeechLogTag, "Ignoring stale streaming error: $message")
                            return@launch
                        }
                        streamingSpeechSession?.cancel()
                        streamingSpeechSession = null
                        clearSpeechSession(sessionId)
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
            if (isActiveSpeechSession(sessionId)) {
                clearSpeechSession(sessionId)
                mutableState.update {
                    it.copy(
                        isListening = false,
                        agentState = AgentState.Idle,
                        voicePartialTranscript = null,
                        isSubmittingVoiceIntent = false,
                        generationMessage = null
                    )
                }
            }
            Log.d(SpeechLogTag, "Streaming ASR start failed", error)
        }.isSuccess
    }

    private var speechStopWatchdogJob: Job? = null

    fun stopStreamingSpeech() {
        val session = streamingSpeechSession ?: return
        session.stop()
        val sessionId = activeSpeechSessionId
        mutableState.update {
            it.copy(
                isListening = false,
                agentState = AgentState.Idle,
                isSubmittingVoiceIntent = true,
                generationMessage = "Floppy 正在理解你说的话"
            )
        }
        // 看门狗：服务端一直不回 final 时，用最后的 partial 兜底提交，别让 UI 永远卡在"正在理解"
        speechStopWatchdogJob?.cancel()
        speechStopWatchdogJob = viewModelScope.launch {
            delay(SpeechStopWatchdogMillis)
            if (sessionId == null || !isActiveSpeechSession(sessionId)) return@launch
            Log.w(SpeechLogTag, "Streaming ASR final timed out; falling back to last partial")
            streamingSpeechSession?.cancel()
            streamingSpeechSession = null
            // completeSpeechListening 会 clearSpeechSession → 迟到的真 final 因会话失效被忽略
            completeSpeechListening(sessionId, mutableState.value.voicePartialTranscript)
        }
    }

    // --- 「和 Floppy 打电话」（豆包端到端实时语音，纯陪聊） ---

    private var realtimeCallSession: RealtimeCallSession? = null

    /** 播放是被通话按下的暂停键 → 挂断后自动续播；手动播放/暂停会清掉这个标记 */
    private var pausedByCall = false

    fun startRealtimeCall() {
        val client = repository.realtimeCallClient ?: run {
            mutableState.update { it.copy(errorMessage = "当前模式不支持语音通话") }
            return
        }
        if (uiState.value.isInCall) return
        // 通话独占声音通道：丢弃进行中的流式识别（不等 final，免得转写在通话中途
        // 变成一条文字意图打进来）、停掉语音回复播报和正在播的助眠音频
        cancelSpeechListening()
        releaseReplyPlayer()
        val playbackStateBeforeCall = uiState.value.playback.state
        pausedByCall = playbackStateBeforeCall == PlaybackState.Playing ||
            playbackStateBeforeCall == PlaybackState.Buffering
        playbackController.pause()
        mutableState.update {
            it.copy(isInCall = true, callStatus = "正在接通 Floppy…", callUserText = null, callFloppyText = null, errorMessage = null)
        }
        realtimeCallSession = client.start(
            userId = repository.userId,
            onReady = {
                mutableState.update { it.copy(callStatus = "接通了，说点什么吧") }
            },
            onUserTranscript = { text, interim ->
                mutableState.update {
                    if (!interim) it.copy(callUserText = text, callFloppyText = null, callStatus = null)
                    else it.copy(callUserText = text, callStatus = null)
                }
            },
            onFloppyText = { chunk ->
                mutableState.update { it.copy(callFloppyText = ((it.callFloppyText ?: "") + chunk).takeLast(160)) }
            },
            onError = { message ->
                Log.w(ViewModelLogTag, "Realtime call error: $message")
                mutableState.update { it.copy(errorMessage = "通话连接失败，请稍后再试") }
            },
            onEnded = {
                realtimeCallSession = null
                mutableState.update { it.copy(isInCall = false, callStatus = null) }
                // 回调可能来自 WS 线程，ExoPlayer 只能在主线程碰 → 切回主线程再续播
                viewModelScope.launch {
                    if (pausedByCall) {
                        pausedByCall = false
                        if (uiState.value.playback.state == PlaybackState.Paused) {
                            playbackController.resume()
                            mutableState.update { it.copy(agentState = AgentState.Playing) }
                        }
                    }
                }
            }
        )
    }

    fun stopRealtimeCall() {
        realtimeCallSession?.hangUp()
    }

    fun cancelSpeechListening() {
        val currentState = mutableState.value
        val shouldCancel = currentState.isListening ||
            currentState.agentState == AgentState.Listening ||
            streamingSpeechSession != null
        if (!shouldCancel) return

        activeSpeechSessionId?.let(::clearSpeechSession)
        streamingSpeechSession?.cancel()
        streamingSpeechSession = null
        mutableState.update {
            it.copy(
                isListening = false,
                agentState = if (it.agentState == AgentState.Listening) AgentState.Idle else it.agentState,
                voicePartialTranscript = null,
                isSubmittingVoiceIntent = false,
                generationMessage = if (it.isListening || it.agentState == AgentState.Listening) {
                    null
                } else {
                    it.generationMessage
                }
            )
        }
    }

    fun failSpeechListening(sessionId: Long, message: String) {
        if (!isActiveSpeechSession(sessionId)) return
        failSpeechListening(message, sessionId)
    }

    private fun failSpeechListening(message: String, sessionId: Long? = activeSpeechSessionId) {
        sessionId?.let(::clearSpeechSession)
        mutableState.update {
            it.copy(
                isListening = false,
                agentState = AgentState.Idle,
                voicePartialTranscript = null,
                isSubmittingVoiceIntent = false,
                generationMessage = it.voiceTranscript?.let { transcript -> "听到：$transcript" },
                errorMessage = message.toUserFacingMessage()
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
                            errorMessage = error.toUserFacingMessage("推荐失败，请稍后再试")
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

    /** 对话开始（开麦/新意图）时暂停正在播的音频 — 打断语义 + 防回声。
     *  同时打断 Floppy 正在念的语音回复（用户开口 = 一切让路）。 */
    private fun pausePlaybackForDialog() {
        releaseReplyPlayer()
        val state = uiState.value.playback.state
        if (state == PlaybackState.Playing || state == PlaybackState.Buffering) {
            playbackController.pause()
            mutableState.update { it.copy(agentState = AgentState.Paused) }
        }
    }

    // --- Floppy 语音回复（replyAudioUrl）播放，播完接主音频 ---

    private var replyPlayer: MediaPlayer? = null

    private fun playReplyThenAudio(replyAudioUrl: String?, audio: AudioItem?) {
        if (uiState.value.isInCall) {
            // 通话中不抢声道：不念回复也不放音频。
            // activeAudio 已由调用方（submitTextIntent onSuccess）记录，挂断后可手动播放
            return
        }
        releaseReplyPlayer()
        if (replyAudioUrl.isNullOrBlank()) {
            audio?.let { play(it) }
            return
        }
        pausePlaybackForDialog()  // Floppy 说话时不和背景音频抢声道
        val player = MediaPlayer()
        replyPlayer = player
        runCatching {
            player.setDataSource(replyAudioUrl)
            player.setOnPreparedListener { it.start() }
            player.setOnCompletionListener {
                releaseReplyPlayer()
                audio?.let { next -> play(next) }
            }
            player.setOnErrorListener { _, _, _ ->
                releaseReplyPlayer()
                audio?.let { next -> play(next) }
                true
            }
            player.prepareAsync()
        }.onFailure {
            releaseReplyPlayer()
            audio?.let { next -> play(next) }
        }
    }

    private fun releaseReplyPlayer() {
        replyPlayer?.let { runCatching { it.release() } }
        replyPlayer = null
    }

    fun play(audio: AudioItem) {
        if (audio.streamUrl.isBlank()) {
            mutableState.update { it.copy(errorMessage = "这个音频暂时没有可播放地址") }
            return
        }
        releaseReplyPlayer()  // 手动点播时停掉正在念的语音回复，避免两路声音叠着
        pausedByCall = false  // 手动操作接管播放，通话挂断后不再自动续播
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
        pausedByCall = false  // 手动播放/暂停后，通话结束不再自动续播
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
                        releaseReplyPlayer()  // 续播前掐掉 Floppy 正在念的语音回复，避免两路声音叠着
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
                    it.copy(errorMessage = error.toUserFacingMessage("反馈提交失败，请重试"))
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

            // 支持"remix 当前这条音频"类意图：把正在播/刚暂停的音频 id 带给后端
            val playbackSnapshot = mutableState.value.playback
            val currentAssetId = if (
                playbackSnapshot.state == PlaybackState.Playing ||
                playbackSnapshot.state == PlaybackState.Paused
            ) {
                (playbackSnapshot.currentAudio ?: mutableState.value.activeAudio)?.id
            } else {
                null
            }

            val request = TextIntentRequest(
                text = cleanText,
                source = source,
                profile = mutableState.value.profile,
                locale = Locale.getDefault().toLanguageTag(),
                conversationId = conversationId,
                clientRequestId = requestId,
                turnIndex = turnIndex,
                supersedesRequestId = previousRequestId,
                currentAssetId = currentAssetId
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
                    // Floppy 先用语音念回复（若有），说完自动接播新音频；
                    // 拿到新音频却继续放旧音频是不合逻辑的 —— 新意图接管播放。
                    playReplyThenAudio(response.replyAudioUrl, audio)
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
                            errorMessage = error.toUserFacingMessage("消息发送失败，请稍后再试")
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

    private fun nextSpeechSessionId(): Long {
        val sessionId = ++speechSessionCounter
        activeSpeechSessionId = sessionId
        return sessionId
    }

    private fun isActiveSpeechSession(sessionId: Long): Boolean {
        return activeSpeechSessionId == sessionId
    }

    private fun clearSpeechSession(sessionId: Long) {
        if (activeSpeechSessionId == sessionId) {
            activeSpeechSessionId = null
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
                    mutableState.update { it.copy(errorMessage = error.toUserFacingMessage("上传失败，请重试")) }
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
                    mutableState.update { it.copy(errorMessage = error.toUserFacingMessage("重试上传失败")) }
                }
        }
    }

    fun completeUpload(uploadId: String) {
        viewModelScope.launch {
            runCatching { repository.completeUpload(uploadId) }
                .onFailure { error ->
                    mutableState.update { it.copy(errorMessage = error.toUserFacingMessage("上传完成状态同步失败")) }
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
                    var elapsedMillis = 0L
                    var consecutivePollFailures = 0
                    while (elapsedMillis < GenerationPollTimeoutMillis) {
                        // 单次轮询失败不炸进程：容忍几次网络抖动，连续失败才放弃
                        val nextTask = runCatching { repository.pollGenerationTask(task.id) }
                            .onFailure { Log.w(ViewModelLogTag, "Generation poll failed", it) }
                            .getOrNull()
                        if (nextTask == null) {
                            consecutivePollFailures += 1
                            if (consecutivePollFailures >= GenerationPollMaxConsecutiveFailures) {
                                mutableState.update {
                                    it.copy(
                                        agentState = AgentState.Failed,
                                        generationMessage = null,
                                        errorMessage = "生成进度查询失败，请稍后再试"
                                    )
                                }
                                return@launch
                            }
                            delay(GenerationPollIntervalMillis)
                            elapsedMillis += GenerationPollIntervalMillis
                            continue
                        }
                        consecutivePollFailures = 0
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
                                        errorMessage = nextTask.message.toUserFacingMessage()
                                            ?: "生成失败，请稍后再试"
                                    )
                                }
                                return@launch
                            }

                            GenerationStatus.Pending,
                            GenerationStatus.Generating -> {
                                delay(GenerationPollIntervalMillis)
                                elapsedMillis += GenerationPollIntervalMillis
                            }
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
                            errorMessage = error.toUserFacingMessage("生成失败，请稍后再试")
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
        realtimeCallSession?.hangUp()
        releaseReplyPlayer()
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

/** 用户永远只看到友好的中文文案；原始异常进日志排查用 */
private fun Throwable.toUserFacingMessage(fallback: String): String {
    Log.w(ViewModelLogTag, "Operation failed: $message", this)
    return fallback
}

private fun String?.toUserFacingMessage(): String? {
    val cleanMessage = this?.trim().orEmpty()
    return cleanMessage
        .takeIf { it.isNotEmpty() }
        ?.takeUnless { it.isHiddenTransientMessage() }
        ?.takeUnless { it.isTechnicalFailureMessage() }
}

private fun String.isHiddenTransientMessage(): Boolean {
    return equals("connection closed", ignoreCase = true)
}

private fun String.isTechnicalFailureMessage(): Boolean {
    val lowerMessage = lowercase(Locale.US)
    return listOf(
        "failed to connect",
        "unable to resolve host",
        "timeout",
        "timed out",
        "connection reset",
        "connection refused",
        "network is unreachable",
        "no route to host",
        "socket",
        "ssl",
        "certificate",
        "java.net.",
        "okhttp",
        "retrofit"
    ).any { token -> lowerMessage.contains(token) } ||
        Regex("""\bhttp\s+\d{3}\b""").containsMatchIn(lowerMessage)
}
