package com.floppy.app.data

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import com.floppy.app.domain.AudioItem
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.json.JSONObject
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

private const val CallLogTag = "FloppyCall"
private const val MicSampleRate = 16_000       // 上行：豆包要求 16k/mono/s16le
private const val PlaybackSampleRate = 24_000  // 下行：后端配置的 pcm_s16le 24k

/**
 * 「和 Floppy 打电话」客户端 — 连接后端 /voice/realtime（豆包端到端实时语音代理）。
 *
 * 上行：麦克风 PCM 二进制流（20ms/包）
 * 下行：binary = 24k PCM 回复音频（AudioTrack 播放）
 *       JSON  = ready / asr（用户话语转写）/ chat（Floppy 字幕）/ asr_info（打断信号）/ tts_end / error
 */
class RealtimeCallClient(
    private val okHttpClient: OkHttpClient,
    private val baseUrl: String
) {
    private val callUrl = baseUrl.toCallWebSocketUrl("voice/realtime")

    fun start(
        userId: String,
        onReady: () -> Unit,
        onUserTranscript: (String, Boolean) -> Unit,
        onFloppyText: (String) -> Unit,
        onError: (String) -> Unit,
        onEnded: () -> Unit,
        onTtsEnd: () -> Unit = {},
        onGenerationStarted: (String) -> Unit = {},
        onGenerationDone: (AudioItem, String?, String?) -> Unit = { _, _, _ -> }
    ): RealtimeCallSession {
        val session = RealtimeCallSession(
            okHttpClient = okHttpClient,
            callUrl = "$callUrl?user_id=$userId",
            onReady = onReady,
            onUserTranscript = onUserTranscript,
            onFloppyText = onFloppyText,
            onError = onError,
            onEnded = onEnded,
            httpBaseUrl = baseUrl,
            onTtsEnd = onTtsEnd,
            onGenerationStarted = onGenerationStarted,
            onGenerationDone = onGenerationDone
        )
        session.start()
        return session
    }
}

class RealtimeCallSession(
    private val okHttpClient: OkHttpClient,
    private val callUrl: String,
    private val onReady: () -> Unit,
    private val onUserTranscript: (String, Boolean) -> Unit,
    private val onFloppyText: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onEnded: () -> Unit,
    // 以下参数带默认值：老的构造调用无需改动
    private val httpBaseUrl: String = "",
    private val onTtsEnd: () -> Unit = {},
    private val onGenerationStarted: (String) -> Unit = {},
    private val onGenerationDone: (AudioItem, String?, String?) -> Unit = { _, _, _ -> }
) {
    private val isClosed = AtomicBoolean(false)
    private val isSessionReady = AtomicBoolean(false)
    private var webSocket: WebSocket? = null
    private var audioRecord: AudioRecord? = null
    private var audioThread: Thread? = null

    // 下行 PCM 不在 OkHttp 读线程上阻塞写（否则 asr_info/error 排在几秒音频后面），
    // 而是丢进队列由专门的播放线程消费
    private val playbackQueue = LinkedBlockingQueue<ByteString>()
    private val flushGeneration = AtomicLong(0)
    private val trackLock = Any()
    private var audioTrack: AudioTrack? = null
    private var playbackThread: Thread? = null

    fun start() {
        Log.d(CallLogTag, "Starting realtime call: $callUrl")
        setupAudioTrack()
        val request = Request.Builder().url(callUrl).build()
        webSocket = okHttpClient.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.d(CallLogTag, "Call socket opened, waiting for ready")
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    handleServerMessage(webSocket, text)
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    // Floppy 的回复语音（24k PCM）——入队即返回，绝不阻塞 WS 读线程
                    if (!isClosed.get()) {
                        playbackQueue.offer(bytes)
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    if (isClosed.get()) return
                    Log.d(CallLogTag, "Call socket failed", t)
                    onError(t.message ?: "通话连接失败")
                    finish()
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d(CallLogTag, "Call socket closed: $code $reason")
                    finish()
                }
            }
        )
    }

    fun hangUp() {
        if (isClosed.get()) return
        Log.d(CallLogTag, "Hanging up call")
        webSocket?.send("""{"type":"stop"}""")
        finish()
    }

    private fun handleServerMessage(webSocket: WebSocket, text: String) {
        val message = runCatching { JSONObject(text) }.getOrElse { return }
        when (message.optString("type")) {
            "ready" -> {
                isSessionReady.set(true)
                onReady()
                startMicCapture(webSocket)
            }

            "asr" -> onUserTranscript(message.optString("text"), message.optBoolean("interim"))

            "chat" -> message.optString("text").takeIf { it.isNotEmpty() }?.let(onFloppyText)

            // Floppy 说完一轮话 = 回合边界，上层用它决定何时优雅挂断交接
            "tts_end" -> onTtsEnd()

            // 通话里触发了后台生成任务
            "generation_started" -> onGenerationStarted(message.optString("jobId"))

            // 后台生成完成：解析成品音频交给上层；解析失败只记日志不打断通话
            "generation_done" -> runCatching {
                val audio: AudioItem? = Gson().fromJson(
                    message.getJSONObject("audio").toString(),
                    AudioItem::class.java
                )
                requireNotNull(audio) { "generation_done missing audio" }
                val notifyAudioUrl = if (message.isNull("notifyAudioUrl")) {
                    null
                } else {
                    message.optString("notifyAudioUrl").takeIf { it.isNotBlank() }
                }
                val jobId = if (message.isNull("jobId")) {
                    null
                } else {
                    message.optString("jobId").takeIf { it.isNotBlank() }
                }
                onGenerationDone(
                    audio.copy(streamUrl = audio.streamUrl.toAbsoluteHttpUrl()),
                    notifyAudioUrl?.toAbsoluteHttpUrl(),
                    jobId
                )
            }.onFailure { Log.w(CallLogTag, "Ignoring malformed generation_done event", it) }

            // 服务端主动收尾（对话自然结束）：按正常挂断处理，不当错误
            "session_end" -> {
                Log.d(CallLogTag, "Call server ended session")
                finish()
            }

            // 用户开口 → 立刻打断 Floppy 正在播的语音（barge-in）：
            // 换代号让播放线程丢掉写了一半的块，清队列，再冲掉 AudioTrack 里已缓冲的音频
            "asr_info" -> {
                flushGeneration.incrementAndGet()
                playbackQueue.clear()
                synchronized(trackLock) {
                    if (!isClosed.get()) {
                        audioTrack?.let { track ->
                            runCatching {
                                track.pause()
                                track.flush()
                                track.play()
                            }
                        }
                    }
                }
            }

            "error" -> {
                Log.d(CallLogTag, "Call server error: ${message.optString("message")}")
                onError(message.optString("message", "通话服务异常"))
                finish()
            }
        }
    }

    /** WS 事件里的相对路径（/static/...）补全成完整 http 地址，播放器才认得 */
    private fun String.toAbsoluteHttpUrl(): String {
        return if (startsWith("/") && httpBaseUrl.isNotBlank()) {
            httpBaseUrl.trimEnd('/') + this
        } else {
            this
        }
    }

    private fun setupAudioTrack() {
        val minBuffer = AudioTrack.getMinBufferSize(
            PlaybackSampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val track = AudioTrack(
            AudioAttributes.Builder()
                // USAGE_MEDIA：走外放喇叭；VOICE_COMMUNICATION 在很多机型会路由到听筒，外人听不见
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
            AudioFormat.Builder()
                .setSampleRate(PlaybackSampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build(),
            maxOf(minBuffer, PlaybackSampleRate),  // ~0.5s buffer
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        ).also { it.play() }
        audioTrack = track
        startPlaybackThread(track)
    }

    /** 专职播放线程：从队列取 PCM 阻塞写入 AudioTrack；退出时由自己 release，避免 use-after-release */
    private fun startPlaybackThread(track: AudioTrack) {
        playbackThread = Thread {
            try {
                while (!isClosed.get()) {
                    val chunk = try {
                        playbackQueue.take()
                    } catch (_: InterruptedException) {
                        break
                    }
                    if (isClosed.get()) break
                    val generation = flushGeneration.get()
                    val data = chunk.toByteArray()
                    var offset = 0
                    while (offset < data.size) {
                        // barge-in / 挂断会 pause() 掉正在阻塞的 write()，这里检查后丢弃剩余旧音频
                        if (isClosed.get() || flushGeneration.get() != generation) break
                        val written = track.write(data, offset, data.size - offset)
                        if (written <= 0) break
                        offset += written
                    }
                }
            } finally {
                synchronized(trackLock) {
                    runCatching { track.release() }
                    audioTrack = null
                }
            }
        }.apply {
            name = "floppy-call-playback"
            start()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startMicCapture(webSocket: WebSocket) {
        val minBufferSize = AudioRecord.getMinBufferSize(
            MicSampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBufferSize <= 0) {
            onError("设备不支持当前录音格式")
            return
        }
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            MicSampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBufferSize, MicSampleRate / 5)
        )
        audioRecord = recorder
        runCatching { recorder.startRecording() }
            .onFailure {
                recorder.release()
                audioRecord = null
                onError("通话录音启动失败")
                return
            }

        audioThread = Thread {
            val buffer = ByteArray(640)  // 20ms @16k/s16le，按豆包推荐节奏发包
            while (!isClosed.get() && isSessionReady.get()) {
                val read = recorder.read(buffer, 0, buffer.size)
                if (read > 0) {
                    webSocket.send(buffer.toByteString(0, read))
                }
            }
        }.apply {
            name = "floppy-realtime-call"
            start()
        }
    }

    private fun finish() {
        if (!isClosed.compareAndSet(false, true)) return
        isSessionReady.set(false)
        audioRecord?.let { runCatching { it.stop() }; it.release() }
        audioRecord = null
        audioThread = null
        // 唤醒播放线程：pause() 解开阻塞中的 write()，interrupt() 解开 take()；
        // AudioTrack 由播放线程退出时自行 release
        playbackQueue.clear()
        synchronized(trackLock) {
            audioTrack?.let { runCatching { it.pause() } }
        }
        playbackThread?.interrupt()
        playbackThread = null
        webSocket?.close(1000, "hang up")
        webSocket = null
        onEnded()
    }
}

private fun String.toCallWebSocketUrl(path: String): String {
    val trimmedBase = trimEnd('/')
    val wsBase = when {
        trimmedBase.startsWith("https://") -> "wss://" + trimmedBase.removePrefix("https://")
        trimmedBase.startsWith("http://") -> "ws://" + trimmedBase.removePrefix("http://")
        else -> trimmedBase
    }
    return "$wsBase/${path.trimStart('/')}"
}
