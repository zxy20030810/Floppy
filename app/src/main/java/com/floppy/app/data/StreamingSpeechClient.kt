package com.floppy.app.data

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString.Companion.toByteString
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

private const val SpeechLogTag = "FloppySpeech"
private const val StreamSampleRate = 16_000
private const val StreamChannelConfig = AudioFormat.CHANNEL_IN_MONO
private const val StreamAudioFormat = AudioFormat.ENCODING_PCM_16BIT

class StreamingSpeechClient(
    private val okHttpClient: OkHttpClient,
    baseUrl: String
) {
    private val streamUrl = baseUrl.toWebSocketUrl("v1/speech/stream")

    fun start(
        onPartial: (String) -> Unit,
        onFinal: (String) -> Unit,
        onError: (String) -> Unit
    ): StreamingSpeechSession {
        val session = StreamingSpeechSession(
            okHttpClient = okHttpClient,
            streamUrl = streamUrl,
            onPartial = onPartial,
            onFinal = onFinal,
            onError = onError
        )
        session.start()
        return session
    }
}

class StreamingSpeechSession(
    private val okHttpClient: OkHttpClient,
    private val streamUrl: String,
    private val onPartial: (String) -> Unit,
    private val onFinal: (String) -> Unit,
    private val onError: (String) -> Unit
) {
    private val isClosed = AtomicBoolean(false)
    private val isSocketOpen = AtomicBoolean(false)
    private var webSocket: WebSocket? = null
    private var audioRecord: AudioRecord? = null
    private var audioThread: Thread? = null

    fun start() {
        Log.d(SpeechLogTag, "Starting streaming speech session: $streamUrl")
        val request = Request.Builder().url(streamUrl).build()
        webSocket = okHttpClient.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    if (isClosed.get()) return
                    Log.d(SpeechLogTag, "Streaming speech socket opened")
                    isSocketOpen.set(true)
                    webSocket.send(startMessage())
                    startAudioCapture(webSocket)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    handleServerMessage(text)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    if (isClosed.get()) return
                    Log.d(SpeechLogTag, "Streaming speech socket failed", t)
                    closeAudio()
                    onError(t.message ?: "流式语音连接失败")
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d(SpeechLogTag, "Streaming speech socket closed: $code $reason")
                    closeAudio()
                }
            }
        )
    }

    fun stop() {
        if (isClosed.get()) return
        Log.d(SpeechLogTag, "Stopping streaming speech session")
        closeAudio()
        webSocket?.send("""{"type":"stop"}""")
    }

    fun cancel() {
        if (!isClosed.compareAndSet(false, true)) return
        Log.d(SpeechLogTag, "Canceling streaming speech session")
        closeAudio()
        webSocket?.cancel()
        webSocket = null
    }

    @SuppressLint("MissingPermission")
    private fun startAudioCapture(webSocket: WebSocket) {
        val minBufferSize = AudioRecord.getMinBufferSize(
            StreamSampleRate,
            StreamChannelConfig,
            StreamAudioFormat
        )
        if (minBufferSize <= 0) {
            onError("设备不支持当前录音格式")
            return
        }
        val bufferSize = maxOf(minBufferSize, StreamSampleRate / 5)
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            StreamSampleRate,
            StreamChannelConfig,
            StreamAudioFormat,
            bufferSize
        )
        audioRecord = recorder
        runCatching { recorder.startRecording() }
            .onFailure { error ->
                Log.d(SpeechLogTag, "Streaming audio capture failed", error)
                recorder.release()
                audioRecord = null
                onError("实时录音启动失败")
                return
            }

        audioThread = Thread {
            val buffer = ByteArray(bufferSize)
            while (!isClosed.get() && isSocketOpen.get()) {
                val read = recorder.read(buffer, 0, buffer.size)
                if (read > 0) {
                    webSocket.send(buffer.toByteString(0, read))
                }
            }
        }.apply {
            name = "floppy-streaming-speech"
            start()
        }
    }

    private fun closeAudio() {
        isSocketOpen.set(false)
        val recorder = audioRecord
        audioRecord = null
        if (recorder != null) {
            runCatching { recorder.stop() }
            recorder.release()
        }
        audioThread = null
    }

    private fun handleServerMessage(text: String) {
        val message = runCatching { JSONObject(text) }
            .getOrElse {
                Log.d(SpeechLogTag, "Invalid streaming speech message: $text", it)
                return
            }
        when (message.optString("type")) {
            "partial" -> message.optString("text").trim().takeIf { it.isNotEmpty() }?.let {
                Log.d(SpeechLogTag, "Streaming partial: $it")
                onPartial(it)
            }

            "final" -> {
                val finalText = message.optString("text").trim()
                Log.d(SpeechLogTag, "Streaming final: $finalText")
                onFinal(finalText)
                closeSocket()
            }

            "error" -> {
                val error = message.optString("message", "流式语音识别失败")
                Log.d(SpeechLogTag, "Streaming speech server error: $error")
                onError(error)
                closeSocket()
            }
        }
    }

    private fun closeSocket() {
        if (!isClosed.compareAndSet(false, true)) return
        closeAudio()
        webSocket?.close(1000, "done")
        webSocket = null
    }

    private fun startMessage(): String {
        return JSONObject()
            .put("type", "start")
            .put("locale", Locale.SIMPLIFIED_CHINESE.toLanguageTag())
            .put("sample_rate", StreamSampleRate)
            .put("encoding", "pcm_s16le")
            .put("channels", 1)
            .toString()
    }
}

private fun String.toWebSocketUrl(path: String): String {
    val trimmedBase = trimEnd('/')
    val wsBase = when {
        trimmedBase.startsWith("https://") -> "wss://" + trimmedBase.removePrefix("https://")
        trimmedBase.startsWith("http://") -> "ws://" + trimmedBase.removePrefix("http://")
        else -> trimmedBase
    }
    return "$wsBase/${path.trimStart('/')}"
}
