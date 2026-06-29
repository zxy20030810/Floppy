package com.floppy.app.data

import com.floppy.app.domain.AudioArtwork
import com.floppy.app.domain.AudioItem
import com.floppy.app.domain.AudioSource
import kotlin.math.roundToInt

class DemoTextIntentClient(
    private val api: FloppyApi,
    private val baseUrl: String
) {
    suspend fun submitTextIntent(request: TextIntentRequest): TextIntentResponse {
        val response = api.submitDemoChat(DemoChatRequest(request.text))
        val audio = response.toAudioItem()
        return TextIntentResponse(
            action = response.action,
            reply = response.toReplyText(audio),
            audio = audio,
            clientRequestId = request.clientRequestId,
            turnIndex = request.turnIndex,
            jobId = response.jobId
        )
    }

    private fun DemoChatResponse.toAudioItem(): AudioItem? {
        val playbackUrl = (asset?.playbackUrl ?: audioUrl)?.toAbsoluteUrl() ?: return null
        val title = asset?.title?.takeIf { it.isNotBlank() } ?: "后端返回的睡前音频"
        val reasonSummary = reasons.orEmpty().take(2).joinToString(" / ")
        return AudioItem(
            id = asset?.id ?: "demo-${playbackUrl.hashCode()}",
            title = title,
            subtitle = reasonSummary.ifBlank { action ?: "play_asset" },
            durationSeconds = asset?.durationSec ?: 60,
            streamUrl = playbackUrl,
            artwork = AudioArtwork(
                seedColor = 0xFF7D6BFF,
                prompt = reasonSummary.ifBlank { title }
            ),
            source = AudioSource.Generated,
            category = asset?.type ?: "Backend",
            isGenerated = hit != true
        )
    }

    private fun DemoChatResponse.toReplyText(audio: AudioItem?): String {
        replyText?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }

        val reasonText = reasons.orEmpty().take(2).joinToString(" / ")
        val scoreText = bestScore?.let { "匹配度 ${(it * 100).roundToInt()}%" }
        return when {
            audio != null -> listOfNotNull(
                "已根据你的描述找到音频《${audio.title}》。",
                reasonText.takeIf { it.isNotBlank() },
                scoreText
            ).joinToString("\n")
            action == "generate_job" && jobId != null -> "后端已创建生成任务：$jobId"
            action == "no_match" -> "后端暂时没有匹配到合适音频，可以换个说法再试。"
            else -> "后端已处理这句话：${action ?: "done"}"
        }
    }

    private fun String.toAbsoluteUrl(): String {
        return if (startsWith("http://") || startsWith("https://")) {
            this
        } else {
            baseUrl.trimEnd('/') + "/" + trimStart('/')
        }
    }
}
