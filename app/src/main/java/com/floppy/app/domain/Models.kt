package com.floppy.app.domain

enum class Gender {
    Female,
    Male,
    NonBinary,
    PreferNotToSay
}

enum class AgeRange(val label: String) {
    Under18("18 岁以下"),
    From18To24("18-24"),
    From25To34("25-34"),
    From35To44("35-44"),
    From45Plus("45+")
}

enum class SleepIssue(val label: String) {
    RacingThoughts("脑子停不下来"),
    Stress("压力大"),
    LightSleep("睡得浅"),
    IrregularSchedule("作息不稳定"),
    Loneliness("想被陪伴")
}

enum class ContentPreference(val label: String) {
    Story("Stories, novels"),
    Asmr("ASMR"),
    WhiteNoise("White noise: rain, waves"),
    Meditation("meditation"),
    PsychologicalHealing("psychological healing"),
    PopularKnowledge("Popular knowledge (even math classes)")
}

enum class CompanionStyle(val label: String) {
    Gentle("温柔治愈型"),
    Patient("耐心倾听型"),
    Reassuring("安心哄睡型"),
    Playful("轻松陪伴型"),
    Quiet("安静陪伴型"),
    Coaching("引导放松型"),
    Storyteller("睡前故事型")
}

enum class VoicePreference(val label: String) {
    WarmFemale("温暖治愈音"),
    CalmMale("沉稳低音"),
    Neutral("清澈中性音"),
    Whisper("轻柔低语音"),
    Story("睡前故事音"),
    Radio("柔和电台音"),
    Ocean("低沉海洋音"),
    Bright("明亮陪伴音")
}

data class VoiceOption(
    val id: String,
    val name: String,
    val previewAudioUrl: String,
    val providerVoiceId: String,
    val provider: String? = null
)

enum class CareerChoice(val label: String) {
    Students("Students"),
    OfficeWorkers("office workers"),
    FreelanceEntrepreneurs("freelance entrepreneurs"),
    FamilyCaregivers("family caregivers"),
    Others("others")
}

data class UserProfile(
    val gender: Gender = Gender.PreferNotToSay,
    val ageRange: AgeRange = AgeRange.From25To34,
    val career: CareerChoice = CareerChoice.FreelanceEntrepreneurs,
    val bedtime: String = "23:30",
    val wakeTime: String = "07:30",
    val sleepIssues: Set<SleepIssue> = setOf(SleepIssue.RacingThoughts),
    val contentPreferences: Set<ContentPreference> = setOf(ContentPreference.WhiteNoise, ContentPreference.PsychologicalHealing),
    val companionStyle: CompanionStyle = CompanionStyle.Gentle,
    val voicePreference: VoicePreference = VoicePreference.WarmFemale,
    val companionPrompt: String = ""
)

enum class AgentState {
    Idle,
    Listening,
    Recommending,
    Generating,
    Ready,
    Playing,
    Paused,
    Failed
}

enum class AudioSource {
    Library,
    Upload,
    Generated
}

data class AudioArtwork(
    val imageUrl: String? = null,
    val seedColor: Long,
    val prompt: String,
    val status: ArtworkStatus = ArtworkStatus.Ready
)

enum class ArtworkStatus {
    Pending,
    Generating,
    Ready,
    Failed
}

data class AudioItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val durationSeconds: Int,
    val streamUrl: String,
    val coverUrl: String? = null,
    val artwork: AudioArtwork? = null,
    val source: AudioSource = AudioSource.Library,
    val category: String = "My upload",
    val playbackProgress: Float = 0f,
    val isGenerated: Boolean = false
)

enum class UploadStatus {
    Idle,
    Uploading,
    Failed,
    Completed
}

data class UploadItem(
    val id: String,
    val fileName: String,
    val fileType: String,
    val sizeLabel: String,
    val progress: Float,
    val status: UploadStatus,
    val message: String? = null,
    val generatedAudio: AudioItem? = null
)

sealed interface RecommendationResult {
    data class Ready(val audio: AudioItem) : RecommendationResult
    data class NeedsGeneration(val prompt: String) : RecommendationResult
}

enum class GenerationStatus {
    Pending,
    Generating,
    Success,
    Failed
}

data class GenerationTask(
    val id: String,
    val status: GenerationStatus,
    val message: String,
    val audio: AudioItem? = null
)

data class Feedback(
    val audioId: String,
    val rating: Int,
    val reason: String? = null
)

data class AppSettings(
    val profile: UserProfile,
    val userNickname: String = "小宝",
    val aiPartnerName: String = "Floppy",
    val autoSaveHighRatedAudio: Boolean = true
)

data class AudioLibrary(
    val recommended: List<AudioItem>,
    val uploads: List<UploadItem>,
    val history: List<AudioItem>
)
