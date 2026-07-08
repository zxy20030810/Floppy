# Floppy Voice Reference Package

This package contains local voice reference previews for Floppy's AI companion voice choices.

Frontend will send a stable `voice_id` with chat / voice intent requests. Backend should map that `voice_id` to the corresponding TTS voice, voice model, or style prompt, then return generated audio via `audio_url` or `audio.streamUrl`.

These audio files are local preview references. They are useful for matching direction and product intent, but they are not production voice-clone samples.

## Voice Mapping

| voice_id | Display name | Reference file | Voice direction |
| --- | --- | --- | --- |
| `warm_female` | 温暖治愈音 | `voice_warm_female.wav` | Young female voice, warm, gentle, soothing, close and relaxing. Suitable for bedtime comfort. |
| `warm_male` | 沉稳低音 | `voice_calm_male.wav` | Mature male voice, low, stable, slow, calm, safe-feeling. |
| `neutral` | 清澈中性音 | `voice_neutral.wav` | Neutral voice, clear, natural, clean, emotionally restrained. |
| `whisper` | 轻柔低语音 | `voice_whisper.wav` | Soft whisper-like voice, slow and close, but still clear and intelligible. |
| `storyteller_female` | 睡前故事音 | `voice_story.wav` | Gentle story voice, rhythmic and expressive, suitable for bedtime stories. |
| `radio` | 柔和电台音 | `voice_radio.wav` | Late-night radio style, warm, steady, companionable. |
| `ocean_low` | 低沉海洋音 | `voice_ocean.wav` | Low-frequency, slow, quiet, meditative, suitable for sleep and ambient guidance. |
| `bright` | 明亮陪伴音 | `voice_bright.wav` | Bright, friendly, clear, lightly energetic, but not excited. |

## Suggested Request Field

Add `voice_id` to chat / voice intent requests:

```json
{
  "text": "我今晚想听一点放松的内容",
  "source": "chat",
  "voice_id": "warm_female"
}
```

## Backend Expectation

When backend receives `voice_id`, it should:

1. Select the matching TTS voice / model / style.
2. Generate the response audio using that voice.
3. Return playable audio through `audio_url`, `audio.streamUrl`, or equivalent response fields.

Avoid robotic, overly commercial, overly excited, or news-anchor delivery for all sleep companion voices.
