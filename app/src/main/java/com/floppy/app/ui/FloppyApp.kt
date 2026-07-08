package com.floppy.app.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Animatable2
import android.graphics.drawable.Drawable
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.graphics.drawable.Animatable as DrawableAnimatable
import android.provider.OpenableColumns
import android.provider.Settings
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.text.format.DateFormat
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.ui.AspectRatioFrameLayout
import com.floppy.app.R
import com.floppy.app.bluetooth.FloppyBluetoothController
import com.floppy.app.bluetooth.FloppyBluetoothDevice
import com.floppy.app.bluetooth.FloppyBluetoothState
import com.floppy.app.bluetooth.FloppyBluetoothStatus
import com.floppy.app.data.FallbackAudioLibrary
import com.floppy.app.data.LocalVoiceOptions
import com.floppy.app.domain.AgeRange
import com.floppy.app.domain.AgentState
import com.floppy.app.domain.AppSettings
import com.floppy.app.domain.AudioArtwork
import com.floppy.app.domain.AudioItem
import com.floppy.app.domain.AudioLibrary
import com.floppy.app.domain.AudioSource
import com.floppy.app.domain.CareerChoice
import com.floppy.app.domain.CompanionStyle
import com.floppy.app.domain.ContentPreference
import com.floppy.app.domain.Gender
import com.floppy.app.domain.UploadItem
import com.floppy.app.domain.UploadStatus
import com.floppy.app.domain.UserProfile
import com.floppy.app.domain.VoiceOption
import com.floppy.app.domain.VoicePreference
import com.floppy.app.playback.PlaybackState
import com.floppy.app.playback.PlaybackUiState
import com.floppy.app.ui.video.Mp4VideoPlayer
import com.floppy.app.ui.video.rememberRawMp4Uri
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

private enum class MainTab(val label: String) {
    Home("首页"),
    Library("音频"),
    UploadLink("上传链接"),
    Chat("聊天"),
    Settings("设置")
}

private const val SpeechLogTag = "FloppySpeech"
private const val ExitBackWindowMillis = 2_000L

private enum class ResearchStep {
    Gender,
    Age,
    Career,
    Content,
    Companion
}

@Composable
fun FloppyApp(viewModel: FloppyViewModel) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // 每次打开 app 都先播放开场视频
    var introFinished by rememberSaveable { mutableStateOf(false) }
    // 仅在首次下载打开（还没有画像）时，视频播完后展示调研
    val isFirstLaunch = rememberSaveable { mutableStateOf<Boolean?>(null) }
    if (isFirstLaunch.value == null) {
        isFirstLaunch.value = !state.hasProfile
    }

    LaunchedEffect(state.errorMessage, state.feedbackMessage) {
        val message = state.errorMessage ?: state.feedbackMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessages()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        when {
            !introFinished -> {
                IntroVideoScreen(onFinished = { introFinished = true })
            }

            isFirstLaunch.value == true && !state.hasProfile -> {
                ProfileIntakeScreen(
                    isSubmitting = state.isSubmittingProfile,
                    onSubmit = viewModel::submitProfile
                )
            }

            else -> {
                MainShell(
                state = state,
                snackbarHostState = snackbarHostState,
                    onStartSpeechListening = viewModel::startSpeechListening,
                    onStopSpeechListening = viewModel::stopSpeechListening,
                onStartStreamingSpeech = viewModel::startStreamingSpeech,
                onStopStreamingSpeech = viewModel::stopStreamingSpeech,
                    onSpeechPartial = viewModel::updateSpeechPartial,
                    onSpeechComplete = viewModel::completeSpeechListening,
                    onSpeechRecordingComplete = viewModel::transcribeSpeechRecording,
                    onSpeechError = viewModel::failSpeechListening,
                onSpeechPermissionDenied = viewModel::denySpeechPermission,
                onCancelSpeechListening = viewModel::cancelSpeechListening,
                onPlay = viewModel::play,
                onPauseOrResume = viewModel::pauseOrResume,
                onFeedback = viewModel::submitFeedback,
                onStartUpload = viewModel::startUpload,
                onRetryUpload = viewModel::retryUpload,
                onCompleteUpload = viewModel::completeUpload,
                onSubmitChatMessage = viewModel::submitChatMessage,
                onUpdateSettings = viewModel::updateSettings,
                onRefreshVoiceOptions = viewModel::refreshVoiceOptions,
                onSaveVoiceSelection = viewModel::saveVoiceSelection
            )
            }
        }
    }
}

@Composable
private fun IntroVideoScreen(onFinished: () -> Unit) {
    val videoUri = rememberRawMp4Uri(R.raw.app_intro)
    var finished by remember { mutableStateOf(false) }
    val topVideoEdge = Color(0xFF020914)
    val bottomVideoEdge = Color(0xFF4652B4)

    fun finishOnce() {
        if (!finished) {
            finished = true
            onFinished()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.00f to topVideoEdge,
                        0.18f to topVideoEdge,
                        0.52f to Color(0xFF111C49),
                        0.78f to Color(0xFF303B92),
                        0.92f to bottomVideoEdge,
                        1.00f to bottomVideoEdge
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Mp4VideoPlayer(
            videoUri = videoUri,
            modifier = Modifier.fillMaxSize(),
            autoPlay = true,
            showControls = false,
            loop = false,
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT,
            onPlaybackEnded = { finishOnce() },
            onPlaybackError = { finishOnce() }
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(132.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            topVideoEdge,
                            topVideoEdge.copy(alpha = 0.72f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(168.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            bottomVideoEdge.copy(alpha = 0.58f),
                            bottomVideoEdge
                        )
                    )
                )
        )
    }
}

@Composable
private fun ProfileIntakeScreen(
    isSubmitting: Boolean,
    onSubmit: (UserProfile) -> Unit
) {
    var profile by remember { mutableStateOf(UserProfile()) }
    var step by remember { mutableStateOf(ResearchStep.Gender) }
    val steps = ResearchStep.entries
    val stepIndex = steps.indexOf(step)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ResearchBackground)
            .padding(WindowInsets.statusBars.asPaddingValues())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 36.dp)
                .padding(top = 12.dp, bottom = 26.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                ResearchProgress(
                    currentStep = stepIndex + 1,
                    totalSteps = steps.size
                )
                Spacer(modifier = Modifier.height(28.dp))
                when (step) {
                    ResearchStep.Gender -> GenderResearchPage(
                        selected = profile.gender,
                        onSelected = { profile = profile.copy(gender = it) }
                    )

                    ResearchStep.Age -> AgeResearchPage(
                        selected = profile.ageRange,
                        onSelected = { profile = profile.copy(ageRange = it) }
                    )

                    ResearchStep.Career -> SingleChoiceResearchPage(
                        title = "Is your career closer?",
                        subtitle = "Help us understand the pace of your life",
                        options = CareerChoice.entries,
                        selected = profile.career,
                        optionLabel = { it.label },
                        onSelected = { profile = profile.copy(career = it) }
                    )

                    ResearchStep.Content -> MultiChoiceResearchPage(
                        title = "What do you like to hear? (Multiple choices)",
                        subtitle = "Help us understand the pace of your life",
                        options = ContentPreference.entries,
                        selected = profile.contentPreferences,
                        optionLabel = { it.label },
                        onToggle = { item ->
                            profile = profile.copy(contentPreferences = profile.contentPreferences.toggle(item))
                        }
                    )

                    ResearchStep.Companion -> CompanionPromptResearchPage(
                        value = profile.companionPrompt,
                        onValueChange = { profile = profile.copy(companionPrompt = it) }
                    )
                }
            }

            ResearchBottomButton(
                label = if (step == ResearchStep.Companion) "Completed" else "Next",
                enabled = !isSubmitting && isResearchStepValid(step, profile),
                onClick = {
                    if (step == ResearchStep.Companion) {
                        onSubmit(profile)
                    } else {
                        step = steps[stepIndex + 1]
                    }
                }
            )
        }
    }
}

@Composable
private fun ResearchProgress(currentStep: Int, totalSteps: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(
            text = "$currentStep/$totalSteps",
            color = Color.White,
            fontSize = 12.sp
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(Color(0xFF2A314F))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(currentStep / totalSteps.toFloat())
                    .height(10.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(ResearchPurple)
            )
        }
    }
}

@Composable
private fun GenderResearchPage(
    selected: Gender,
    onSelected: (Gender) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(26.dp)) {
        ResearchTitle(
            title = "Hi~your gender",
            subtitle = "match more accurate content for you"
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            GenderCard(
                label = "BOY",
                selected = selected == Gender.Male,
                crossed = true,
                onClick = { onSelected(Gender.Male) },
                modifier = Modifier.weight(1f)
            )
            GenderCard(
                label = "GIRL",
                selected = selected == Gender.Female,
                crossed = false,
                onClick = { onSelected(Gender.Female) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun GenderCard(
    label: String,
    selected: Boolean,
    crossed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(92.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(Color(0xFF131A33))
            .border(
                width = 1.dp,
                color = if (selected) ResearchPurple else Color.Transparent,
                shape = RoundedCornerShape(15.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 25.sp,
            fontWeight = FontWeight.Medium
        )
        if (crossed) {
            Canvas(modifier = Modifier.size(62.dp, 32.dp)) {
                val path = Path().apply {
                    moveTo(size.width * 0.05f, size.height * 0.72f)
                    cubicTo(
                        size.width * 0.28f,
                        size.height * 0.95f,
                        size.width * 0.82f,
                        size.height * 0.56f,
                        size.width * 0.94f,
                        size.height * 0.12f
                    )
                }
                drawPath(path, color = Color(0xFFFF8C44), style = Stroke(width = 3.5f))
            }
        }
    }
}

@Composable
private fun AgeResearchPage(
    selected: AgeRange,
    onSelected: (AgeRange) -> Unit
) {
    val ageOptions = AgeRange.entries.toList()
    val itemHeight = 44.dp
    val visibleCount = 5
    val edgeCount = visibleCount / 2

    val selectedIndex = ageOptions.indexOf(selected).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val coroutineScope = rememberCoroutineScope()

    // 滚动过程中，落在中间高亮带的词条即为当前选中项。
    val centeredIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
            layoutInfo.visibleItemsInfo.minByOrNull { info ->
                kotlin.math.abs((info.offset + info.size / 2f) - viewportCenter)
            }?.index ?: selectedIndex
        }
    }

    // 将当前居中的词条同步回选中状态。
    LaunchedEffect(centeredIndex) {
        ageOptions.getOrNull(centeredIndex)?.let { option ->
            if (option != selected) onSelected(option)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(26.dp)) {
        ResearchTitle(
            title = "Hi~your gender",
            subtitle = "match more accurate content for you"
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight * visibleCount)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF111832).copy(alpha = 0.94f))
        ) {
            // 中间高亮带，标识当前选中位置。
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .height(itemHeight)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ResearchPurple)
            )
            LazyColumn(
                state = listState,
                flingBehavior = flingBehavior,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = itemHeight * edgeCount)
            ) {
                itemsIndexed(ageOptions) { index, option ->
                    val isSelected = index == centeredIndex
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(itemHeight)
                            .clickable {
                                coroutineScope.launch { listState.animateScrollToItem(index) }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = option.researchLabel(),
                            color = if (isSelected) Color.White else Color(0xFF737A94).copy(alpha = 0.7f),
                            fontSize = if (isSelected) 15.sp else 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> SingleChoiceResearchPage(
    title: String,
    subtitle: String,
    options: List<T>,
    selected: T,
    optionLabel: (T) -> String,
    onSelected: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
        ResearchTitle(title = title, subtitle = subtitle)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            options.forEach { option ->
                ResearchChip(
                    label = optionLabel(option),
                    selected = option == selected,
                    onClick = { onSelected(option) }
                )
            }
        }
    }
}

@Composable
private fun <T> MultiChoiceResearchPage(
    title: String,
    subtitle: String,
    options: List<T>,
    selected: Set<T>,
    optionLabel: (T) -> String,
    onToggle: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
        ResearchTitle(title = title, subtitle = subtitle)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            options.forEach { option ->
                ResearchChip(
                    label = optionLabel(option),
                    selected = selected.contains(option),
                    onClick = { onToggle(option) }
                )
            }
        }
    }
}

@Composable
private fun CompanionPromptResearchPage(
    value: String,
    onValueChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ResearchTitle(
            title = "How do you want me to accompany you tonight?",
            subtitle = "Help us understand the pace of your life"
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                color = Color.White,
                fontSize = 12.sp,
                lineHeight = 18.sp
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, ResearchPurple, RoundedCornerShape(10.dp))
                .padding(14.dp),
            decorationBox = { innerTextField ->
                if (value.isBlank()) {
                    Text(
                        text = "For example: I was scolded by my boss at a meeting today, and the AI immediately generated: \"Let’s not talk about work tonight. Let me tell you a story about the forest dog Floppy...\"",
                        color = Color(0xFF9BA1B7),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
                innerTextField()
            }
        )
    }
}

@Composable
private fun ResearchTitle(
    title: String,
    subtitle: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 23.sp,
            lineHeight = 25.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtitle,
            color = Color(0xFF8C93AB),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun ResearchChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(if (selected) Color(0xFFF4F5FA) else Color(0xFF262C42))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color(0xFF11131C) else Color(0xFFA5ABBE),
            fontSize = 11.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun ResearchBottomButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = ResearchPurple,
                contentColor = Color.White,
                disabledContainerColor = ResearchPurple.copy(alpha = 0.38f),
                disabledContentColor = Color.White.copy(alpha = 0.65f)
            ),
            modifier = Modifier
                .width(124.dp)
                .height(45.dp)
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun isResearchStepValid(step: ResearchStep, profile: UserProfile): Boolean {
    return when (step) {
        ResearchStep.Gender -> profile.gender == Gender.Male || profile.gender == Gender.Female
        ResearchStep.Age -> true
        ResearchStep.Career -> true
        ResearchStep.Content -> profile.contentPreferences.isNotEmpty()
        ResearchStep.Companion -> true
    }
}

private fun AgeRange.researchLabel(): String {
    return when (this) {
        AgeRange.Under18 -> "under the age of 18"
        AgeRange.From18To24 -> "18–24 year–old"
        AgeRange.From25To34 -> "25–34 Years–old"
        AgeRange.From35To44 -> "35–44 years–old"
        AgeRange.From45Plus -> "45 years–old"
    }
}

private val ResearchBackground = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF111936),
        Color(0xFF090E22),
        Color(0xFF060A16)
    )
)

private val ResearchPurple = Color(0xFF7D6BFF)

private val HomeBackground = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF101833),
        Color(0xFF070C1B),
        Color(0xFF020711)
    )
)

private val ChatBackground = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF111832),
        Color(0xFF070C1C),
        Color(0xFF020711)
    )
)

private val SettingsBackground = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF101833),
        Color(0xFF080D1F),
        Color(0xFF030711)
    )
)

private val AudioPageBackground = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF070B16),
        Color(0xFF090E22),
        Color(0xFF080C18)
    )
)

private val AudioPanel = Color(0xFF222A4E)

private val AudioSelectedTabBrush = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF30375A),
        Color(0xFF6B5FE8),
        Color(0xFF2D345A)
    )
)

@Composable
private fun Modifier.noRippleClickable(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    return clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        enabled = enabled,
        onClick = onClick
    )
}

@Composable
private fun SharedPageBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Image(
            painter = painterResource(id = R.drawable.page_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun MainShell(
    state: FloppyUiState,
    snackbarHostState: SnackbarHostState,
    onStartSpeechListening: () -> Long,
    onStopSpeechListening: (Long) -> Unit,
    onStartStreamingSpeech: (() -> Unit) -> Boolean,
    onStopStreamingSpeech: () -> Unit,
    onSpeechPartial: (Long, String) -> Unit,
    onSpeechComplete: (Long, String?) -> Unit,
    onSpeechRecordingComplete: (Long, Uri, String, String?) -> Unit,
    onSpeechError: (Long, String) -> Unit,
    onSpeechPermissionDenied: () -> Unit,
    onCancelSpeechListening: () -> Unit,
    onPlay: (AudioItem) -> Unit,
    onPauseOrResume: () -> Unit,
    onFeedback: (Int, String?) -> Unit,
    onStartUpload: (Uri, String, String, String?) -> Unit,
    onRetryUpload: (String) -> Unit,
    onCompleteUpload: (String) -> Unit,
    onSubmitChatMessage: (String) -> Unit,
    onUpdateSettings: (AppSettings) -> Unit,
    onRefreshVoiceOptions: () -> Unit,
    onSaveVoiceSelection: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(MainTab.Home) }
    var previousTab by remember { mutableStateOf<MainTab?>(null) }
    var lastExitBackTime by remember { mutableStateOf(0L) }

    fun openTab(tab: MainTab) {
        if (tab == selectedTab) return
        previousTab = selectedTab
        selectedTab = tab
        lastExitBackTime = 0L
    }

    fun goBackToPreviousOrHome() {
        val targetTab = previousTab?.takeIf { it != selectedTab } ?: MainTab.Home
        previousTab = if (targetTab == MainTab.Home) null else MainTab.Home
        selectedTab = targetTab
        lastExitBackTime = 0L
    }

    fun requestExit() {
        val now = System.currentTimeMillis()
        if (now - lastExitBackTime <= ExitBackWindowMillis) {
            (context as? Activity)?.finish()
        } else {
            lastExitBackTime = now
            Toast.makeText(context, "连续返回两次离开陪伴", Toast.LENGTH_SHORT).show()
        }
    }

    BackHandler {
        when (selectedTab) {
            MainTab.UploadLink,
            MainTab.Home -> requestExit()

            MainTab.Settings,
            MainTab.Library,
            MainTab.Chat -> goBackToPreviousOrHome()
        }
    }

    Scaffold(
        containerColor = Color.Black,
        contentColor = Color.White,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                MainTab.Home -> HomeScreen(
                    state = state,
                    onStartSpeechListening = onStartSpeechListening,
                    onStopSpeechListening = onStopSpeechListening,
                    onStartStreamingSpeech = onStartStreamingSpeech,
                    onStopStreamingSpeech = onStopStreamingSpeech,
                    onSpeechPartial = onSpeechPartial,
                    onSpeechComplete = onSpeechComplete,
                    onSpeechRecordingComplete = onSpeechRecordingComplete,
                    onSpeechError = onSpeechError,
                    onSpeechPermissionDenied = onSpeechPermissionDenied,
                    onCancelSpeechListening = onCancelSpeechListening,
                    onOpenHome = { openTab(MainTab.Home) },
                    onOpenUploadLink = { openTab(MainTab.UploadLink) },
                    onOpenLibrary = { openTab(MainTab.Library) },
                    onOpenChat = { openTab(MainTab.Chat) },
                    onOpenSettings = { openTab(MainTab.Settings) },
                    onPauseOrResume = onPauseOrResume
                )

                MainTab.UploadLink -> LinkUploadScreen(
                    onOpenHome = { openTab(MainTab.Home) },
                    onOpenSettings = { openTab(MainTab.Settings) },
                    onOpenLibrary = { openTab(MainTab.Library) },
                    onOpenChat = { openTab(MainTab.Chat) }
                )

                MainTab.Library -> AudioLibraryScreen(
                    state = state,
                    onBack = ::goBackToPreviousOrHome,
                    onStartUpload = onStartUpload,
                    onRetryUpload = onRetryUpload,
                    onCompleteUpload = onCompleteUpload,
                    onPlay = onPlay,
                    onPauseOrResume = onPauseOrResume
                )

                MainTab.Chat -> ChatScreen(
                    messages = state.chatMessages,
                    playback = state.playback,
                    isSending = state.isSubmittingTextIntent,
                    onBack = ::goBackToPreviousOrHome,
                    onSendMessage = onSubmitChatMessage,
                    onPlayAudio = onPlay,
                    onPauseOrResume = onPauseOrResume
                )

                MainTab.Settings -> SettingsScreen(
                    settings = state.settings,
                    voiceOptions = state.voiceOptions,
                    isLoadingVoiceOptions = state.isLoadingVoiceOptions,
                    isSavingVoiceSelection = state.isSavingVoiceSelection,
                    selectedVoiceId = state.selectedVoiceId,
                    onBack = ::goBackToPreviousOrHome,
                    onUpdateSettings = onUpdateSettings,
                    onRefreshVoiceOptions = onRefreshVoiceOptions,
                    onSaveVoiceSelection = onSaveVoiceSelection
                )
            }
        }
    }
}

@Composable
private fun ChatScreen(
    messages: List<ChatMessage>,
    playback: PlaybackUiState,
    isSending: Boolean,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    onPlayAudio: (AudioItem) -> Unit,
    onPauseOrResume: () -> Unit
) {
    var input by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        SharedPageBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.statusBars.asPaddingValues())
                .imePadding()
                .navigationBarsPadding()
        ) {
            ChatTopBar(onBack = onBack)
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(
                    start = 24.dp,
                    top = 22.dp,
                    end = 24.dp,
                    bottom = 20.dp
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(messages) { message ->
                    ChatMessageRow(
                        message = message,
                        playback = playback,
                        onPlayAudio = onPlayAudio,
                        onPauseOrResume = onPauseOrResume
                    )
                }
            }
            ChatInputBar(
                value = input,
                onValueChange = { input = it },
                onSend = {
                    val nextMessage = input.trim()
                    if (nextMessage.isNotEmpty() && !isSending) {
                        onSendMessage(nextMessage)
                        input = ""
                    }
                },
                enabled = !isSending,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 31.dp)
                    .padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun ChatAmbientGlow() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            color = ResearchPurple.copy(alpha = 0.18f),
            radius = size.width * 0.42f,
            center = Offset(-size.width * 0.08f, size.height * 0.37f)
        )
        drawCircle(
            color = Color(0xFF2769CE).copy(alpha = 0.12f),
            radius = size.width * 0.44f,
            center = Offset(size.width * 1.06f, size.height * 0.13f)
        )
        drawCircle(
            color = ResearchPurple.copy(alpha = 0.10f),
            radius = size.width * 0.40f,
            center = Offset(size.width * 0.78f, size.height * 1.06f)
        )
    }
}

@Composable
private fun ChatTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .background(Color(0xFF111832).copy(alpha = 0.40f))
            .padding(horizontal = 18.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(40.dp)
                .clip(CircleShape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            BackArrowGlyph()
        }
        Text(
            text = "chat",
            color = Color.White,
            fontSize = 20.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun BackArrowGlyph() {
    Canvas(modifier = Modifier.size(24.dp)) {
        val stroke = Stroke(width = 2.1.dp.toPx(), cap = StrokeCap.Round)
        drawLine(
            color = Color.White,
            start = Offset(size.width * 0.68f, size.height * 0.18f),
            end = Offset(size.width * 0.32f, size.height * 0.50f),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White,
            start = Offset(size.width * 0.32f, size.height * 0.50f),
            end = Offset(size.width * 0.68f, size.height * 0.82f),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun ChatMessageRow(
    message: ChatMessage,
    playback: PlaybackUiState,
    onPlayAudio: (AudioItem) -> Unit,
    onPauseOrResume: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.outgoing) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!message.outgoing) {
            ChatAvatar()
            Spacer(modifier = Modifier.width(9.dp))
        }
        ChatBubble(
            message = message,
            playback = playback,
            onPlayAudio = onPlayAudio,
            onPauseOrResume = onPauseOrResume
        )
    }
}

@Composable
private fun ChatAvatar() {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFE8E9F6),
                        Color(0xFF7E72CE),
                        Color(0xFF313653)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        OptionalDrawable(
            name = "settings_floppy_avatar",
            contentDescription = "Floppy",
            modifier = Modifier.size(35.dp),
            contentScale = ContentScale.Fit
        ) {
            SettingsFloppyAvatar(
                modifier = Modifier.fillMaxSize(),
                sleepy = false
            )
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    playback: PlaybackUiState,
    onPlayAudio: (AudioItem) -> Unit,
    onPauseOrResume: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val modifier = Modifier
        .widthIn(max = if (message.outgoing) 304.dp else 292.dp)
        .clip(shape)
        .then(
            if (message.outgoing) {
                Modifier.background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF8A78FF), Color(0xFF7769F4))
                    )
                )
            } else {
                Modifier
                    .background(Color(0xFF202844).copy(alpha = 0.96f))
                    .border(1.dp, Color(0xFF354162), shape)
            }
        )
        .padding(horizontal = if (message.outgoing) 22.dp else 20.dp, vertical = 16.dp)

    val audio = message.audio
    if (audio != null && !message.outgoing) {
        AssistantAudioBubble(
            message = message,
            audio = audio,
            playback = playback,
            onPlayAudio = onPlayAudio,
            onPauseOrResume = onPauseOrResume,
            modifier = modifier
        )
    } else {
        Box(modifier = modifier) {
            Text(
                text = message.body,
                color = Color.White,
                fontSize = 18.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
private fun AssistantAudioBubble(
    message: ChatMessage,
    audio: AudioItem,
    playback: PlaybackUiState,
    onPlayAudio: (AudioItem) -> Unit,
    onPauseOrResume: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showTranscript by rememberSaveable(audio.id, message.body) { mutableStateOf(false) }
    val isCurrent = playback.currentAudio?.id == audio.id
    val isPlaying = isCurrent &&
        playback.state in setOf(PlaybackState.Playing, PlaybackState.Buffering)

    Column(
        modifier = modifier.widthIn(min = 218.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White.copy(alpha = 0.10f))
                .clickable { if (isCurrent) onPauseOrResume() else onPlayAudio(audio) }
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                if (isPlaying) {
                    VoicePauseGlyph()
                } else {
                    VoicePlayGlyph()
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            ChatVoiceWaveform(isPlaying = isPlaying, modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = formatTime(audio.durationSeconds.toLong()),
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 13.sp,
                lineHeight = 13.sp,
                maxLines = 1
            )
        }
        if (message.body.isNotBlank()) {
            Text(
                text = if (showTranscript) "收起文字" else "转文字",
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 13.sp,
                lineHeight = 15.sp,
                modifier = Modifier
                    .align(Alignment.End)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showTranscript = !showTranscript }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
            if (showTranscript) {
                Text(
                    text = message.body,
                    color = Color.White,
                    fontSize = 16.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun ChatVoiceWaveform(isPlaying: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "chat-audio-wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 920, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "chat-audio-wave-phase"
    )
    val bars = 18

    Row(
        modifier = modifier.height(24.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(bars) { index ->
            val wave = if (isPlaying) {
                0.5f + 0.5f * sin((phase * 2f * PI + index * 0.72f)).toFloat()
            } else {
                0.36f + ((index % 5) * 0.11f)
            }
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((7 + wave * 17).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = if (isPlaying) 0.90f else 0.58f))
            )
        }
    }
}

@Composable
private fun VoicePlayGlyph() {
    Canvas(modifier = Modifier.size(13.dp)) {
        val path = Path().apply {
            moveTo(size.width * 0.28f, size.height * 0.18f)
            lineTo(size.width * 0.78f, size.height * 0.50f)
            lineTo(size.width * 0.28f, size.height * 0.82f)
            close()
        }
        drawPath(path, Color.White)
    }
}

@Composable
private fun VoicePauseGlyph() {
    Row(
        modifier = Modifier.size(13.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(2) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(12.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White)
            )
        }
    }
}

@Composable
private fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val canSend = enabled && value.isNotBlank()

    Row(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White)
            .padding(start = 20.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = value,
            onValueChange = { if (enabled) onValueChange(it.take(160)) },
            enabled = enabled,
            singleLine = true,
            cursorBrush = SolidColor(ResearchPurple),
            textStyle = TextStyle(
                color = Color(0xFF67708B),
                fontSize = 14.sp,
                lineHeight = 18.sp
            ),
            modifier = Modifier.weight(1f),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isBlank()) {
                        Text(
                            text = "Type a message...",
                            color = Color(0xFFC7CCD8),
                            fontSize = 14.sp,
                            lineHeight = 18.sp
                        )
                    }
                    innerTextField()
                }
            }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(ResearchPurple.copy(alpha = if (canSend) 1f else 0.50f))
                .clickable(enabled = canSend, onClick = onSend),
            contentAlignment = Alignment.Center
        ) {
            SendGlyph()
        }
    }
}

@Composable
private fun SendGlyph() {
    Canvas(modifier = Modifier.size(22.dp)) {
        val plane = Path().apply {
            moveTo(size.width * 0.10f, size.height * 0.47f)
            lineTo(size.width * 0.88f, size.height * 0.12f)
            lineTo(size.width * 0.58f, size.height * 0.88f)
            lineTo(size.width * 0.43f, size.height * 0.60f)
            lineTo(size.width * 0.10f, size.height * 0.47f)
        }
        drawPath(plane, color = Color.White)
        drawLine(
            color = ResearchPurple.copy(alpha = 0.42f),
            start = Offset(size.width * 0.43f, size.height * 0.60f),
            end = Offset(size.width * 0.88f, size.height * 0.12f),
            strokeWidth = 1.2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun HomeScreen(
    state: FloppyUiState,
    onStartSpeechListening: () -> Long,
    onStopSpeechListening: (Long) -> Unit,
    onStartStreamingSpeech: (() -> Unit) -> Boolean,
    onStopStreamingSpeech: () -> Unit,
    onSpeechPartial: (Long, String) -> Unit,
    onSpeechComplete: (Long, String?) -> Unit,
    onSpeechRecordingComplete: (Long, Uri, String, String?) -> Unit,
    onSpeechError: (Long, String) -> Unit,
    onSpeechPermissionDenied: () -> Unit,
    onCancelSpeechListening: () -> Unit,
    onOpenHome: () -> Unit,
    onOpenUploadLink: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenSettings: () -> Unit,
    onPauseOrResume: () -> Unit
) {
    val activeAudio = state.playback.currentAudio ?: state.activeAudio
    val isPlaying = state.playback.state == PlaybackState.Playing ||
        state.playback.state == PlaybackState.Buffering
    val hasPlaybackAudio = state.playback.currentAudio != null
    val isDogListening = state.isListening
    val isSignalAnimating = isPlaying ||
        state.isListening ||
        state.isSubmittingTextIntent ||
        state.agentState in setOf(AgentState.Listening, AgentState.Recommending, AgentState.Generating)
    val networkSignalLevel = rememberNetworkSignalLevel()
    val statusLabel = when {
        state.isListening -> "Floppy 正在听你说"
        else -> state.generationMessage ?: agentStatusText(state)
    }
    val speechInputController = rememberSpeechInputController(
        isListening = state.isListening,
        onStartListening = onStartSpeechListening,
        onStopListening = onStopSpeechListening,
        onStartStreaming = onStartStreamingSpeech,
        onStopStreaming = onStopStreamingSpeech,
        onPartialResult = onSpeechPartial,
        onFinalResult = onSpeechComplete,
        onRecordingResult = onSpeechRecordingComplete,
        onError = onSpeechError,
        onPermissionDenied = onSpeechPermissionDenied,
        onCancelListening = onCancelSpeechListening
    )
    ShakeToListenEffect(
        onShake = speechInputController.startListening
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        SharedPageBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.statusBars.asPaddingValues())
                .padding(top = 12.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            HomeTopBar(
                networkSignalLevel = networkSignalLevel,
                onSettingsClick = onOpenSettings,
                modifier = Modifier.padding(horizontal = 28.dp)
            )
            HomeCenterStage(
                activeAudio = activeAudio,
                isDogListening = isDogListening,
                isSignalAnimating = isSignalAnimating,
                hasPlaybackAudio = hasPlaybackAudio,
                statusLabel = statusLabel,
                speechTranscript = state.currentVoiceTranscript.takeIf {
                    state.isListening || state.isSubmittingVoiceIntent
                },
                onToggleConversation = speechInputController.toggleListening,
                onTogglePlayback = onPauseOrResume,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
            HomeBottomArea(
                isSignalAnimating = isSignalAnimating,
                onOpenHome = onOpenHome,
                onOpenUploadLink = onOpenUploadLink,
                onOpenLibrary = onOpenLibrary,
                onOpenChat = onOpenChat,
                modifier = Modifier.padding(horizontal = 28.dp)
            )
        }
    }
}

private class SpeechInputController(
    val startListening: () -> Unit,
    val toggleListening: () -> Unit
)

@Composable
private fun ShakeToListenEffect(
    onShake: () -> Unit
) {
    val context = LocalContext.current
    val latestOnShake by rememberUpdatedState(onShake)

    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (sensorManager == null || accelerometer == null) {
            return@DisposableEffect onDispose { }
        }

        var lastShakeAt = 0L
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values.getOrNull(0) ?: return
                val y = event.values.getOrNull(1) ?: return
                val z = event.values.getOrNull(2) ?: return
                val acceleration = kotlin.math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                val shakeForce = kotlin.math.abs(acceleration - SensorManager.GRAVITY_EARTH)
                val now = android.os.SystemClock.elapsedRealtime()
                if (shakeForce > 10.5f && now - lastShakeAt > 1200L) {
                    lastShakeAt = now
                    latestOnShake()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }
}

@Composable
private fun rememberSpeechInputController(
    isListening: Boolean,
    onStartListening: () -> Long,
    onStopListening: (Long) -> Unit,
    onStartStreaming: (() -> Unit) -> Boolean,
    onStopStreaming: () -> Unit,
    onPartialResult: (Long, String) -> Unit,
    onFinalResult: (Long, String?) -> Unit,
    onRecordingResult: (Long, Uri, String, String?) -> Unit,
    onError: (Long, String) -> Unit,
    onPermissionDenied: () -> Unit,
    onCancelListening: () -> Unit
): SpeechInputController {
    val context = LocalContext.current
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var introPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isIntroPlaying by remember { mutableStateOf(false) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordingFile by remember { mutableStateOf<File?>(null) }
    var isBackendRecording by remember { mutableStateOf(false) }
    var isStreamingSpeech by remember { mutableStateOf(false) }
    var activeSpeechSessionId by remember { mutableStateOf<Long?>(null) }
    var backendRecordingStartedAt by remember { mutableStateOf(0L) }
    var recognitionStartedAt by remember { mutableStateOf(0L) }
    var recognitionSessionId by remember { mutableIntStateOf(0) }
    var recognitionCallbackSeen by remember { mutableStateOf(false) }
    val latestIsListening by rememberUpdatedState(isListening)
    val latestOnStartListening by rememberUpdatedState(onStartListening)
    val latestOnStopListening by rememberUpdatedState(onStopListening)
    val latestOnStartStreaming by rememberUpdatedState(onStartStreaming)
    val latestOnStopStreaming by rememberUpdatedState(onStopStreaming)
    val latestOnPartialResult by rememberUpdatedState(onPartialResult)
    val latestOnFinalResult by rememberUpdatedState(onFinalResult)
    val latestOnRecordingResult by rememberUpdatedState(onRecordingResult)
    val latestOnError by rememberUpdatedState(onError)
    val latestOnPermissionDenied by rememberUpdatedState(onPermissionDenied)
    val latestOnCancelListening by rememberUpdatedState(onCancelListening)

    LaunchedEffect(isListening) {
        if (!isListening && isStreamingSpeech) {
            isStreamingSpeech = false
        }
    }

    fun releaseRecognizer(cancelRecognition: Boolean = true) {
        val recognizer = speechRecognizer ?: return
        speechRecognizer = null
        recognitionCallbackSeen = true
        if (cancelRecognition) {
            recognizer.cancel()
        }
        recognizer.destroy()
    }

    fun createSpeechRecordingFile(): File {
        val dir = File(context.cacheDir, "speech-recordings").apply { mkdirs() }
        return File(dir, "floppy-speech-${System.currentTimeMillis()}.m4a")
    }

    fun cancelBackendRecording() {
        val activeRecorder = recorder
        if (activeRecorder != null) {
            runCatching { activeRecorder.stop() }
            activeRecorder.release()
        }
        recorder = null
        recordingFile = null
        isBackendRecording = false
    }

    fun requireSpeechSession(): Long {
        return activeSpeechSessionId ?: latestOnStartListening().also { sessionId ->
            activeSpeechSessionId = sessionId
        }
    }

    fun clearSpeechSession(sessionId: Long) {
        if (activeSpeechSessionId == sessionId) {
            activeSpeechSessionId = null
        }
    }

    fun startBackendRecording() {
        releaseRecognizer()
        cancelBackendRecording()
        val sessionId = latestOnStartListening()
        activeSpeechSessionId = sessionId
        val outputFile = createSpeechRecordingFile()
        val nextRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        runCatching {
            nextRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            nextRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            nextRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            nextRecorder.setAudioSamplingRate(44100)
            nextRecorder.setAudioEncodingBitRate(96000)
            nextRecorder.setOutputFile(outputFile.absolutePath)
            nextRecorder.prepare()
            nextRecorder.start()
        }.onSuccess {
            recorder = nextRecorder
            recordingFile = outputFile
            isBackendRecording = true
            backendRecordingStartedAt = android.os.SystemClock.elapsedRealtime()
            Log.d(SpeechLogTag, "Started backend speech recording: ${outputFile.name}")
        }.onFailure { error ->
            clearSpeechSession(sessionId)
            nextRecorder.release()
            Log.d(SpeechLogTag, "Backend speech recording failed", error)
            latestOnError(sessionId, "录音启动失败，请再试一次")
        }
    }

    fun stopBackendRecording() {
        val activeRecorder = recorder ?: return
        val sessionId = activeSpeechSessionId ?: return
        val activeFile = recordingFile
        val elapsedSinceStart = android.os.SystemClock.elapsedRealtime() - backendRecordingStartedAt
        if (elapsedSinceStart < 700L) {
            Log.d(SpeechLogTag, "Ignoring backend recording stop; recording just started ${elapsedSinceStart}ms ago")
            return
        }
        recorder = null
        recordingFile = null
        isBackendRecording = false
        val stopped = runCatching {
            activeRecorder.stop()
        }
        activeRecorder.release()
        latestOnStopListening(sessionId)
        if (activeFile == null || stopped.isFailure || activeFile.length() <= 0L) {
            Log.d(SpeechLogTag, "Backend speech recording produced no audio", stopped.exceptionOrNull())
            latestOnFinalResult(sessionId, null)
            clearSpeechSession(sessionId)
            return
        }
        Log.d(SpeechLogTag, "Completed backend speech recording: ${activeFile.name}, ${activeFile.length()} bytes")
        latestOnRecordingResult(sessionId, Uri.fromFile(activeFile), activeFile.name, "audio/mp4")
    }

    fun startStreamingOrFallback() {
        releaseRecognizer()
        cancelBackendRecording()
        isStreamingSpeech = latestOnStartStreaming {
            Log.d(SpeechLogTag, "Streaming ASR unavailable; falling back to uploaded recording")
            isStreamingSpeech = false
            startBackendRecording()
        }
        if (!isStreamingSpeech) {
            Log.d(SpeechLogTag, "Streaming ASR client unavailable; falling back to uploaded recording")
            startBackendRecording()
        } else {
            Log.d(SpeechLogTag, "Started streaming speech")
        }
    }

    fun releaseIntroPlayer() {
        introPlayer?.release()
        introPlayer = null
        isIntroPlaying = false
    }

    fun startRecognition() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.d(SpeechLogTag, "Speech recognition is not available")
            startBackendRecording()
            return
        }

        releaseRecognizer()
        Log.d(SpeechLogTag, "Starting speech recognition")
        recognitionStartedAt = android.os.SystemClock.elapsedRealtime()
        recognitionCallbackSeen = false
        val sessionId = ++recognitionSessionId
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context).also { speechRecognizer = it }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                recognitionCallbackSeen = true
                Log.d(SpeechLogTag, "Ready for speech")
            }

            override fun onBeginningOfSpeech() {
                recognitionCallbackSeen = true
                Log.d(SpeechLogTag, "Beginning of speech")
                requireSpeechSession()
            }

            override fun onRmsChanged(rmsdB: Float) = Unit

            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() {
                recognitionCallbackSeen = true
                Log.d(SpeechLogTag, "End of speech")
                activeSpeechSessionId?.let(latestOnStopListening)
            }

            override fun onError(error: Int) {
                val fallbackTranscript = null
                recognitionCallbackSeen = true
                Log.d(SpeechLogTag, "Speech recognition error: $error")
                releaseRecognizer(cancelRecognition = false)
                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> activeSpeechSessionId?.let { sessionId ->
                        latestOnFinalResult(sessionId, fallbackTranscript)
                        clearSpeechSession(sessionId)
                    }
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> latestOnPermissionDenied()
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                        activeSpeechSessionId?.let { sessionId ->
                            latestOnStopListening(sessionId)
                            latestOnError(sessionId, "语音识别刚刚停下，请再点一次")
                            clearSpeechSession(sessionId)
                        }
                    }
                    else -> activeSpeechSessionId?.let { sessionId ->
                        latestOnError(sessionId, speechErrorMessage(error))
                        clearSpeechSession(sessionId)
                    }
                }
            }

            override fun onResults(results: Bundle?) {
                val transcript = results.bestSpeechText()
                recognitionCallbackSeen = true
                Log.d(SpeechLogTag, "Final result: ${transcript.orEmpty()}")
                releaseRecognizer(cancelRecognition = false)
                activeSpeechSessionId?.let { sessionId ->
                    latestOnFinalResult(sessionId, transcript)
                    clearSpeechSession(sessionId)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val transcript = partialResults.bestSpeechText()
                recognitionCallbackSeen = true
                Log.d(SpeechLogTag, "Partial result: ${transcript.orEmpty()}")
                val sessionId = activeSpeechSessionId
                if (sessionId != null && transcript != null) {
                    latestOnPartialResult(sessionId, transcript)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })

        activeSpeechSessionId = latestOnStartListening()
        runCatching {
            recognizer.startListening(speechRecognizerIntent())
        }.onSuccess {
            Log.d(SpeechLogTag, "startListening call returned")
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (sessionId == recognitionSessionId && speechRecognizer != null && !recognitionCallbackSeen) {
                    Log.d(SpeechLogTag, "No callback from system speech recognition service")
                    releaseRecognizer(cancelRecognition = false)
                    startBackendRecording()
                }
            }, 3500L)
        }.onFailure { error ->
            Log.d(SpeechLogTag, "startListening call failed", error)
            releaseRecognizer(cancelRecognition = false)
            activeSpeechSessionId?.let { sessionId ->
                latestOnError(sessionId, "语音识别启动失败，请再点一次 Floppy")
                clearSpeechSession(sessionId)
            }
        }
    }

    fun stopRecognition() {
        releaseIntroPlayer()
        if (isStreamingSpeech) {
            latestOnStopStreaming()
            isStreamingSpeech = false
            return
        }
        if (isBackendRecording) {
            stopBackendRecording()
            return
        }
        val recognizer = speechRecognizer
        if (recognizer != null) {
            val sessionId = activeSpeechSessionId ?: return
            val elapsedSinceStart = android.os.SystemClock.elapsedRealtime() - recognitionStartedAt
            if (elapsedSinceStart < 1200L) {
                Log.d(SpeechLogTag, "Ignoring stop request; recognition just started ${elapsedSinceStart}ms ago")
                return
            }
            Log.d(SpeechLogTag, "Stopping speech recognition and waiting for final result")
            recognizer.stopListening()
            latestOnStopListening(sessionId)
            return
        }
    }

    fun playIntroThenStartRecognition() {
        releaseIntroPlayer()
        val player = MediaPlayer.create(context, R.raw.home_listening_intro)
        if (player == null) {
            startStreamingOrFallback()
            return
        }
        introPlayer = player
        isIntroPlaying = true
        player.setOnCompletionListener { completedPlayer ->
            if (introPlayer === completedPlayer) {
                releaseIntroPlayer()
                startStreamingOrFallback()
            } else {
                completedPlayer.release()
            }
        }
        player.setOnErrorListener { errorPlayer, _, _ ->
            if (introPlayer === errorPlayer) {
                releaseIntroPlayer()
                startStreamingOrFallback()
            } else {
                errorPlayer.release()
            }
            true
        }
        player.start()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            playIntroThenStartRecognition()
        } else {
            latestOnPermissionDenied()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            releaseIntroPlayer()
            releaseRecognizer()
            if (isStreamingSpeech) {
                latestOnStopStreaming()
                isStreamingSpeech = false
            }
            activeSpeechSessionId = null
            cancelBackendRecording()
            latestOnCancelListening()
        }
    }

    return remember(context, isListening, isIntroPlaying, isBackendRecording, isStreamingSpeech) {
        SpeechInputController(
            startListening = {
                Log.d(SpeechLogTag, "startListening requested; latestIsListening=$latestIsListening, isIntroPlaying=$isIntroPlaying, isBackendRecording=$isBackendRecording, isStreamingSpeech=$isStreamingSpeech")
                if (!latestIsListening && !isIntroPlaying && !isBackendRecording && !isStreamingSpeech) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                        PackageManager.PERMISSION_GRANTED
                    ) {
                        playIntroThenStartRecognition()
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            },
            toggleListening = {
                Log.d(SpeechLogTag, "toggleListening requested; latestIsListening=$latestIsListening, isIntroPlaying=$isIntroPlaying, isBackendRecording=$isBackendRecording, isStreamingSpeech=$isStreamingSpeech")
                if (latestIsListening || isIntroPlaying || isBackendRecording || isStreamingSpeech) {
                    stopRecognition()
                } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    playIntroThenStartRecognition()
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        )
    }
}

private fun speechRecognizerIntent(): Intent {
    return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.SIMPLIFIED_CHINESE.toLanguageTag())
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, Locale.SIMPLIFIED_CHINESE.toLanguageTag())
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Floppy 正在听")
    }
}

private fun Bundle?.bestSpeechText(): String? {
    return this
        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        ?.firstOrNull()
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
}

private fun speechErrorMessage(error: Int): String {
    return when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "录音失败，请再试一次"
        SpeechRecognizer.ERROR_CLIENT -> "语音识别启动失败，请再点一次 Floppy"
        SpeechRecognizer.ERROR_NETWORK -> "语音识别网络异常"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "语音识别网络超时"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "语音识别刚刚停下，请再点一次"
        SpeechRecognizer.ERROR_SERVER -> "语音识别服务暂时不可用"
        else -> "语音识别失败，请再试一次"
    }
}

@Composable
private fun HomeAmbientGlow() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            color = Color(0xFF6E57FF).copy(alpha = 0.11f),
            radius = size.width * 0.54f,
            center = Offset(size.width * 0.80f, size.height * 1.10f)
        )
    }
}

@Composable
private fun HomeTopBar(
    networkSignalLevel: Int,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OptionalDrawable(
                    name = "home_floppy_logo",
                    contentDescription = "Floppy",
                    modifier = Modifier
                        .width(98.dp)
                        .height(29.dp),
                    contentScale = ContentScale.Fit
                ) {
                    Text(
                        text = "Floppy",
                        color = Color(0xFFF4F3FF),
                        fontSize = 29.sp,
                        lineHeight = 29.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(33.dp),
                    contentAlignment = Alignment.Center
                ) {
                    PawSignal(
                        activeCount = networkSignalLevel,
                        totalCount = 4,
                        color = Color(0xFF7F7BFF),
                        inactiveAlpha = 0.28f,
                        modifier = Modifier
                            .width(60.dp)
                            .height(33.dp)
                    )
                }
            }
            FloppyCloudButton(onClick = onSettingsClick)
        }
    }
}

@Composable
private fun FloppyCloudButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .noRippleClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        OptionalDrawable(
            name = "home_floppy_cloud",
            contentDescription = "Floppy shortcut",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(width = 25.dp, height = 19.dp)) {
                    val cloudColor = Color(0xFFE5E5E7)
                    drawCircle(cloudColor, radius = size.height * 0.34f, center = Offset(size.width * 0.28f, size.height * 0.52f))
                    drawCircle(cloudColor, radius = size.height * 0.46f, center = Offset(size.width * 0.50f, size.height * 0.44f))
                    drawCircle(cloudColor, radius = size.height * 0.39f, center = Offset(size.width * 0.72f, size.height * 0.52f))
                    drawRoundRect(
                        color = cloudColor,
                        topLeft = Offset(size.width * 0.20f, size.height * 0.44f),
                        size = Size(size.width * 0.62f, size.height * 0.34f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx(), 12.dp.toPx())
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeCenterStage(
    activeAudio: AudioItem?,
    isDogListening: Boolean,
    isSignalAnimating: Boolean,
    hasPlaybackAudio: Boolean,
    statusLabel: String,
    speechTranscript: String?,
    onToggleConversation: () -> Unit,
    onTogglePlayback: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val clampedDogWidth = maxWidth
        val dogAspectRatio = 3f / 4f
        val dogForegroundWidthFraction = 1f
        val dogImageModifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth(dogForegroundWidthFraction)
            .aspectRatio(dogAspectRatio)
            .offset(y = (-32).dp)
        val context = LocalContext.current
        var showPlaybackHint by rememberSaveable {
            mutableStateOf(!context.getSharedPreferences("floppy_prefs", Context.MODE_PRIVATE).getBoolean("playback_hint_dismissed", false))
        }
        var showPlaybackBubble by rememberSaveable { mutableStateOf(true) }
        // 每次出现新音频气泡时递增，用来在三种图标间轮换
        var artworkVariant by rememberSaveable { mutableIntStateOf(0) }
        var renderedDogListening by remember { mutableStateOf(isDogListening) }
        var isDogTransitioning by remember { mutableStateOf(false) }
        var isDogInteractionLocked by remember { mutableStateOf(false) }
        var dogInteractionLockTarget by remember { mutableStateOf<Boolean?>(null) }
        val latestDogListening by rememberUpdatedState(isDogListening)
        LaunchedEffect(hasPlaybackAudio, activeAudio?.id) {
            if (hasPlaybackAudio) {
                artworkVariant = (artworkVariant + 1) % 3
                showPlaybackBubble = true
            }
        }
        LaunchedEffect(isDogListening) {
            if (isDogListening != renderedDogListening) {
                isDogTransitioning = true
            }
        }
        LaunchedEffect(dogInteractionLockTarget) {
            val lockTarget = dogInteractionLockTarget ?: return@LaunchedEffect
            delay(10_000)
            if (isDogInteractionLocked && dogInteractionLockTarget == lockTarget) {
                if (isDogTransitioning) {
                    renderedDogListening = latestDogListening
                    isDogTransitioning = false
                }
                isDogInteractionLocked = false
                dogInteractionLockTarget = null
            }
        }
        val dismissPlaybackHint = {
            context.getSharedPreferences("floppy_prefs", Context.MODE_PRIVATE).edit().putBoolean("playback_hint_dismissed", true).apply()
            showPlaybackHint = false
        }
        val toggleConversation = {
            if (!isDogInteractionLocked && !isDogTransitioning && isDogListening == renderedDogListening) {
                isDogInteractionLocked = true
                dogInteractionLockTarget = !isDogListening
                showPlaybackBubble = false
                onToggleConversation()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .clipToBounds()
                .align(Alignment.BottomCenter)
                .noRippleClickable(onClick = toggleConversation)
        ) {
            if (isDogTransitioning) {
                HomeDogGifStage(
                    resId = R.drawable.home_floppy_state_transition,
                    contentDescription = "Floppy changing state",
                    loop = false,
                    onAnimationEnd = {
                        renderedDogListening = latestDogListening
                        isDogTransitioning = false
                        if (dogInteractionLockTarget == latestDogListening) {
                            isDogInteractionLocked = false
                            dogInteractionLockTarget = null
                        }
                    },
                    foregroundModifier = dogImageModifier
                )
            } else if (renderedDogListening) {
                HomeDogGifStage(
                    resId = R.drawable.home_floppy_listening_call,
                    contentDescription = "Floppy listening",
                    loop = true,
                    foregroundModifier = dogImageModifier
                )
            } else {
                HomeDogGifStage(
                    resId = R.drawable.home_floppy_idle,
                    contentDescription = "Floppy quiet",
                    loop = true,
                    foregroundModifier = dogImageModifier
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(86.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black,
                                Color(0xFF020713).copy(alpha = 0.82f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(250.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFF24317E).copy(alpha = 0.24f),
                                Color(0xFF3A43A3).copy(alpha = 0.70f),
                                Color(0xFF4B54AE),
                                Color(0xFF4B54AE)
                            )
                        )
                    )
            )
            if (!hasPlaybackAudio || !showPlaybackBubble) {
                HomeStateTextBlock(
                    statusText = statusLabel,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = 40.dp, end = 40.dp, bottom = 24.dp)
                )
            }
            speechTranscript
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let { transcript ->
                    Text(
                        text = "你说：$transcript",
                        color = Color.White.copy(alpha = 0.84f),
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(start = 40.dp, end = 40.dp, bottom = 56.dp)
                            .widthIn(max = 320.dp)
                    )
                }
            if (hasPlaybackAudio && showPlaybackBubble) {
                PlayingBubble(
                    audio = activeAudio,
                    variant = artworkVariant,
                    onClick = {
                        dismissPlaybackHint()
                        onTogglePlayback()
                    },
                    modifier = Modifier
                        .width(clampedDogWidth * 0.27f)
                        .align(Alignment.TopStart)
                        .offset(x = clampedDogWidth * 0.12f, y = clampedDogWidth * 0.19f)
                )
            }
        }

        if (hasPlaybackAudio && showPlaybackBubble && showPlaybackHint) {
            val offsetAnim = remember { Animatable(0f) }
            LaunchedEffect(Unit) {
                repeat(3) {
                    offsetAnim.animateTo(6f, tween(200))
                    offsetAnim.animateTo(-6f, tween(200))
                }
                offsetAnim.animateTo(0f, tween(150))
            }
            PlaybackToggleHintText(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = clampedDogWidth * 0.42f, y = clampedDogWidth * 0.20f)
                    .graphicsLayer { translationX = offsetAnim.value }
                    .noRippleClickable { dismissPlaybackHint() }
            )
        }
    }
}

@Composable
private fun HomeDogGifStage(
    resId: Int,
    contentDescription: String?,
    loop: Boolean,
    onAnimationEnd: (() -> Unit)? = null,
    foregroundModifier: Modifier = Modifier
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF020713),
                            Color(0xFF050C18),
                            Color(0xFF0B1428),
                            Color(0xFF1A2552),
                            Color(0xFF5862D2)
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.18f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.18f)
                        )
                    )
                )
        )
        GifImage(
            resId = resId,
            contentDescription = contentDescription,
            loop = loop,
            onAnimationEnd = onAnimationEnd,
            modifier = foregroundModifier,
            scaleType = ImageView.ScaleType.FIT_CENTER
        )
    }
}

@Composable
private fun HomeStateTextBlock(
    statusText: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.widthIn(max = 320.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text(
            text = statusText,
            color = Color.White.copy(alpha = 0.92f),
            fontSize = 17.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun GifImage(
    resId: Int,
    contentDescription: String?,
    loop: Boolean,
    onAnimationEnd: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    scaleType: ImageView.ScaleType = ImageView.ScaleType.CENTER_CROP
) {
    val latestOnAnimationEnd by rememberUpdatedState(onAnimationEnd)
    key(resId, loop) {
        AndroidView(
            factory = { context ->
                ImageView(context).apply {
                    this.contentDescription = contentDescription
                    this.scaleType = scaleType
                    setImageResource(resId)
                    startGifDrawable(loop = loop, onAnimationEnd = { latestOnAnimationEnd?.invoke() })
                }
            },
            update = { imageView ->
                imageView.contentDescription = contentDescription
                imageView.scaleType = scaleType
            },
            modifier = modifier
        )
    }
}

private fun ImageView.startGifDrawable(
    loop: Boolean,
    onAnimationEnd: (() -> Unit)? = null
) {
    val animatedDrawable = drawable as? AnimatedImageDrawable
    if (animatedDrawable != null) {
        animatedDrawable.stop()
        animatedDrawable.clearAnimationCallbacks()
        animatedDrawable.repeatCount = if (loop) {
            AnimatedImageDrawable.REPEAT_INFINITE
        } else {
            0
        }
        if (!loop && onAnimationEnd != null) {
            animatedDrawable.registerAnimationCallback(
                object : Animatable2.AnimationCallback() {
                    override fun onAnimationEnd(drawable: Drawable?) {
                        onAnimationEnd()
                    }
                }
            )
        }
        animatedDrawable.start()
    } else {
        (drawable as? DrawableAnimatable)?.start()
        if (!loop) {
            postDelayed({ onAnimationEnd?.invoke() }, 900L)
        }
    }
}

@Composable
private fun PlaybackToggleHintText(modifier: Modifier = Modifier) {
    HomeHintText(
        text = "点击我进行播放和暂停",
        modifier = modifier
    )
}

@Composable
private fun HomeHintText(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF111832).copy(alpha = 0.82f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(
            text = text,
            color = Color(0xFFF4F3FF),
            fontSize = 13.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun FloppyOrb(
    isDogActive: Boolean,
    showRing: Boolean,
    isFlowing: Boolean,
    modifier: Modifier = Modifier
) {
    val ringStartAngle = if (isFlowing) {
        val transition = rememberInfiniteTransition(label = "home-orb")
        val sweep by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "home-orb-sweep"
        )
        sweep
    } else {
        0f
    }

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val dogSize = (maxWidth - 58.dp).coerceAtLeast(160.dp)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val outerGap = 29.dp.toPx()
            val innerGap = 14.dp.toPx()
            val iconRadius = (size.minDimension - outerGap * 2f) / 2f
            val innerRadius = iconRadius + innerGap
            val outerRadius = size.minDimension / 2f
            val strokeWidth = (outerRadius - innerRadius).coerceAtLeast(1.dp.toPx())
            drawCircle(
                color = Color(0xFF303077).copy(alpha = 0.34f),
                radius = outerRadius
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF202B52),
                        Color(0xFF101832),
                        Color(0xFF0D1327)
                    ),
                    center = Offset(size.width * 0.46f, size.height * 0.40f),
                    radius = innerRadius
                ),
                radius = innerRadius
            )
            if (showRing || isFlowing) {
                val arcInset = strokeWidth / 2f
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF766BFF),
                            Color(0xFF9C92FF),
                            Color.Transparent
                        )
                    ),
                    startAngle = ringStartAngle,
                    sweepAngle = 116f,
                    useCenter = false,
                    topLeft = Offset(arcInset, arcInset),
                    size = Size(size.width - arcInset * 2f, size.height - arcInset * 2f),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                drawCircle(
                    color = Color(0xFF8B7BFF),
                    radius = innerRadius,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
        HomeFloppyDog(
            isActivated = isDogActive,
            modifier = Modifier
                .size(dogSize)
        )
    }
}

@Composable
private fun HomeFloppyDog(
    isActivated: Boolean,
    modifier: Modifier = Modifier
) {
    if (isActivated) {
        OptionalDrawable(
            name = "home_floppy_active",
            contentDescription = "Floppy active",
            modifier = modifier,
            contentScale = ContentScale.Fit
        ) {
            SettingsFloppyAvatar(
                modifier = Modifier.fillMaxSize(),
                sleepy = true
            )
        }
    } else {
        OptionalDrawable(
            name = "home_floppy_thinking",
            contentDescription = "Floppy waiting",
            modifier = modifier,
            contentScale = ContentScale.Fit
        ) {
            ThinkingFloppyFallback(modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun ThinkingFloppyFallback(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawCircle(
            color = Color.White.copy(alpha = 0.18f),
            radius = size.minDimension * 0.37f,
            center = Offset(size.width * 0.52f, size.height * 0.62f)
        )
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFE9E9F6), Color(0xFF9FA2BE)),
                startY = size.height * 0.54f,
                endY = size.height * 0.88f
            ),
            topLeft = Offset(size.width * 0.18f, size.height * 0.56f),
            size = Size(size.width * 0.70f, size.height * 0.26f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width * 0.17f, size.width * 0.17f)
        )
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF4C4B61), Color(0xFF1A1A25), Color(0xFF0A0A11)),
                center = Offset(size.width * 0.46f, size.height * 0.30f),
                radius = size.width * 0.45f
            ),
            topLeft = Offset(size.width * 0.22f, size.height * 0.10f),
            size = Size(size.width * 0.56f, size.height * 0.54f)
        )
        drawOval(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF77728F), Color(0xFF15151F)),
                start = Offset(size.width * 0.18f, size.height * 0.10f),
                end = Offset(size.width * 0.07f, size.height * 0.42f)
            ),
            topLeft = Offset(size.width * 0.06f, size.height * 0.12f),
            size = Size(size.width * 0.22f, size.height * 0.34f)
        )
        drawOval(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF77728F), Color(0xFF15151F)),
                start = Offset(size.width * 0.72f, size.height * 0.12f),
                end = Offset(size.width * 0.88f, size.height * 0.44f)
            ),
            topLeft = Offset(size.width * 0.73f, size.height * 0.14f),
            size = Size(size.width * 0.20f, size.height * 0.33f)
        )
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFB7A9FF), Color(0xFF7668E8)),
                start = Offset(size.width * 0.35f, size.height * 0.55f),
                end = Offset(size.width * 0.63f, size.height * 0.72f)
            ),
            topLeft = Offset(size.width * 0.37f, size.height * 0.56f),
            size = Size(size.width * 0.27f, size.height * 0.18f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width * 0.07f, size.width * 0.07f)
        )
        val leftEye = Offset(size.width * 0.40f, size.height * 0.37f)
        val rightEye = Offset(size.width * 0.62f, size.height * 0.37f)
        drawOval(
            color = Color.White,
            topLeft = Offset(leftEye.x - size.width * 0.09f, leftEye.y - size.height * 0.05f),
            size = Size(size.width * 0.18f, size.height * 0.12f)
        )
        drawOval(
            color = Color.White,
            topLeft = Offset(rightEye.x - size.width * 0.09f, rightEye.y - size.height * 0.05f),
            size = Size(size.width * 0.18f, size.height * 0.12f)
        )
        drawCircle(Color(0xFF101018), radius = size.width * 0.022f, center = Offset(leftEye.x - size.width * 0.025f, leftEye.y))
        drawCircle(Color(0xFF101018), radius = size.width * 0.022f, center = Offset(rightEye.x - size.width * 0.04f, rightEye.y))
        drawOval(
            color = Color(0xFF07070E),
            topLeft = Offset(size.width * 0.49f, size.height * 0.49f),
            size = Size(size.width * 0.07f, size.height * 0.04f)
        )
        drawLine(
            color = Color(0xFF0B0B12),
            start = Offset(size.width * 0.52f, size.height * 0.53f),
            end = Offset(size.width * 0.52f, size.height * 0.58f),
            strokeWidth = 1.4.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawCircle(Color(0xFF0E0E17), radius = size.width * 0.055f, center = Offset(size.width * 0.44f, size.height * 0.73f))
        drawCircle(Color(0xFF0E0E17), radius = size.width * 0.055f, center = Offset(size.width * 0.62f, size.height * 0.73f))
    }
}

@Composable
private fun PlayingBubble(
    audio: AudioItem?,
    variant: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.noRippleClickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(CircleShape)
                .border(5.dp, Color.White, CircleShape)
        ) {
            ArtworkPreview(audio = audio, variant = variant, modifier = Modifier.fillMaxSize())
        }
        Canvas(
            modifier = Modifier
                .width(54.dp)
                .height(32.dp)
                .offset(x = 36.dp, y = (-12).dp)
        ) {
            drawCircle(Color.White, radius = size.minDimension * 0.18f, center = Offset(size.width * 0.74f, size.height * 0.18f))
            drawCircle(Color.White, radius = size.minDimension * 0.32f, center = Offset(size.width * 0.48f, size.height * 0.72f))
        }
    }
}

@Composable
private fun ArtworkPreview(
    audio: AudioItem?,
    variant: Int,
    modifier: Modifier = Modifier
) {
    // 在三张图片之间轮换，由外部传入的 variant 控制
    val resId = when (((variant % 3) + 3) % 3) {
        0 -> R.drawable.bubble_artwork_1
        1 -> R.drawable.bubble_artwork_2
        else -> R.drawable.bubble_artwork_3
    }
    Image(
        painter = painterResource(id = resId),
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Crop
    )
}

@Composable
private fun HomeBottomArea(
    isSignalAnimating: Boolean,
    onOpenHome: () -> Unit,
    onOpenUploadLink: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        HomeVoiceIndicator(
            isAnimating = isSignalAnimating,
            modifier = Modifier
                .width(64.dp)
                .height(32.dp)
        )
        HomeModeTabs(
            selected = HomeMode.Home,
            onOpenHome = onOpenHome,
            onOpenUploadLink = onOpenUploadLink,
            onOpenLibrary = onOpenLibrary,
            onOpenChat = onOpenChat
        )
    }
}

@Composable
private fun NetworkSignalCard(
    isSignalAnimating: Boolean,
    isActivated: Boolean,
    statusLabel: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(176.dp)
            .clip(RoundedCornerShape(31.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF151D3D),
                        Color(0xFF111834),
                        Color(0xFF19204A)
                    )
                )
            )
            .padding(horizontal = 24.dp, vertical = 17.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(Color(0xFF3B425C).copy(alpha = 0.78f))
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
                PawSignal(
                    activeCount = rememberNetworkSignalLevel(),
                    totalCount = 4,
                    modifier = Modifier
                        .width(94.dp)
                        .height(24.dp)
                )
            Text(
                text = statusLabel,
                color = Color.White,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 18.dp)
        ) {
            if (isSignalAnimating) {
                AnimatedWaveform(
                    modifier = Modifier
                        .width(118.dp)
                        .height(44.dp)
                )
            } else {
                WaitingDots(
                    modifier = Modifier
                        .width(112.dp)
                        .height(22.dp)
                )
            }
        }
    }
}

@Composable
private fun rememberNetworkSignalLevel(): Int {
    val context = LocalContext.current
    var level by remember { mutableIntStateOf(0) }
    DisposableEffect(Unit) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        fun update() {
            val net = cm.activeNetwork
            val caps = if (net != null) cm.getNetworkCapabilities(net) else null
            level = when {
                caps == null -> 0
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    val signal: Int = caps.signalStrength
                    when {
                        signal >= -55 -> 4
                        signal >= -70 -> 3
                        signal >= -85 -> 2
                        else -> 1
                    }
                }
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    val signal: Int = caps.signalStrength
                    when {
                        signal >= -70 -> 4
                        signal >= -85 -> 3
                        signal >= -100 -> 2
                        else -> 1
                    }
                }
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> 2
                else -> 0
            }
        }
        update()
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = update()
            override fun onLost(network: Network) = update()
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = update()
        }
        val req = NetworkRequest.Builder().build()
        cm.registerNetworkCallback(req, callback)
        onDispose { cm.unregisterNetworkCallback(callback) }
    }
    return level
}

@Composable
private fun PawSignal(
    activeCount: Int,
    totalCount: Int = 4,
    color: Color = Color.White,
    inactiveAlpha: Float = 0.28f,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val count = totalCount.coerceAtLeast(1)
        val horizontalExtent = 0.83f
        val spacingRatio = 0.98f
        val scale = minOf(
            size.height * 0.90f,
            size.width / (horizontalExtent + (count - 1) * spacingRatio)
        )
        val centerSpacing = scale * spacingRatio
        val groupWidth = (count - 1) * centerSpacing + scale * horizontalExtent
        val startCenterX = ((size.width - groupWidth) / 2f) + scale * 0.41f
        val centerY = size.height / 2f + scale * 0.10f
        repeat(count) { index ->
            val alpha = if (index < activeCount) 1f else inactiveAlpha
            val center = Offset(startCenterX + centerSpacing * index, centerY)
            drawPawPrint(
                center = center,
                scale = scale,
                color = color.copy(alpha = alpha)
            )
        }
    }
}

private fun DrawScope.drawPawPrint(
    center: Offset,
    scale: Float,
    color: Color
) {
    drawOval(
        color = color,
        topLeft = Offset(center.x - scale * 0.24f, center.y - scale * 0.08f),
        size = Size(scale * 0.48f, scale * 0.36f)
    )
    drawCircle(color, radius = scale * 0.13f, center = Offset(center.x - scale * 0.28f, center.y - scale * 0.22f))
    drawCircle(color, radius = scale * 0.14f, center = Offset(center.x - scale * 0.09f, center.y - scale * 0.34f))
    drawCircle(color, radius = scale * 0.14f, center = Offset(center.x + scale * 0.10f, center.y - scale * 0.34f))
    drawCircle(color, radius = scale * 0.13f, center = Offset(center.x + scale * 0.29f, center.y - scale * 0.22f))
}

@Composable
private fun HomeVoiceIndicator(
    isAnimating: Boolean,
    modifier: Modifier = Modifier
) {
    if (isAnimating) {
        AnimatedWaveform(
            bars = 5,
            modifier = modifier
        )
    } else {
        WaitingDots(
            dots = 5,
            modifier = modifier
        )
    }
}

@Composable
private fun WaitingDots(modifier: Modifier = Modifier) {
    WaitingDots(dots = 9, modifier = modifier)
}

@Composable
private fun WaitingDots(
    dots: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val dotSize = (size.height * 0.36f).coerceAtMost(8.dp.toPx())
        val gap = (size.width - dotSize * dots) / (dots - 1)
        repeat(dots) { index ->
            drawCircle(
                color = Color(0xFFA69DFF),
                radius = dotSize / 2f,
                center = Offset(
                    x = dotSize / 2f + index * (dotSize + gap),
                    y = size.height / 2f
                )
            )
        }
    }
}

@Composable
private fun AnimatedWaveform(modifier: Modifier = Modifier) {
    AnimatedWaveform(bars = 9, modifier = modifier)
}

@Composable
private fun AnimatedWaveform(
    bars: Int,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "home-wave")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 920, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "home-wave-progress"
    )
    Canvas(modifier = modifier) {
        val barWidth = 5.dp.toPx()
        val gap = (size.width - bars * barWidth) / (bars - 1)
        repeat(bars) { index ->
            val wave = (sin((progress * 2f * PI) + index * 0.82f).toFloat() + 1f) / 2f
            val height = size.height * (0.28f + wave * 0.58f)
            val x = index * (barWidth + gap)
            drawRoundRect(
                color = Color(0xFFA79BFF),
                topLeft = Offset(x, (size.height - height) / 2f),
                size = Size(barWidth, height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth, barWidth)
            )
        }
    }
}

private enum class HomeMode {
    Tv,
    Home,
    Explore,
    Chat
}

@Composable
private fun HomeModeTabs(
    selected: HomeMode,
    onOpenHome: () -> Unit,
    onOpenUploadLink: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenChat: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HomeModePill(
            label = "Tv",
            icon = LinkTagIcon.Tv,
            selected = selected == HomeMode.Tv,
            onClick = onOpenUploadLink,
            modifier = Modifier.weight(1f)
        )
        HomeModePill(
            label = "Home",
            icon = LinkTagIcon.Volume,
            selected = selected == HomeMode.Home,
            onClick = onOpenHome,
            modifier = Modifier.weight(1f)
        )
        HomeModePill(
            label = "Explore",
            icon = LinkTagIcon.Explore,
            selected = selected == HomeMode.Explore,
            onClick = onOpenLibrary,
            modifier = Modifier.weight(1.22f)
        )
        HomeModePill(
            label = "chat",
            icon = LinkTagIcon.Anxious,
            selected = selected == HomeMode.Chat,
            onClick = onOpenChat,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun HomeModePill(
    label: String,
    icon: LinkTagIcon,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val labelSize = 12.sp

    Row(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(23.dp))
            .border(
                width = 1.dp,
                color = if (selected) Color.White.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.20f),
                shape = RoundedCornerShape(23.dp)
            )
            .background(
                if (selected) {
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF151C36), Color(0xFF8279FF))
                    )
                } else {
                    SolidColor(Color(0xFF8279FF).copy(alpha = 0.06f))
                }
            )
            .noRippleClickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        HomeModeIcon(icon = icon, selected = selected)
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = label,
            color = if (selected) Color.White else Color(0xFFC9CAD8),
            fontSize = labelSize,
            lineHeight = labelSize,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HomeModeIcon(icon: LinkTagIcon, selected: Boolean) {
    when (icon) {
        LinkTagIcon.Volume -> {
            OptionalDrawable(
                name = "home_volume_icon",
                contentDescription = "Home",
                modifier = Modifier.size(19.dp),
                contentScale = ContentScale.Fit
            ) {
                LinkTagGlyph(icon = icon, selected = selected)
            }
        }

        LinkTagIcon.Anxious -> {
            Box(
                modifier = Modifier.size(20.dp),
                contentAlignment = Alignment.Center
            ) {
                OptionalDrawable(
                    name = "home_chat_bubble",
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                ) {
                    LinkTagGlyph(icon = icon, selected = selected)
                }
                OptionalDrawable(
                    name = "home_chat_heart",
                    contentDescription = "Chat",
                    modifier = Modifier.size(9.dp),
                    contentScale = ContentScale.Fit
                ) {
                    Canvas(modifier = Modifier.size(9.dp)) {
                        val color = if (selected) Color.White else Color(0xFFC8CADD)
                        val heart = Path().apply {
                            moveTo(size.width * 0.50f, size.height * 0.76f)
                            cubicTo(size.width * 0.18f, size.height * 0.53f, size.width * 0.20f, size.height * 0.20f, size.width * 0.46f, size.height * 0.33f)
                            cubicTo(size.width * 0.63f, size.height * 0.14f, size.width * 0.90f, size.height * 0.39f, size.width * 0.50f, size.height * 0.76f)
                        }
                        drawPath(heart, color = color)
                    }
                }
            }
        }

        else -> LinkTagGlyph(icon = icon, selected = selected)
    }
}

@Composable
private fun HeaderBlock(state: FloppyUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "晚上好",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "预计 ${state.settings.profile.bedtime} 入睡，Floppy 会把节奏放慢。",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AgentPanel(
    state: FloppyUiState,
    onToggleListening: () -> Unit,
    onRecommend: () -> Unit,
    onGenerate: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FloppyAvatar(agentState = state.agentState)
            Text(
                text = agentStatusText(state),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            state.generationMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onToggleListening) {
                    Text(if (state.isListening) "结束轻触" else "轻触 Floppy")
                }
                Button(
                    onClick = onRecommend,
                    enabled = state.agentState !in setOf(AgentState.Recommending, AgentState.Generating)
                ) {
                    Text("推荐今晚")
                }
            }
            TextButton(
                onClick = onGenerate,
                enabled = state.agentState != AgentState.Generating
            ) {
                Text("直接生成 1 分钟试听")
            }
        }
    }
}

@Composable
private fun FloppyAvatar(agentState: AgentState) {
    val bodyColor = when (agentState) {
        AgentState.Failed -> Color(0xFF8F3F3A)
        AgentState.Playing -> Color(0xFF31668A)
        AgentState.Generating, AgentState.Recommending -> Color(0xFF8A5A44)
        AgentState.Listening -> Color(0xFF24564A)
        else -> Color(0xFF23483B)
    }
    Canvas(
        modifier = Modifier
            .size(148.dp)
            .clip(CircleShape)
            .background(Color(0xFFE7E2D5))
    ) {
        drawFloppy(bodyColor)
    }
}

private fun DrawScope.drawFloppy(bodyColor: Color) {
    drawOval(
        color = bodyColor,
        topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.18f, size.height * 0.14f),
        size = androidx.compose.ui.geometry.Size(size.width * 0.64f, size.height * 0.72f)
    )
    drawCircle(Color(0xFFFFFCF7), radius = size.width * 0.07f, center = center.copy(x = size.width * 0.42f, y = size.height * 0.46f))
    drawCircle(Color(0xFFFFFCF7), radius = size.width * 0.07f, center = center.copy(x = size.width * 0.58f, y = size.height * 0.46f))
    val smile = Path().apply {
        moveTo(size.width * 0.41f, size.height * 0.62f)
        quadraticTo(size.width * 0.50f, size.height * 0.70f, size.width * 0.59f, size.height * 0.62f)
    }
    drawPath(smile, color = Color(0xFFFFFCF7), style = Stroke(width = 5f))
}

@Composable
private fun PlayerPanel(
    audio: AudioItem,
    state: FloppyUiState,
    onPlay: () -> Unit,
    onPauseOrResume: () -> Unit,
    onFeedback: (Int, String?) -> Unit
) {
    var rating by remember(audio.id) { mutableFloatStateOf(4f) }
    val playback = state.playback
    val progress = if (playback.durationMs > 0) {
        (playback.positionMs.toFloat() / playback.durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFCF7)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(audio.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(audio.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(formatTime(playback.positionMs / 1000), color = MaterialTheme.colorScheme.onSurfaceVariant)
                val totalSeconds = (playback.durationMs / 1000).takeIf { it > 0 } ?: audio.durationSeconds.toLong()
                Text(formatTime(totalSeconds), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = if (playback.currentAudio?.id == audio.id) onPauseOrResume else onPlay) {
                    Text(
                        when (playback.state) {
                            PlaybackState.Playing,
                            PlaybackState.Buffering -> "暂停"

                            PlaybackState.Paused -> "继续"
                            PlaybackState.Ended -> "重播"
                            PlaybackState.Failed -> "重试"
                            PlaybackState.Idle -> "播放"
                        }
                    )
                }
                OutlinedButton(onClick = onPlay) {
                    Text("从头开始")
                }
            }
            HorizontalDivider()
            Text("试听反馈", fontWeight = FontWeight.SemiBold)
            Slider(
                value = rating,
                onValueChange = { rating = it },
                valueRange = 1f..5f,
                steps = 3
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${rating.roundToInt()} 分")
                Button(onClick = { onFeedback(rating.roundToInt(), null) }) {
                    Text("提交反馈")
                }
            }
        }
    }
}

@Composable
private fun LinkUploadScreen(
    onOpenHome: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenChat: () -> Unit
) {
    var uploadState by remember { mutableStateOf(LinkUploadState.Waiting) }
    var selectedUploadName by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedUploadPath by rememberSaveable { mutableStateOf<String?>(null) }

    fun resetUpload() {
        uploadState = LinkUploadState.Waiting
        selectedUploadName = null
        selectedUploadPath = null
    }

    BackHandler(enabled = uploadState != LinkUploadState.Waiting) {
        when (uploadState) {
            LinkUploadState.Pasting -> uploadState = LinkUploadState.Waiting
            LinkUploadState.Identifying,
            LinkUploadState.Ready,
            LinkUploadState.Playing -> resetUpload()
            LinkUploadState.Waiting -> Unit
        }
    }

    LaunchedEffect(uploadState) {
        when (uploadState) {
            LinkUploadState.Identifying -> {
                delay(1400)
                uploadState = LinkUploadState.Ready
            }

            else -> Unit
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LinkUploadBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.statusBars.asPaddingValues())
                .padding(horizontal = 26.dp)
                .padding(top = 12.dp, bottom = 28.dp)
        ) {
            LinkUploadHeader(
                uploadName = selectedUploadName,
                uploadPath = selectedUploadPath,
                onSettingsClick = onOpenSettings
            )
            Spacer(modifier = Modifier.height(30.dp))
            if (uploadState == LinkUploadState.Pasting) {
                LinkPastePanel(
                    onBack = { uploadState = LinkUploadState.Waiting },
                    onPlay = { link ->
                        val normalizedLink = link.cleanVideoLinkInput()
                        selectedUploadName = "Video link"
                        selectedUploadPath = normalizedLink
                        uploadState = LinkUploadState.Ready
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            } else {
                LinkUploadStage(
                    state = uploadState,
                    videoLink = selectedUploadPath,
                    onUploadClick = {
                        if (uploadState == LinkUploadState.Waiting) {
                            uploadState = LinkUploadState.Pasting
                        }
                    },
                    onVideoClick = {
                        uploadState = when (uploadState) {
                            LinkUploadState.Ready -> LinkUploadState.Playing
                            LinkUploadState.Playing -> LinkUploadState.Ready
                            else -> uploadState
                        }
                    },
                    onCloseVideo = {
                        resetUpload()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            LinkUploadTags(
                onOpenHome = onOpenHome,
                onOpenUploadLink = {},
                onOpenLibrary = onOpenLibrary,
                onOpenChat = onOpenChat
            )
        }
    }
}

private enum class LinkUploadState {
    Waiting,
    Pasting,
    Identifying,
    Ready,
    Playing
}

private val LinkUploadBackground = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF101936),
        Color(0xFF111633),
        Color(0xFF050914)
    )
)

private val LinkStageBackground = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF101837),
        Color(0xFF070C1D),
        Color(0xFF756BF2)
    )
)

@Composable
private fun LinkUploadHeader(
    uploadName: String?,
    uploadPath: String?,
    onSettingsClick: () -> Unit
) {
    val deviceTime = rememberDeviceUploadTime()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OptionalDrawable(
                name = "home_floppy_logo",
                contentDescription = "Floppy",
                modifier = Modifier
                    .width(98.dp)
                    .height(29.dp),
                contentScale = ContentScale.Fit
            ) {
                Text(
                    text = "Floppy",
                    color = Color(0xFFF4F3FF),
                    fontSize = 29.sp,
                    lineHeight = 29.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = deviceTime,
                    color = Color(0xFFA9ADC7),
                    fontSize = 15.sp
                )
                Text(
                    text = uploadName ?: "Paste your link here...",
                    color = Color.White,
                    fontSize = 30.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = uploadPath ?: "tiktok/bilibili/kuaishou",
                    color = Color(0xFFA9ADC7),
                    fontSize = 17.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        FloppyCloudButton(
            onClick = onSettingsClick,
            modifier = Modifier.size(36.dp)
        )
    }
}

@Composable
private fun rememberDeviceUploadTime(): String {
    val context = LocalContext.current
    var currentTime by remember { mutableStateOf(formatDeviceUploadTime(context)) }

    LaunchedEffect(context) {
        while (true) {
            currentTime = formatDeviceUploadTime(context)
            delay(30_000)
        }
    }

    return currentTime
}

private fun formatDeviceUploadTime(context: Context): String {
    val locale = Locale.getDefault()
    val timePattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
    val formatter = DateTimeFormatter.ofPattern("EEEE · $timePattern", locale)
    return LocalDateTime.now().format(formatter).uppercase(locale)
}

@Composable
private fun LinkPastePanel(
    onBack: () -> Unit,
    onPlay: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var link by rememberSaveable { mutableStateOf("") }
    val canSubmit = link.isNotBlank()

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .background(Color(0xFF101837).copy(alpha = 0.96f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(26.dp))
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1B2442))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                BackArrowGlyph()
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = "Paste video link",
                color = Color.White,
                fontSize = 24.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Bold
            )
        }
        BasicTextField(
            value = link,
            onValueChange = { link = it },
            textStyle = TextStyle(
                color = Color.White,
                fontSize = 16.sp,
                lineHeight = 22.sp
            ),
            cursorBrush = SolidColor(ResearchPurple),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .heightIn(min = 150.dp)
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, Color(0xFF756BF2).copy(alpha = 0.78f), RoundedCornerShape(18.dp))
                .background(Color(0xFF070C1D).copy(alpha = 0.82f))
                .padding(16.dp),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.fillMaxSize()) {
                    if (link.isBlank()) {
                        Text(
                            text = "https://www.tiktok.com/...\nhttps://www.bilibili.com/...\nhttps://www.kuaishou.com/...",
                            color = Color(0xFFA9ADC7),
                            fontSize = 15.sp,
                            lineHeight = 23.sp
                        )
                    }
                    innerTextField()
                }
            }
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(27.dp))
                .background(if (canSubmit) ResearchPurple else Color(0xFF303752))
                .clickable(enabled = canSubmit) { onPlay(link) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Play video",
                color = Color.White.copy(alpha = if (canSubmit) 1f else 0.52f),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun LinkUploadStage(
    state: LinkUploadState,
    videoLink: String?,
    onUploadClick: () -> Unit,
    onVideoClick: () -> Unit,
    onCloseVideo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasVideo = state in setOf(LinkUploadState.Ready, LinkUploadState.Playing)
    val showThoughtFood = state == LinkUploadState.Waiting
    val showEmptyThought = state == LinkUploadState.Identifying
    var showPlaybackControl by remember { mutableStateOf(false) }

    LaunchedEffect(hasVideo, videoLink) {
        showPlaybackControl = hasVideo
    }

    LaunchedEffect(state) {
        if (state == LinkUploadState.Ready) {
            showPlaybackControl = true
        }
    }

    LaunchedEffect(state, showPlaybackControl) {
        if (state == LinkUploadState.Playing && showPlaybackControl) {
            delay(3_000)
            showPlaybackControl = false
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .background(LinkStageBackground)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF172348).copy(alpha = 0.72f),
                            Color.Transparent
                        ),
                        center = Offset(120f, 120f),
                        radius = 560f
                    )
                )
        )
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val stageWidth = maxWidth
            val stageHeight = maxHeight
            val tvWidth = stageWidth * if (hasVideo) 0.70f else 0.68f
            val tvTop = stageHeight * 0.15f
            val dogWidth = stageWidth * 0.435f

            TvFrame(
                state = state,
                hasVideo = hasVideo,
                videoLink = videoLink,
                onUploadClick = onUploadClick,
                showPlaybackControl = showPlaybackControl,
                onVideoClick = {
                    if (state == LinkUploadState.Playing && !showPlaybackControl) {
                        showPlaybackControl = true
                    } else {
                        onVideoClick()
                    }
                },
                modifier = Modifier
                    .width(tvWidth)
                    .align(Alignment.TopCenter)
                    .offset(y = tvTop)
            )
            if (hasVideo) {
                CloseVideoButton(
                    onClick = onCloseVideo,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(x = tvWidth * 0.48f, y = tvTop - 12.dp)
                )
            }
            SleepingDog(
                modifier = Modifier
                    .width(dogWidth)
                    .aspectRatio(0.82f)
                    .align(Alignment.BottomStart)
                    .offset(x = stageWidth * 0.08f, y = -stageHeight * 0.03f)
            )
            when {
                showThoughtFood -> FoodThoughtBubble(
                    modifier = Modifier
                        .width(stageWidth * 0.38f)
                        .align(Alignment.CenterEnd)
                        .offset(x = -stageWidth * 0.03f, y = stageHeight * 0.03f)
                )

                showEmptyThought -> EmptyThoughtBubble(
                    modifier = Modifier
                        .width(stageWidth * 0.37f)
                        .align(Alignment.CenterEnd)
                        .offset(x = -stageWidth * 0.03f, y = stageHeight * 0.08f)
                )
            }
        }
    }
}

@Composable
private fun TvFrame(
    state: LinkUploadState,
    hasVideo: Boolean,
    videoLink: String?,
    onUploadClick: () -> Unit,
    showPlaybackControl: Boolean,
    onVideoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.aspectRatio(1.45f)) {
        OptionalDrawable(
            name = "upload_link_tv_frame",
            contentDescription = "TV frame",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val screenTop = size.height * 0.04f
                val screenHeight = size.height * 0.82f
                drawRect(
                    color = Color(0xFF39446D).copy(alpha = 0.72f),
                    topLeft = Offset(size.width * 0.01f, screenTop + size.height * 0.02f),
                    size = Size(size.width * 0.98f, screenHeight)
                )
                drawRect(
                    color = Color(0xFF222C4E),
                    topLeft = Offset(size.width * 0.03f, screenTop),
                    size = Size(size.width * 0.94f, screenHeight)
                )
                drawRect(
                    color = Color(0xFF11182D),
                    topLeft = Offset(size.width * 0.05f, screenTop + size.height * 0.03f),
                    size = Size(size.width * 0.90f, screenHeight * 0.84f)
                )
                drawRoundRect(
                    color = Color(0xFF18213B).copy(alpha = 0.58f),
                    topLeft = Offset(size.width * 0.32f, size.height * 0.85f),
                    size = Size(size.width * 0.36f, size.height * 0.05f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
                drawRoundRect(
                    color = Color(0xFF1D2747),
                    topLeft = Offset(size.width * 0.16f, size.height * 0.90f),
                    size = Size(size.width * 0.68f, size.height * 0.035f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .fillMaxSize(0.76f)
                .align(Alignment.TopCenter)
                .offset(y = 8.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(if (hasVideo) Color.Transparent else Color(0xFF0F0078)),
            contentAlignment = Alignment.Center
        ) {
            if (hasVideo) {
                if (!videoLink.isNullOrBlank()) {
                    LinkMediaPlaybackWebView(
                        link = videoLink,
                        isPlaying = state == LinkUploadState.Playing,
                        modifier = Modifier
                            .size(1.dp)
                            .graphicsLayer(alpha = 0.01f)
                    )
                }
                VideoThumbnail(modifier = Modifier.fillMaxSize())
                if (showPlaybackControl) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(onClick = onVideoClick),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state == LinkUploadState.Playing) {
                            PauseCircle(modifier = Modifier.size(48.dp))
                        } else {
                            PlayCircle(modifier = Modifier.size(48.dp))
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(onClick = onVideoClick)
                    )
                }
            } else {
                when (state) {
                    LinkUploadState.Identifying -> IdentifyingContent()
                    else -> UploadContent()
                }
                if (state == LinkUploadState.Waiting) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(onClick = onUploadClick)
                    )
                }
            }
        }
    }
}

@Composable
private fun UploadContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Canvas(modifier = Modifier.size(34.dp)) {
            drawLine(
                color = Color.White,
                start = Offset(size.width / 2, size.height * 0.08f),
                end = Offset(size.width / 2, size.height * 0.62f),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color.White,
                start = Offset(size.width * 0.30f, size.height * 0.30f),
                end = Offset(size.width / 2, size.height * 0.08f),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color.White,
                start = Offset(size.width * 0.70f, size.height * 0.30f),
                end = Offset(size.width / 2, size.height * 0.08f),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(size.width * 0.18f, size.height * 0.58f),
                size = Size(size.width * 0.64f, size.height * 0.28f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                style = Stroke(width = 3.dp.toPx())
            )
        }
        Text("Click here to paste link", color = Color.White, fontSize = 17.sp)
    }
}

@Composable
private fun IdentifyingContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressGlyph()
        Text("Identifying...", color = Color.White, fontSize = 17.sp)
    }
}

@Composable
private fun CircularProgressGlyph() {
    Canvas(modifier = Modifier.size(48.dp)) {
        drawCircle(
            color = Color.White.copy(alpha = 0.35f),
            radius = size.minDimension * 0.34f,
            style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
        )
        drawArc(
            color = Color.White,
            startAngle = -90f,
            sweepAngle = 110f,
            useCenter = false,
            topLeft = Offset(size.width * 0.16f, size.height * 0.16f),
            size = Size(size.width * 0.68f, size.height * 0.68f),
            style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun VideoThumbnail(modifier: Modifier = Modifier) {
    OptionalDrawable(
        name = "upload_link_video_thumbnail",
        contentDescription = "Uploaded video thumbnail",
        modifier = modifier
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF143F4B),
                        Color(0xFF101327),
                        Color(0xFFB45232)
                    ),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                )
            )
            drawRoundRect(
                color = Color(0xFFEAA21B),
                topLeft = Offset(size.width * 0.55f, size.height * 0.08f),
                size = Size(size.width * 0.22f, size.height * 0.84f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx(), 16.dp.toPx())
            )
            drawCircle(
                color = Color(0xFFF4C2A0),
                radius = size.width * 0.08f,
                center = Offset(size.width * 0.54f, size.height * 0.30f)
            )
            drawRoundRect(
                color = Color(0xFFF28F37),
                topLeft = Offset(size.width * 0.46f, size.height * 0.38f),
                size = Size(size.width * 0.18f, size.height * 0.42f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx(), 10.dp.toPx())
            )
            drawCircle(
                color = Color(0xFF101010),
                radius = size.width * 0.07f,
                center = Offset(size.width * 0.56f, size.height * 0.26f)
            )
        }
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "소희",
                color = Color(0xFFFFF033),
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
            )
            Text(
                text = "헷주",
                color = Color(0xFFFFF033),
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun LinkMediaPlaybackWebView(
    link: String,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val normalizedLink = remember(link) { link.normalizedVideoUrl() }
    val webViewHolder = remember { arrayOfNulls<WebView>(1) }
    val playbackScript = remember(isPlaying) {
        if (isPlaying) {
            """
                (function() {
                    var media = document.querySelector('audio, video');
                    if (media) {
                        media.play();
                    }
                })();
            """.trimIndent()
        } else {
            """
                (function() {
                    var media = document.querySelector('audio, video');
                    if (media) {
                        media.pause();
                    }
                })();
            """.trimIndent()
        }
    }
    val latestPlaybackScript by rememberUpdatedState(playbackScript)

    DisposableEffect(Unit) {
        onDispose {
            webViewHolder[0]?.stopLoading()
            webViewHolder[0]?.destroy()
            webViewHolder[0] = null
        }
    }

    AndroidView(
        modifier = modifier.background(Color.Black),
        factory = { context ->
            WebView(context).apply {
                webViewHolder[0] = this
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String?) {
                        view.evaluateJavascript(latestPlaybackScript, null)
                    }
                }
                webChromeClient = WebChromeClient()
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                settings.javaScriptCanOpenWindowsAutomatically = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                loadUrl(normalizedLink)
            }
        },
        update = { webView ->
            if (webView.url != normalizedLink) {
                webView.loadUrl(normalizedLink)
            }
            webView.evaluateJavascript(latestPlaybackScript, null)
        }
    )
}

private fun String.normalizedVideoUrl(): String {
    val trimmed = trim()
    return if (
        trimmed.startsWith("http://", ignoreCase = true) ||
        trimmed.startsWith("https://", ignoreCase = true)
    ) {
        trimmed
    } else {
        "https://$trimmed"
    }
}

private fun String.cleanVideoLinkInput(): String {
    return lineSequence()
        .map { it.trim() }
        .firstOrNull { it.isNotEmpty() }
        ?: trim()
}

@Composable
private fun OptionalDrawable(
    name: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallback: @Composable () -> Unit
) {
    val context = LocalContext.current
    val resourceId = remember(name) {
        context.resources.getIdentifier(name, "drawable", context.packageName)
    }
    if (resourceId != 0) {
        Image(
            painter = painterResource(resourceId),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        Box(modifier = modifier) {
            fallback()
        }
    }
}

@Composable
private fun SleepingDog(modifier: Modifier = Modifier) {
    OptionalDrawable(
        name = "upload_link_floppy_dog",
        contentDescription = "Floppy watching the screen",
        modifier = modifier
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val shadowCenter = Offset(size.width * 0.54f, size.height * 0.87f)
            drawOval(
                color = Color.Black.copy(alpha = 0.88f),
                topLeft = Offset(shadowCenter.x - size.width * 0.44f, shadowCenter.y - size.height * 0.06f),
                size = Size(size.width * 0.88f, size.height * 0.12f)
            )
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF49495D),
                        Color(0xFF161722),
                        Color(0xFF03040A)
                    ),
                    center = Offset(size.width * 0.48f, size.height * 0.34f),
                    radius = size.width * 0.45f
                ),
                topLeft = Offset(size.width * 0.22f, size.height * 0.16f),
                size = Size(size.width * 0.48f, size.height * 0.44f)
            )
            drawOval(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF6C6B8B), Color(0xFF11121B)),
                    start = Offset(size.width * 0.15f, size.height * 0.16f),
                    end = Offset(size.width * 0.04f, size.height * 0.50f)
                ),
                topLeft = Offset(size.width * 0.02f, size.height * 0.17f),
                size = Size(size.width * 0.22f, size.height * 0.38f)
            )
            drawOval(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF6C6B8B), Color(0xFF11121B)),
                    start = Offset(size.width * 0.62f, size.height * 0.16f),
                    end = Offset(size.width * 0.83f, size.height * 0.52f)
                ),
                topLeft = Offset(size.width * 0.66f, size.height * 0.17f),
                size = Size(size.width * 0.22f, size.height * 0.38f)
            )
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF8E83FF), Color(0xFF342D79)),
                    start = Offset(size.width * 0.25f, size.height * 0.52f),
                    end = Offset(size.width * 0.68f, size.height * 0.62f)
                ),
                topLeft = Offset(size.width * 0.28f, size.height * 0.51f),
                size = Size(size.width * 0.36f, size.height * 0.09f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx(), 20.dp.toPx())
            )
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF343541), Color(0xFF0B0C12)),
                    center = Offset(size.width * 0.45f, size.height * 0.64f),
                    radius = size.width * 0.36f
                ),
                topLeft = Offset(size.width * 0.24f, size.height * 0.56f),
                size = Size(size.width * 0.42f, size.height * 0.30f)
            )
            drawOval(
                color = Color(0xFF0E0F15),
                topLeft = Offset(size.width * 0.32f, size.height * 0.72f),
                size = Size(size.width * 0.13f, size.height * 0.16f)
            )
            drawOval(
                color = Color(0xFF0E0F15),
                topLeft = Offset(size.width * 0.49f, size.height * 0.72f),
                size = Size(size.width * 0.13f, size.height * 0.16f)
            )
        }
    }
}

@Composable
private fun FoodThoughtBubble(modifier: Modifier = Modifier) {
    OptionalDrawable(
        name = "upload_link_food_bubble",
        contentDescription = "Food thought bubble",
        modifier = modifier.aspectRatio(1.28f),
        contentScale = ContentScale.Fit
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(Color.White, radius = size.width * 0.07f, center = Offset(size.width * 0.08f, size.height * 0.62f))
                drawCircle(Color.White, radius = size.width * 0.11f, center = Offset(size.width * 0.16f, size.height * 0.80f))
                drawOval(
                    color = Color.White,
                    topLeft = Offset(size.width * 0.18f, size.height * 0.10f),
                    size = Size(size.width * 0.80f, size.height * 0.68f)
                )
            }
            DogBowl(modifier = Modifier.align(Alignment.Center).fillMaxWidth(0.52f))
        }
    }
}

@Composable
private fun EmptyThoughtBubble(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.aspectRatio(1.24f)) {
        drawCircle(Color.White, radius = size.width * 0.07f, center = Offset(size.width * 0.08f, size.height * 0.70f))
        drawCircle(Color.White, radius = size.width * 0.11f, center = Offset(size.width * 0.16f, size.height * 0.88f))
        drawOval(
            color = Color.White,
            topLeft = Offset(size.width * 0.18f, size.height * 0.08f),
            size = Size(size.width * 0.80f, size.height * 0.68f)
        )
    }
}

@Composable
private fun DogBowl(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.aspectRatio(1.35f)) {
        drawOval(
            color = Color(0xFF5448B7),
            topLeft = Offset(size.width * 0.05f, size.height * 0.50f),
            size = Size(size.width * 0.90f, size.height * 0.34f)
        )
        drawRoundRect(
            color = Color(0xFF1E2438),
            topLeft = Offset(size.width * 0.18f, size.height * 0.28f),
            size = Size(size.width * 0.64f, size.height * 0.42f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx(), 18.dp.toPx())
        )
        repeat(8) { index ->
            val x = size.width * (0.30f + (index % 4) * 0.12f)
            val y = size.height * (0.36f + (index / 4) * 0.12f)
            drawCircle(Color(0xFFA56539), radius = size.width * 0.055f, center = Offset(x, y))
        }
        drawRoundRect(
            color = Color(0xFF8F83FF),
            topLeft = Offset(size.width * 0.32f, size.height * 0.59f),
            size = Size(size.width * 0.36f, size.height * 0.12f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx(), 10.dp.toPx())
        )
        drawCircle(Color(0xFF8F83FF), radius = size.width * 0.07f, center = Offset(size.width * 0.31f, size.height * 0.65f))
        drawCircle(Color(0xFF8F83FF), radius = size.width * 0.07f, center = Offset(size.width * 0.69f, size.height * 0.65f))
    }
}

@Composable
private fun CloseVideoButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(17.dp)
            .clip(CircleShape)
            .background(Color(0xFF1E2948))
            .border(1.dp, Color.White, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(9.dp)) {
            drawLine(Color.White, Offset.Zero, Offset(size.width, size.height), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
            drawLine(Color.White, Offset(size.width, 0f), Offset(0f, size.height), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
        }
    }
}

@Composable
private fun PauseCircle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0xFF202543).copy(alpha = 0.94f)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(15.dp)) {
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(0f, 0f),
                size = Size(size.width * 0.32f, size.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.4.dp.toPx(), 1.4.dp.toPx())
            )
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(size.width * 0.68f, 0f),
                size = Size(size.width * 0.32f, size.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.4.dp.toPx(), 1.4.dp.toPx())
            )
        }
    }
}

@Composable
private fun LinkUploadTags(
    onOpenHome: () -> Unit,
    onOpenUploadLink: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenChat: () -> Unit
) {
    HomeModeTabs(
        selected = HomeMode.Tv,
        onOpenHome = onOpenHome,
        onOpenUploadLink = onOpenUploadLink,
        onOpenLibrary = onOpenLibrary,
        onOpenChat = onOpenChat
    )
}

private enum class LinkTagIcon {
    Tv,
    Volume,
    Explore,
    Anxious
}

@Composable
private fun LinkTagGlyph(icon: LinkTagIcon, selected: Boolean) {
    val color = if (selected) Color.White else Color(0xFFC8CADD)
    Canvas(modifier = Modifier.size(22.dp)) {
        val strokeWidth = 1.4.dp.toPx()
        when (icon) {
            LinkTagIcon.Tv -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(size.width * 0.12f, size.height * 0.18f),
                    size = Size(size.width * 0.76f, size.height * 0.56f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                    style = Stroke(width = strokeWidth)
                )
                val path = Path().apply {
                    moveTo(size.width * 0.43f, size.height * 0.32f)
                    lineTo(size.width * 0.43f, size.height * 0.60f)
                    lineTo(size.width * 0.66f, size.height * 0.46f)
                    close()
                }
                drawPath(path, color = color)
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.34f, size.height * 0.84f),
                    end = Offset(size.width * 0.66f, size.height * 0.84f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }

            LinkTagIcon.Volume -> {
                repeat(3) { index ->
                    val x = size.width * (0.22f + index * 0.20f)
                    drawLine(
                        color = color,
                        start = Offset(x, size.height * (0.70f - index * 0.10f)),
                        end = Offset(x, size.height * (0.30f + index * 0.10f)),
                        strokeWidth = 1.6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
                drawArc(
                    color = color,
                    startAngle = -40f,
                    sweepAngle = 80f,
                    useCenter = false,
                    topLeft = Offset(size.width * 0.58f, size.height * 0.24f),
                    size = Size(size.width * 0.28f, size.height * 0.52f),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            LinkTagIcon.Explore -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(size.width * 0.16f, size.height * 0.15f),
                    size = Size(size.width * 0.56f, size.height * 0.62f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                    style = Stroke(width = strokeWidth)
                )
                drawCircle(
                    color = color,
                    radius = size.width * 0.14f,
                    center = Offset(size.width * 0.66f, size.height * 0.66f),
                    style = Stroke(width = strokeWidth)
                )
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.76f, size.height * 0.76f),
                    end = Offset(size.width * 0.86f, size.height * 0.86f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }

            LinkTagIcon.Anxious -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(size.width * 0.12f, size.height * 0.14f),
                    size = Size(size.width * 0.76f, size.height * 0.66f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    style = Stroke(width = strokeWidth)
                )
                val heart = Path().apply {
                    moveTo(size.width * 0.50f, size.height * 0.64f)
                    cubicTo(size.width * 0.24f, size.height * 0.46f, size.width * 0.30f, size.height * 0.28f, size.width * 0.47f, size.height * 0.38f)
                    cubicTo(size.width * 0.60f, size.height * 0.25f, size.width * 0.78f, size.height * 0.42f, size.width * 0.50f, size.height * 0.64f)
                }
                drawPath(heart, color = color)
            }
        }
    }
}

private enum class AudioLibraryTab(val label: String) {
    Library("Library"),
    Uploads("Uploads"),
    History("History")
}

private fun exploreMockLibrary(): AudioLibrary {
    val calm = FallbackAudioLibrary.audio()
    val friday = calm.copy(
        id = "mock-friday-fever",
        title = "Friday Fever",
        category = "Metal music",
        playbackProgress = 0.85f,
        artwork = AudioArtwork(
            seedColor = 0xFFFF563F,
            prompt = "red cinematic Friday fever album cover"
        )
    )
    val night = calm.copy(
        id = "mock-night-drive",
        title = "Night Drive",
        category = "Metal music",
        playbackProgress = 0.90f,
        artwork = AudioArtwork(
            seedColor = 0xFF00A7FF,
            prompt = "blue chrome sculpture in motion"
        )
    )
    val solar = calm.copy(
        id = "mock-solar-rest",
        title = "Solar Rest",
        category = "Absolute music",
        playbackProgress = 0.75f,
        artwork = AudioArtwork(
            seedColor = 0xFFFF7A1A,
            prompt = "orange sunset silhouette and quiet dream"
        )
    )
    val pixel = calm.copy(
        id = "mock-pixel-bloom",
        title = "Pixel Bloom",
        category = "Absolute music",
        playbackProgress = 0.55f,
        artwork = AudioArtwork(
            seedColor = 0xFFFFB86C,
            prompt = "pixel tree growing from a calm head"
        )
    )
    val uploads = listOf(
        UploadItem(
            id = "mock-upload-relaxing-podcast",
            fileName = "relaxing-podcast.mp3",
            fileType = "mp3",
            sizeLabel = "753 KB",
            progress = 1f,
            status = UploadStatus.Completed,
            generatedAudio = calm.copy(id = "mock-upload-audio-relaxing", source = AudioSource.Upload)
        ),
        UploadItem(
            id = "mock-upload-sleep-note",
            fileName = "sleep-note.txt",
            fileType = "txt",
            sizeLabel = "42 KB",
            progress = 1f,
            status = UploadStatus.Completed,
            generatedAudio = night.copy(id = "mock-upload-audio-note", source = AudioSource.Upload, category = "My upload")
        ),
        UploadItem(
            id = "mock-upload-book",
            fileName = "bedtime-book.pdf",
            fileType = "pdf",
            sizeLabel = "2.4 MB",
            progress = 0.68f,
            status = UploadStatus.Failed,
            message = "Upload failed - The file is too large"
        )
    )
    return AudioLibrary(
        recommended = listOf(calm, friday, night, solar, pixel),
        uploads = uploads,
        history = listOf(
            night.copy(playbackProgress = 0.90f),
            calm.copy(playbackProgress = 0.70f),
            solar.copy(playbackProgress = 0.45f),
            uploads.first().generatedAudio!!.copy(playbackProgress = 0.62f)
        )
    )
}

@Composable
private fun AudioLibraryScreen(
    state: FloppyUiState,
    onBack: () -> Unit,
    onStartUpload: (Uri, String, String, String?) -> Unit,
    onRetryUpload: (String) -> Unit,
    onCompleteUpload: (String) -> Unit,
    onPlay: (AudioItem) -> Unit,
    onPauseOrResume: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(AudioLibraryTab.Library) }
    val mockLibrary = remember { exploreMockLibrary() }
    val uploadPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val uploadFile = context.toUploadFileInfo(uri)
            onStartUpload(uri, uploadFile.fileName, uploadFile.fileType, uploadFile.mimeType)
            selectedTab = AudioLibraryTab.Uploads
        }
    }
    val openUploadPicker = {
        uploadPicker.launch(arrayOf("audio/*", "application/pdf", "text/*", "text/plain"))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        SharedPageBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.statusBars.asPaddingValues())
                .padding(horizontal = 22.dp)
                .padding(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            AudioLibraryTopBar(
                onBack = onBack,
                onUpload = openUploadPicker
            )
            AudioLibraryTabs(
                selected = selectedTab,
                onSelected = { selectedTab = it }
            )
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    AudioLibraryTab.Library -> AudioLibraryContent(
                        library = mockLibrary,
                        activeAudio = state.activeAudio,
                        playback = state.playback,
                        onPlay = onPlay,
                        onPauseOrResume = onPauseOrResume
                    )

                    AudioLibraryTab.Uploads -> UploadsContent(
                        uploads = mockLibrary.uploads,
                        onStartUpload = openUploadPicker,
                        onRetryUpload = onRetryUpload,
                        onCompleteUpload = onCompleteUpload,
                        onPlay = onPlay,
                        playback = state.playback,
                        onPauseOrResume = onPauseOrResume
                    )

                    AudioLibraryTab.History -> HistoryContent(
                        history = mockLibrary.history,
                        playback = state.playback,
                        onPlay = onPlay,
                        onPauseOrResume = onPauseOrResume
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioLibraryTopBar(
    onBack: () -> Unit,
    onUpload: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFF111628))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            BackArrowGlyph()
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Audio",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Library · Uploads · History",
                color = Color(0xFFA7AEC7),
                fontSize = 11.sp,
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.size(44.dp))
    }
}

@Composable
private fun AudioLibraryTabs(
    selected: AudioLibraryTab,
    onSelected: (AudioLibraryTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF121A35))
            .padding(6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        AudioLibraryTab.entries.forEach { tab ->
            val isSelected = selected == tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (isSelected) AudioSelectedTabBrush else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent)))
                    .border(
                        width = if (isSelected) 1.dp else 0.dp,
                        color = if (isSelected) Color(0xFF8F84FF) else Color.Transparent,
                        shape = RoundedCornerShape(22.dp)
                    )
                    .clickable { onSelected(tab) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab.label,
                    color = if (isSelected) Color(0xFF9B8CFF) else Color(0xFFA5AABD),
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun AudioLibraryContent(
    library: AudioLibrary,
    activeAudio: AudioItem?,
    playback: PlaybackUiState,
    onPlay: (AudioItem) -> Unit,
    onPauseOrResume: () -> Unit
) {
    val grouped = library.recommended
        .filter { it.isPlayableAudio() }
        .groupBy { it.category.ifBlank { "Library" } }
    if (grouped.isEmpty()) {
        EmptyAudioState(
            title = "No audio yet",
            subtitle = "Upload a file or ask Floppy for a recommendation."
        )
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        grouped.forEach { (category, items) ->
            item {
                AudioShelf(
                    title = category,
                    audios = items,
                    activeAudio = activeAudio,
                    playback = playback,
                    onPlay = onPlay,
                    onPauseOrResume = onPauseOrResume
                )
            }
        }
    }
}

@Composable
private fun AudioShelf(
    title: String,
    audios: List<AudioItem>,
    activeAudio: AudioItem?,
    playback: PlaybackUiState,
    onPlay: (AudioItem) -> Unit,
    onPauseOrResume: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(audios) { audio ->
                val isCurrent = playback.currentAudio?.id == audio.id
                val isPlaying = isCurrent &&
                    playback.state in setOf(PlaybackState.Playing, PlaybackState.Buffering)
                AudioCoverCard(
                    audio = audio,
                    isActive = activeAudio?.id == audio.id,
                    isPlaying = isPlaying,
                    onClick = {
                        if (isCurrent) {
                            onPauseOrResume()
                        } else {
                            onPlay(audio)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun AudioCoverCard(
    audio: AudioItem,
    isActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(width = 104.dp, height = 136.dp)
            .clip(RoundedCornerShape(7.dp))
            .border(
                width = if (isActive) 1.dp else 0.dp,
                color = if (isActive) ResearchPurple else Color.Transparent,
                shape = RoundedCornerShape(7.dp)
            )
            .background(audio.artworkBrush())
            .clickable(onClick = onClick)
    ) {
        OptionalDrawable(
            name = audio.coverDrawableName(),
            contentDescription = audio.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.14f),
                    radius = size.width * 0.7f,
                    center = Offset(size.width * 0.88f, size.height * 0.2f)
                )
                drawCircle(
                    color = Color.Black.copy(alpha = 0.18f),
                    radius = size.width * 0.8f,
                    center = Offset(size.width * 0.15f, size.height * 0.98f)
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.45f)
                        )
                    )
                )
        )
        Text(
            text = "Futura",
            color = Color.White.copy(alpha = 0.62f),
            fontSize = 8.sp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 7.dp)
        )
        PlayCircle(
            isPlaying = isPlaying,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 8.dp)
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 10.dp, end = 8.dp, bottom = 10.dp)
        ) {
            Text(
                text = audio.title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = audio.subtitle,
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun UploadsContent(
    uploads: List<UploadItem>,
    onStartUpload: () -> Unit,
    onRetryUpload: (String) -> Unit,
    onCompleteUpload: (String) -> Unit,
    onPlay: (AudioItem) -> Unit,
    playback: PlaybackUiState,
    onPauseOrResume: () -> Unit
) {
    var dismissedHeroUploadIds by remember { mutableStateOf(emptySet<String>()) }
    val activeUpload = uploads.firstOrNull { upload ->
        upload.id !in dismissedHeroUploadIds &&
            (upload.status == UploadStatus.Uploading ||
                upload.status == UploadStatus.Failed ||
                (upload.status == UploadStatus.Completed && !upload.id.startsWith("upload-complete")))
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            UploadHeroCard(
                upload = activeUpload,
                onSelectFile = onStartUpload,
                onRetryUpload = onRetryUpload,
                onCompleteUpload = onCompleteUpload,
                onDismissUpload = { uploadId -> dismissedHeroUploadIds = dismissedHeroUploadIds + uploadId }
            )
        }
        item {
            Text(
                text = "My upload",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        items(uploads) { upload ->
            val audio = upload.generatedAudio
            val isCurrent = audio != null && playback.currentAudio?.id == audio.id
            val isPlaying = isCurrent &&
                playback.state in setOf(PlaybackState.Playing, PlaybackState.Buffering)
            UploadListRow(
                upload = upload,
                isPlaying = isPlaying,
                onClick = {
                    when {
                        audio == null -> Unit
                        isCurrent -> onPauseOrResume()
                        else -> onPlay(audio)
                    }
                }
            )
        }
    }
}

@Composable
private fun UploadHeroCard(
    upload: UploadItem?,
    onSelectFile: () -> Unit,
    onRetryUpload: (String) -> Unit,
    onCompleteUpload: (String) -> Unit,
    onDismissUpload: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(22.dp))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(22.dp))
            .background(Color(0xFF131C37))
            .clickable(onClick = onSelectFile)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        if (upload == null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                UploadIconBubble(icon = "paw", status = UploadStatus.Idle)
                Text("Select file upload", color = Color.White, fontSize = 14.sp)
                Text(
                    text = "You can upload your own books, passages, music and podcasts (pdf, txt, mp3) here.",
                    color = Color(0xFFAFA8FF),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        } else {
            val canRetry = upload.status == UploadStatus.Failed && !upload.id.startsWith("remote-pending")
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                UploadIconBubble(icon = upload.fileType, status = upload.status)
                Text(upload.fileName, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                when (upload.status) {
                    UploadStatus.Uploading -> UploadProgressRow(label = "Uploading...", progress = upload.progress)
                    UploadStatus.Failed -> UploadProgressRow(
                        label = upload.message ?: "Upload failed - The file is too large",
                        progress = upload.progress,
                        isError = true
                    )

                    UploadStatus.Completed -> UploadProgressRow(
                        label = upload.message ?: if (upload.generatedAudio != null) "Upload complete, ready to play" else "Upload complete, waiting for audio",
                        progress = upload.progress
                    )

                    UploadStatus.Idle -> Unit
                }
                when (upload.status) {
                    UploadStatus.Failed -> Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        if (canRetry) {
                            Button(
                                onClick = { onRetryUpload(upload.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = ResearchPurple),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            ) {
                                Text(
                                    text = "Retry upload",
                                    fontSize = 12.sp,
                                    lineHeight = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = onSelectFile,
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Text(
                                text = "Change file",
                                color = Color.White,
                                fontSize = 12.sp,
                                lineHeight = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    UploadStatus.Completed -> Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Button(
                            onClick = { onDismissUpload(upload.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = ResearchPurple),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Text(
                                text = "return",
                                fontSize = 12.sp,
                                lineHeight = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                        OutlinedButton(
                            onClick = onSelectFile,
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Text(
                                text = "Continue uploading",
                                color = Color.White,
                                fontSize = 12.sp,
                                lineHeight = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    UploadStatus.Uploading -> TextButton(onClick = { onCompleteUpload(upload.id) }) {
                        Text("Continue uploading", color = Color.White.copy(alpha = 0.65f), fontSize = 11.sp)
                    }

                    UploadStatus.Idle -> Unit
                }
            }
        }
    }
}

@Composable
private fun UploadProgressRow(
    label: String,
    progress: Float,
    isError: Boolean = false
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            color = if (isError) Color(0xFFFF5E6A) else Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(7.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color(0xFF31384F))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(7.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(ResearchPurple)
                )
            }
            Text(
                "${(progress * 100).roundToInt()}%",
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier.offset(y = (-4).dp)
            )
        }
    }
}

@Composable
private fun UploadListRow(
    upload: UploadItem,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val isPlayable = upload.generatedAudio != null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF111A34))
            .clickable(enabled = isPlayable, onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UploadIconBubble(icon = upload.fileType, status = upload.status, compact = true)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = upload.fileName,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(upload.sizeLabel, color = Color.White.copy(alpha = 0.58f), fontSize = 12.sp)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color(0xFF302D74))
                        .padding(horizontal = 9.dp, vertical = 3.dp)
                ) {
                    Text(upload.status.statusLabel(), color = Color(0xFFC5BCFF), fontSize = 10.sp)
                }
            }
        }
        if (isPlayable) {
            PlayCircle(
                isPlaying = isPlaying,
                modifier = Modifier.size(34.dp)
            )
        } else {
            Text(
                text = "待处理",
                color = Color.White.copy(alpha = 0.34f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun HistoryContent(
    history: List<AudioItem>,
    playback: PlaybackUiState,
    onPlay: (AudioItem) -> Unit,
    onPauseOrResume: () -> Unit
) {
    var filter by remember { mutableStateOf(HistoryFilter.All) }
    val filtered = history.distinctBy { it.id }.filter { audio ->
        when (filter) {
            HistoryFilter.All -> true
            HistoryFilter.Library -> audio.source == AudioSource.Library || audio.source == AudioSource.Generated
            HistoryFilter.Uploads -> audio.source == AudioSource.Upload
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Text("Playback history", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HistoryFilter.entries.forEach { item ->
                    HistoryFilterChip(
                        label = item.label,
                        selected = filter == item,
                        onClick = { filter = item },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        item {
            Text("Today", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        if (filtered.isEmpty()) {
            item {
                EmptyAudioState(
                    title = "No history yet",
                    subtitle = "Play something from Library or Uploads first."
                )
            }
        } else {
            items(filtered) { audio ->
                val isCurrent = playback.currentAudio?.id == audio.id
                val isPlaying = isCurrent &&
                    playback.state in setOf(PlaybackState.Playing, PlaybackState.Buffering)
                HistoryRow(
                    audio = audio,
                    isPlaying = isPlaying,
                    onClick = {
                        if (!audio.isPlayableAudio()) {
                            return@HistoryRow
                        }
                        if (isCurrent) {
                            onPauseOrResume()
                        } else {
                            onPlay(audio)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptyAudioState(
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 220.dp)
            .padding(vertical = 36.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            UploadIconBubble(icon = "paw", status = UploadStatus.Idle, compact = true)
            Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                text = subtitle,
                color = Color(0xFFA7AEC7),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}

private enum class HistoryFilter(val label: String) {
    All("All"),
    Library("Library"),
    Uploads("Uploads")
}

@Composable
private fun HistoryFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(if (selected) ResearchPurple else Color(0xFF303752))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 15.sp)
    }
}

@Composable
private fun HistoryRow(
    audio: AudioItem,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val isPlayable = audio.isPlayableAudio()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(20.dp))
            .background(Color(0xFF131C37))
            .clickable(enabled = isPlayable, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(14.dp))
            .background(audio.artworkBrush()),
            contentAlignment = Alignment.Center
        ) {
            OptionalDrawable(
                name = audio.coverDrawableName(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            ) {}
            if (isPlayable) {
                PlayCircle(isPlaying = isPlaying)
            } else {
                Text("…", color = Color.White.copy(alpha = 0.62f), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = audio.title.ifBlank { "Untitled audio" },
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (isPlayable) {
                    "${audio.historySubtitle()} · ${(audio.playbackProgress * 100).roundToInt()}%"
                } else {
                    "暂不可播"
                },
                color = Color.White.copy(alpha = 0.62f),
                fontSize = 14.sp
            )
            Box(
                modifier = Modifier
                    .width(116.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color(0xFF30384E))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(audio.playbackProgress.coerceIn(0f, 1f))
                        .height(5.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(ResearchPurple)
                )
            }
        }
    }
}

@Composable
private fun FloppyMiniButton(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color(0xFF111628))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(20.dp)) {
            drawFloppy(Color(0xFF5B559A))
        }
    }
}

@Composable
private fun PlayCircle(
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false
) {
    Box(
        modifier = modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Color(0xFF111936).copy(alpha = 0.86f))
            .border(1.dp, Color.White.copy(alpha = 0.55f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (isPlaying) {
            VoicePauseGlyph()
        } else {
            VoicePlayGlyph()
        }
    }
}

@Composable
private fun UploadIconBubble(
    icon: String,
    status: UploadStatus,
    compact: Boolean = false
) {
    val bubbleSize = if (compact) 48.dp else 72.dp
    Box(
        modifier = Modifier
            .size(bubbleSize)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        ResearchPurple.copy(alpha = 0.82f),
                        Color(0xFF243158),
                        Color.Transparent
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        when {
            status == UploadStatus.Completed && !compact -> SuccessBadge()
            icon.equals("paw", ignoreCase = true) -> PawGlyph()
            icon.equals("mp3", ignoreCase = true) || icon.equals("music", ignoreCase = true) -> MusicGlyph()
            else -> WordGlyph(compact = compact)
        }
    }
}

@Composable
private fun WordGlyph(compact: Boolean = false) {
    Box(
        modifier = Modifier
            .size(if (compact) 24.dp else 34.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(Color(0xFF11A7FF)),
        contentAlignment = Alignment.Center
    ) {
        Text("W", color = Color.White, fontSize = if (compact) 13.sp else 19.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun MusicGlyph() {
    Canvas(modifier = Modifier.size(34.dp)) {
        drawCircle(
            color = Color.White,
            radius = size.width * 0.16f,
            center = Offset(size.width * 0.30f, size.height * 0.74f)
        )
        drawCircle(
            color = Color.White,
            radius = size.width * 0.16f,
            center = Offset(size.width * 0.74f, size.height * 0.64f)
        )
        drawLine(
            color = Color.White,
            start = Offset(size.width * 0.44f, size.height * 0.74f),
            end = Offset(size.width * 0.44f, size.height * 0.28f),
            strokeWidth = size.width * 0.08f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White,
            start = Offset(size.width * 0.88f, size.height * 0.64f),
            end = Offset(size.width * 0.88f, size.height * 0.18f),
            strokeWidth = size.width * 0.08f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White,
            start = Offset(size.width * 0.44f, size.height * 0.28f),
            end = Offset(size.width * 0.88f, size.height * 0.18f),
            strokeWidth = size.width * 0.08f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun PawGlyph() {
    Canvas(modifier = Modifier.size(42.dp)) {
        drawCircle(Color.White, radius = size.width * 0.13f, center = Offset(size.width * 0.28f, size.height * 0.30f))
        drawCircle(Color.White, radius = size.width * 0.13f, center = Offset(size.width * 0.50f, size.height * 0.20f))
        drawCircle(Color.White, radius = size.width * 0.13f, center = Offset(size.width * 0.72f, size.height * 0.30f))
        drawCircle(Color.White, radius = size.width * 0.12f, center = Offset(size.width * 0.84f, size.height * 0.52f))
        drawOval(
            color = Color.White,
            topLeft = Offset(size.width * 0.24f, size.height * 0.44f),
            size = Size(size.width * 0.50f, size.height * 0.38f)
        )
    }
}

@Composable
private fun SuccessBadge() {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color(0xFF16C86E)),
        contentAlignment = Alignment.Center
    ) {
        Text("✓", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

private fun AudioItem.artworkBrush(): Brush {
    val seed = Color(artwork?.seedColor ?: 0xFF7D6BFF)
    return Brush.linearGradient(
        colors = listOf(
            seed.copy(alpha = 0.96f),
            Color(0xFF101936),
            Color(0xFF070B16)
        ),
        start = Offset.Zero,
        end = Offset(140f, 180f)
    )
}

private const val CoverImageCount = 18

private fun AudioItem.coverDrawableName(): String {
    val index = Math.floorMod(id.hashCode(), CoverImageCount) + 1
    return "cover_%02d".format(index)
}

private fun AudioItem.historySubtitle(): String {
    return subtitle.ifBlank {
        when (source) {
            AudioSource.Upload -> "Uploaded file"
            AudioSource.Generated -> "Generated audio"
            AudioSource.Library -> category.ifBlank { "Library audio" }
        }
    }
}

private fun AudioItem.isPlayableAudio(): Boolean = streamUrl.isNotBlank()

private fun UploadStatus.statusLabel(): String {
    return when (this) {
        UploadStatus.Idle -> "待上传"
        UploadStatus.Uploading -> "上传中"
        UploadStatus.Failed -> "失败"
        UploadStatus.Completed -> "已完成"
    }
}

@Composable
private fun AudioRow(
    audio: AudioItem,
    isActive: Boolean,
    onPlay: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) Color(0xFFE0EFE9) else Color(0xFFFFFCF7))
            .border(1.dp, Color(0xFFE0D9CB), RoundedCornerShape(8.dp))
            .clickable(onClick = onPlay)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(audio.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(audio.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(formatTime(audio.durationSeconds.toLong()), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsScreen(
    settings: AppSettings,
    voiceOptions: List<VoiceOption>,
    isLoadingVoiceOptions: Boolean,
    isSavingVoiceSelection: Boolean,
    selectedVoiceId: String?,
    onBack: () -> Unit,
    onUpdateSettings: (AppSettings) -> Unit,
    onRefreshVoiceOptions: () -> Unit,
    onSaveVoiceSelection: (String) -> Unit
) {
    val context = LocalContext.current
    var draft by remember(settings) { mutableStateOf(settings) }
    var renameOpen by remember { mutableStateOf(false) }
    var voicePickerOpen by remember { mutableStateOf(false) }
    var companionPickerOpen by remember { mutableStateOf(false) }
    var devicePickerOpen by remember { mutableStateOf(false) }
    var voicePreviewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var previewingVoiceId by remember { mutableStateOf<String?>(null) }

    fun stopVoicePreview() {
        voicePreviewPlayer?.release()
        voicePreviewPlayer = null
        previewingVoiceId = null
    }

    fun toggleVoicePreview(option: VoiceOption) {
        if (previewingVoiceId == option.id) {
            stopVoicePreview()
            return
        }
        if (option.previewAudioUrl.isBlank()) return

        stopVoicePreview()
        val nextPlayer = runCatching {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(context, Uri.parse(option.previewAudioUrl))
                setOnPreparedListener { preparedPlayer ->
                    if (voicePreviewPlayer === preparedPlayer) {
                        preparedPlayer.start()
                        previewingVoiceId = option.id
                    } else {
                        runCatching { preparedPlayer.release() }
                    }
                }
                setOnCompletionListener { completedPlayer ->
                    if (voicePreviewPlayer === completedPlayer) {
                        stopVoicePreview()
                    } else {
                        runCatching { completedPlayer.release() }
                    }
                }
                setOnErrorListener { errorPlayer, _, _ ->
                    if (voicePreviewPlayer === errorPlayer) {
                        stopVoicePreview()
                    } else {
                        runCatching { errorPlayer.release() }
                    }
                    true
                }
            }
        }.getOrElse {
            stopVoicePreview()
            return
        }
        voicePreviewPlayer = nextPlayer
        runCatching {
            nextPlayer.prepareAsync()
        }.onFailure {
            stopVoicePreview()
        }
    }

    fun closeActiveOverlay(): Boolean {
        return when {
            renameOpen -> {
                renameOpen = false
                true
            }

            voicePickerOpen -> {
                stopVoicePreview()
                voicePickerOpen = false
                true
            }

            companionPickerOpen -> {
                companionPickerOpen = false
                true
            }

            devicePickerOpen -> {
                devicePickerOpen = false
                true
            }

            else -> false
        }
    }

    BackHandler(enabled = renameOpen || voicePickerOpen || companionPickerOpen || devicePickerOpen) {
        closeActiveOverlay()
    }

    LaunchedEffect(Unit) {
        onRefreshVoiceOptions()
    }

    LaunchedEffect(voicePickerOpen) {
        if (voicePickerOpen) {
            onRefreshVoiceOptions()
        } else {
            stopVoicePreview()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            stopVoicePreview()
        }
    }
    val fallbackVoiceOptions = remember { LocalVoiceOptions.all() }
    val displayedVoiceOptions = voiceOptions.ifEmpty { fallbackVoiceOptions }
    val usingFallbackVoiceOptions = voiceOptions.isEmpty()
    val fallbackVoiceId = LocalVoiceOptions.idFor(draft.profile.voicePreference)
    val activeVoiceId = selectedVoiceId
        ?.takeIf { selectedId -> displayedVoiceOptions.any { it.id == selectedId } }
        ?: fallbackVoiceId
    val selectedVoiceOption = displayedVoiceOptions.firstOrNull { it.id == activeVoiceId }
    val selectedVoiceLabel = selectedVoiceOption?.name ?: draft.profile.voicePreference.label

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SettingsBackground)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = ResearchPurple.copy(alpha = 0.22f),
                radius = size.width * 0.56f,
                center = Offset(-size.width * 0.12f, size.height * 0.42f)
            )
            drawCircle(
                color = Color(0xFF2A74FF).copy(alpha = 0.10f),
                radius = size.width * 0.38f,
                center = Offset(size.width * 1.04f, size.height * 0.22f)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.statusBars.asPaddingValues())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            contentPadding = PaddingValues(bottom = 30.dp)
        ) {
            item {
                SettingsTopBar(
                    onBack = onBack,
                    onDeviceClick = { devicePickerOpen = true }
                )
            }
            item {
                SettingsHeroFloppy(onDeviceClick = { devicePickerOpen = true })
            }
            item {
                SettingsActionCard(
                    icon = SettingsIcon.User,
                    title = "用户名",
                    subtitle = "这是Floppy如何称呼你",
                    trailing = draft.userNickname,
                    onClick = { renameOpen = true }
                )
            }
            item {
                SettingsActionCard(
                    icon = SettingsIcon.Heart,
                    title = "Ai伙伴人设",
                    subtitle = "Floppy性格与陪伴方式",
                    trailing = draft.profile.companionStyle.label,
                    onClick = { companionPickerOpen = true }
                ) {
                    SettingsDescriptionBox(draft.profile.companionStyle.description())
                }
            }
            item {
                SettingsActionCard(
                    icon = SettingsIcon.Wave,
                    title = "Ai伙伴音色",
                    subtitle = "选择Floppy音色",
                    trailing = selectedVoiceLabel,
                    onClick = { voicePickerOpen = true }
                ) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(62.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF242B4F))
                            .padding(horizontal = 18.dp)
                    ) {
                        val buttonWidth = 61.dp
                        val waveformWidth = (maxWidth - buttonWidth - 16.dp).coerceAtMost(207.dp)
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            VoiceWaveform(
                                modifier = Modifier.size(width = waveformWidth, height = 24.dp),
                                accentIndex = 10
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            SettingsPillButton(
                                label = if (previewingVoiceId == selectedVoiceOption?.id) "暂停" else "试听",
                                onClick = {
                                    selectedVoiceOption?.let { option ->
                                        toggleVoicePreview(option)
                                    } ?: onRefreshVoiceOptions()
                                }
                            )
                        }
                    }
                }
            }
            item {
                SettingsActionCard(
                    icon = SettingsIcon.Moon,
                    title = "平均入睡时长",
                    subtitle = "近一周入睡节奏",
                    trailing = draft.profile.bedtime
                ) {
                    SleepChart()
                }
            }
            item {
                SettingsFootnote()
            }
        }

        if (renameOpen) {
            RenameDialog(
                value = draft.userNickname,
                onCancel = { renameOpen = false },
                onConfirm = { name ->
                    draft = draft.copy(userNickname = name.ifBlank { "小宝" })
                    onUpdateSettings(draft.copy(userNickname = name.ifBlank { "小宝" }))
                    renameOpen = false
                }
            )
        }

        if (voicePickerOpen) {
            SettingsPickerSheet(
                title = "请选择心仪的AI伙伴音色",
                onClose = {
                    stopVoicePreview()
                    voicePickerOpen = false
                }
            ) {
                when {
                    isLoadingVoiceOptions && voiceOptions.isEmpty() -> VoicePickerStatus("音色加载中...")
                    else -> displayedVoiceOptions.forEach { option ->
                        val mappedPreference = VoicePreference.entries.firstOrNull { LocalVoiceOptions.idFor(it) == option.id }
                        VoiceOptionRow(
                            option = option,
                            selected = option.id == activeVoiceId,
                            isPreviewing = option.id == previewingVoiceId,
                            enabled = !isSavingVoiceSelection,
                            onPreviewClick = { toggleVoicePreview(option) },
                            onClick = {
                                val next = mappedPreference?.let { preference ->
                                    draft.copy(profile = draft.profile.copy(voicePreference = preference))
                                } ?: draft
                                draft = next
                                if (usingFallbackVoiceOptions) {
                                    onUpdateSettings(next)
                                } else {
                                    onSaveVoiceSelection(option.id)
                                }
                                stopVoicePreview()
                                voicePickerOpen = false
                            }
                        )
                    }
                }
            }
        }

        if (companionPickerOpen) {
            SettingsPickerSheet(
                title = "请选择ai伙伴人设",
                onClose = { companionPickerOpen = false }
            ) {
                CompanionStyle.entries.forEach { option ->
                    CompanionOptionRow(
                        option = option,
                        selected = option == draft.profile.companionStyle,
                        onClick = {
                            val next = draft.copy(profile = draft.profile.copy(companionStyle = option))
                            draft = next
                            onUpdateSettings(next)
                            companionPickerOpen = false
                        }
                    )
                }
            }
        }

        if (devicePickerOpen) {
            DevicePickerSheet(onClose = { devicePickerOpen = false })
        }
    }
}

private enum class SettingsIcon {
    User,
    Heart,
    Wave,
    Moon
}

@Composable
private fun SettingsTopBar(
    onBack: () -> Unit,
    onDeviceClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
        ) {
            Text(
                text = "‹",
                color = Color.White,
                fontSize = 38.sp,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .clickable(onClick = onBack)
            )
            Text(
                text = "Settings",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Center)
            )
            BluetoothMiniButton(
                modifier = Modifier.align(Alignment.CenterEnd),
                onClick = onDeviceClick
            )
        }
    }
}

@Composable
private fun BluetoothMiniButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color(0xFF111628))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        BluetoothGlyph(modifier = Modifier.size(width = 16.dp, height = 26.dp))
    }
}

@Composable
private fun SettingsHeroFloppy(onDeviceClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(116.dp)
    ) {
        Text(
            text = "Floppy",
            color = Color(0xFF273052).copy(alpha = 0.38f),
            fontSize = 74.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            modifier = Modifier.align(Alignment.TopStart)
        )
        OptionalDrawable(
            name = "settings_floppy_avatar",
            contentDescription = "Floppy profile",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 18.dp)
                .size(116.dp)
                .clickable(onClick = onDeviceClick),
            contentScale = ContentScale.Fit
        ) {
            SettingsFloppyAvatar(
                modifier = Modifier.fillMaxSize(),
                sleepy = false
            )
        }
    }
}

@Composable
private fun SettingsActionCard(
    icon: SettingsIcon,
    title: String,
    subtitle: String,
    trailing: String,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, Color(0xFF55607E), RoundedCornerShape(18.dp))
            .background(Color(0xFF111936).copy(alpha = 0.92f))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingsIconBubble(icon = icon)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = Color(0xFFA5AAC2),
                    fontSize = 12.sp
                )
            }
            Text(
                text = trailing,
                color = Color(0xFFA5AAC2),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (onClick != null) {
                Spacer(modifier = Modifier.width(10.dp))
                Text("›", color = Color(0xFFA5AAC2), fontSize = 25.sp)
            }
        }
        content()
    }
}

@Composable
private fun SettingsIconBubble(icon: SettingsIcon) {
    Box(
        modifier = Modifier
            .size(55.dp)
            .clip(CircleShape)
            .background(Color(0xFF2F2A75)),
        contentAlignment = Alignment.Center
    ) {
        when (icon) {
            SettingsIcon.User -> UserGlyph()
            SettingsIcon.Heart -> HeartGlyph()
            SettingsIcon.Wave -> WaveIconGlyph()
            SettingsIcon.Moon -> MoonGlyph()
        }
    }
}

@Composable
private fun UserGlyph() {
    Canvas(modifier = Modifier.size(26.dp)) {
        val stroke = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(
            color = Color(0xFF8C7DFF),
            radius = size.width * 0.16f,
            center = Offset(size.width * 0.5f, size.height * 0.30f),
            style = stroke
        )
        drawRoundRect(
            color = Color(0xFF8C7DFF),
            topLeft = Offset(size.width * 0.19f, size.height * 0.58f),
            size = Size(size.width * 0.62f, size.height * 0.26f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            style = stroke
        )
    }
}

@Composable
private fun HeartGlyph() {
    Canvas(modifier = Modifier.size(27.dp)) {
        val path = Path().apply {
            moveTo(size.width * 0.50f, size.height * 0.82f)
            cubicTo(size.width * 0.16f, size.height * 0.58f, size.width * 0.08f, size.height * 0.26f, size.width * 0.30f, size.height * 0.16f)
            cubicTo(size.width * 0.42f, size.height * 0.10f, size.width * 0.50f, size.height * 0.20f, size.width * 0.50f, size.height * 0.28f)
            cubicTo(size.width * 0.50f, size.height * 0.20f, size.width * 0.58f, size.height * 0.10f, size.width * 0.70f, size.height * 0.16f)
            cubicTo(size.width * 0.92f, size.height * 0.26f, size.width * 0.84f, size.height * 0.58f, size.width * 0.50f, size.height * 0.82f)
        }
        drawPath(path, color = Color(0xFF8C7DFF), style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
private fun WaveIconGlyph() {
    VoiceWaveform(
        modifier = Modifier.size(width = 28.dp, height = 28.dp),
        bars = 7,
        accentIndex = null,
        color = Color(0xFF8C7DFF)
    )
}

@Composable
private fun MoonGlyph() {
    Canvas(modifier = Modifier.size(28.dp)) {
        drawCircle(
            color = Color(0xFF8C7DFF),
            radius = size.width * 0.36f,
            center = Offset(size.width * 0.46f, size.height * 0.48f)
        )
        drawCircle(
            color = Color(0xFF2F2A75),
            radius = size.width * 0.34f,
            center = Offset(size.width * 0.62f, size.height * 0.36f)
        )
        drawLine(
            color = Color(0xFF8C7DFF),
            start = Offset(size.width * 0.69f, size.height * 0.18f),
            end = Offset(size.width * 0.86f, size.height * 0.18f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFF8C7DFF),
            start = Offset(size.width * 0.78f, size.height * 0.09f),
            end = Offset(size.width * 0.78f, size.height * 0.27f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun SettingsDescriptionBox(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(Color(0xFF242B4F))
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = text,
            color = Color(0xFFA7AEC7),
            fontSize = 13.sp,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun SettingsPillButton(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(100.dp))
            .border(1.dp, Color(0xFFC3BDFF), RoundedCornerShape(100.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color(0xFFC3BDFF), fontSize = 12.sp)
    }
}

@Composable
private fun VoiceWaveform(
    modifier: Modifier = Modifier,
    bars: Int = 16,
    accentIndex: Int? = null,
    color: Color = Color(0xFF8C7DFF),
    isAnimating: Boolean = false
) {
    val transition = rememberInfiniteTransition(label = "voice-preview-wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "voice-preview-wave-phase"
    )
    Canvas(modifier = modifier.height(24.dp)) {
        val barWidth = 3.dp.toPx()
        val gap = if (bars <= 1) 0f else (size.width - barWidth * bars) / (bars - 1)
        val heights = listOf(0.22f, 0.58f, 0.34f, 0.86f, 0.46f, 0.72f, 0.32f, 0.56f)
        repeat(bars) { index ->
            val waveOffset = if (isAnimating) {
                0.18f * sin((phase * 2f * PI + index * 0.72f)).toFloat()
            } else {
                0f
            }
            val heightRatio = (heights[index % heights.size] + waveOffset).coerceIn(0.18f, 1f)
            val height = size.height * heightRatio
            val x = index * (barWidth + gap.coerceAtLeast(1.dp.toPx()))
            val top = (size.height - height) / 2f
            drawRoundRect(
                color = if (accentIndex == index || isAnimating && index % 5 == 0) Color(0xFF1FBBFF) else color,
                topLeft = Offset(x, top),
                size = Size(barWidth, height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth, barWidth)
            )
        }
    }
}

@Composable
private fun SleepChart() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(126.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF242B4F))
            .padding(horizontal = 22.dp, vertical = 18.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawLine(
                color = Color.White.copy(alpha = 0.22f),
                start = Offset(0f, size.height * 0.62f),
                end = Offset(size.width, size.height * 0.62f),
                strokeWidth = 1.dp.toPx(),
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(7f, 6f))
            )
            val heights = listOf(0.66f, 0.44f, 0.88f, 0.60f, 0.48f, 0.76f, 0.56f, 1.0f, 0.70f)
            val barWidth = size.width / (heights.size * 2.25f)
            val gap = (size.width - barWidth * heights.size) / (heights.size - 1)
            heights.forEachIndexed { index, ratio ->
                val height = size.height * 0.82f * ratio
                val x = index * (barWidth + gap)
                val brush = if (index == 7) {
                    Brush.verticalGradient(
                        listOf(Color(0xFF9A86FF), Color(0xFF705CFF))
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(Color(0xFF5F56B9).copy(alpha = 0.82f), Color(0xFF433D88).copy(alpha = 0.88f))
                    )
                }
                drawRoundRect(
                    brush = brush,
                    topLeft = Offset(x, size.height - height),
                    size = Size(barWidth, height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(7.dp.toPx(), 7.dp.toPx())
                )
            }
        }
    }
}

@Composable
private fun SettingsFootnote() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 28.dp, top = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Canvas(modifier = Modifier.size(15.dp)) {
            drawCircle(
                color = Color(0xFF8178DB),
                radius = size.width * 0.35f,
                center = Offset(size.width * 0.5f, size.height * 0.42f),
                style = Stroke(width = 1.4.dp.toPx())
            )
            drawLine(
                color = Color(0xFF8178DB),
                start = Offset(size.width * 0.5f, size.height * 0.74f),
                end = Offset(size.width * 0.5f, size.height * 0.95f),
                strokeWidth = 1.4.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        Text(
            text = "Floppy 会根据你的习惯，持续优化陪伴体验哦~",
            color = Color(0xFF8178DB),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun RenameDialog(
    value: String,
    onCancel: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember(value) { mutableStateOf(TextFieldValue(value)) }
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        ModalScrim(alpha = 0.74f, onClick = onCancel)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 64.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF151E3B))
                .border(1.dp, Color(0xFF2E3962), RoundedCornerShape(10.dp))
                .clickable(onClick = {})
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "修改名称",
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF242B4F))
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                cursorBrush = Brush.verticalGradient(listOf(ResearchPurple, ResearchPurple))
            )
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                SettingsDialogButton(
                    label = "取消",
                    filled = false,
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                )
                SettingsDialogButton(
                    label = "确定",
                    filled = true,
                    onClick = { onConfirm(text.text.trim()) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ModalScrim(
    alpha: Float,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = alpha))
            .pointerInput(onClick) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                        if (event.changes.any { it.changedToUp() }) {
                            onClick?.invoke()
                        }
                    }
                }
            }
    )
}

@Composable
private fun SettingsDialogButton(
    label: String,
    filled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(if (filled) ResearchPurple else Color.Transparent)
            .border(1.dp, if (filled) ResearchPurple else Color(0xFF2C365A), RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = if (filled) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun SettingsPickerSheet(
    title: String,
    onClose: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        ModalScrim(alpha = 0.76f)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 420.dp, max = 710.dp)
                .clip(RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp))
                .background(Color(0xFF121B39))
                .padding(horizontal = 28.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "×",
                    color = Color.White,
                    fontSize = 36.sp,
                    modifier = Modifier.clickable(onClick = onClose)
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                content = content
            )
        }
    }
}

@Composable
private fun VoicePickerStatus(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF242B4F))
            .border(1.dp, Color(0xFF313C69), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = Color(0xFFAEB5CD), fontSize = 14.sp)
    }
}

@Composable
private fun VoiceOptionRow(
    option: VoiceOption,
    selected: Boolean,
    isPreviewing: Boolean,
    enabled: Boolean,
    onPreviewClick: () -> Unit,
    onClick: () -> Unit
) {
    val rowShape = RoundedCornerShape(16.dp)
    val backgroundColor = if (selected) Color(0xFF29325D) else Color(0xFF242B4F)
    val borderColor = when {
        selected -> Color(0xFF746BCA)
        isPreviewing -> Color(0xFF5C54A8)
        else -> Color(0xFF313C69)
    }
    val textColor = if (selected) Color(0xFFE1E5F5) else Color(0xFFAEB5CD)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .clip(rowShape)
            .background(backgroundColor)
            .border(1.dp, borderColor, rowShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = option.name,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            modifier = Modifier.width(96.dp)
        )
        Box(
            modifier = Modifier
                .height(42.dp)
                .weight(1f)
                .clip(RoundedCornerShape(21.dp))
                .clickable(enabled = enabled && option.previewAudioUrl.isNotBlank(), onClick = onPreviewClick)
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            VoiceWaveform(
                modifier = Modifier.fillMaxWidth(),
                color = if (selected || isPreviewing) Color(0xFFA095FF) else Color(0xFF8C7DFF),
                isAnimating = isPreviewing
            )
        }
        Spacer(modifier = Modifier.width(18.dp))
        SettingsRadio(selected = selected)
    }
}

@Composable
private fun CompanionOptionRow(
    option: CompanionStyle,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 78.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF242B4F))
            .border(1.dp, Color(0xFF313C69), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = option.description(),
            color = Color(0xFFAEB5CD),
            fontSize = 14.sp,
            lineHeight = 22.sp,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(16.dp))
        SettingsRadio(selected = selected)
    }
}

@Composable
private fun SettingsRadio(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .border(1.2.dp, Color(0xFF9A8DFF), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF8C7DFF))
            )
        }
    }
}

@Composable
private fun DevicePickerSheet(onClose: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val bluetoothController = remember(context) { FloppyBluetoothController(context) }
    val bluetoothState by bluetoothController.state.collectAsState()
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.all { it }) {
            bluetoothController.startScan()
        } else {
            bluetoothController.markPermissionRequired()
        }
    }

    LaunchedEffect(Unit) {
        bluetoothController.refreshReadiness(context.hasBluetoothPermissions())
    }

    DisposableEffect(context, lifecycleOwner, bluetoothController) {
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                bluetoothController.refreshReadiness(context.hasBluetoothPermissions())
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        }
    }

    DisposableEffect(bluetoothController) {
        onDispose {
            bluetoothController.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        ModalScrim(alpha = 0.76f)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(610.dp)
                .clip(RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp))
                .background(Color(0xFF121B39))
                .padding(horizontal = 28.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("选择可以连接的设备", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("×", color = Color.White, fontSize = 36.sp, modifier = Modifier.clickable(onClick = onClose))
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (bluetoothState.devices.isEmpty()) {
                    DeviceEmptyState(bluetoothState)
                } else {
                    DeviceResultList(
                        state = bluetoothState,
                        onDeviceClick = bluetoothController::connect
                    )
                }
            }
            DevicePrimaryButton(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                state = bluetoothState,
                onClick = {
                    handleBluetoothAction(
                        context = context,
                        state = bluetoothState,
                        requestPermissions = {
                            permissionLauncher.launch(requiredBluetoothPermissions())
                        },
                        startScan = bluetoothController::startScan
                    )
                }
            )
        }
    }
}

@Composable
private fun DeviceEmptyState(state: FloppyBluetoothState) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        Box(
            modifier = Modifier
                .size(144.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF6D62FF), Color(0xFF141D3D))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            OptionalDrawable(
                name = "settings_floppy_avatar",
                contentDescription = "Floppy avatar",
                modifier = Modifier.size(116.dp),
                contentScale = ContentScale.Fit
            ) {
                SettingsFloppyAvatar(
                    modifier = Modifier.size(116.dp),
                    sleepy = state.status == FloppyBluetoothStatus.Scanning
                )
            }
        }
        if (state.status == FloppyBluetoothStatus.Scanning) {
            LinearProgressIndicator(
                modifier = Modifier
                    .width(170.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(20.dp)),
                color = ResearchPurple,
                trackColor = Color.White.copy(alpha = 0.12f)
            )
        }
        Text(
            text = state.bluetoothStatusText(),
            color = Color(0xFFA7AEC7),
            fontSize = 13.sp
        )
    }
}

@Composable
private fun DeviceResultList(
    state: FloppyBluetoothState,
    onDeviceClick: (FloppyBluetoothDevice) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = state.bluetoothStatusText(),
            color = Color(0xFFA7AEC7),
            fontSize = 13.sp
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(
                items = state.devices,
                key = { it.address }
            ) { device ->
                DeviceResultRow(
                    device = device,
                    isSelected = state.selectedAddress == device.address,
                    status = state.status,
                    onClick = { onDeviceClick(device) }
                )
            }
        }
    }
}

@Composable
private fun DeviceResultRow(
    device: FloppyBluetoothDevice,
    isSelected: Boolean,
    status: FloppyBluetoothStatus,
    onClick: () -> Unit
) {
    val isBusy = status == FloppyBluetoothStatus.Connecting && isSelected
    val isConnected = status == FloppyBluetoothStatus.Connected && isSelected

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 74.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF242B4F))
            .border(
                1.dp,
                if (isSelected) Color(0xFF9A8DFF) else Color(0xFF313C69),
                RoundedCornerShape(16.dp)
            )
            .clickable(
                enabled = status != FloppyBluetoothStatus.Connecting,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(if (isConnected) ResearchPurple else Color(0xFF343064)),
            contentAlignment = Alignment.Center
        ) {
            BluetoothGlyph()
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = device.displayName,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = device.deviceSummary(),
                color = Color(0xFFA7AEC7),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = when {
                isBusy -> "连接中"
                isConnected -> "已连接"
                device.isBonded -> "已配对"
                device.canUseGatt -> "连接"
                device.isConnectable -> "连接"
                else -> "尝试"
            },
            color = if (isConnected) Color(0xFFC3BDFF) else Color(0xFFA7AEC7),
            fontSize = 12.sp,
            fontWeight = if (isConnected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

private fun FloppyBluetoothDevice.deviceSummary(): String {
    val signal = rssi?.let { "RSSI $it dBm" } ?: "信号未知"
    return "${source.label}  $signal  $address"
}

@Composable
private fun DevicePrimaryButton(
    modifier: Modifier = Modifier,
    state: FloppyBluetoothState,
    onClick: () -> Unit
) {
    val enabled = state.status != FloppyBluetoothStatus.Scanning &&
        state.status != FloppyBluetoothStatus.Connecting &&
        state.status != FloppyBluetoothStatus.BluetoothUnavailable

    Box(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(if (enabled) ResearchPurple else ResearchPurple.copy(alpha = 0.55f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BluetoothGlyph()
            Text(
                text = state.bluetoothActionText(),
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun FloppyBluetoothState.bluetoothStatusText(): String {
    return message ?: when (status) {
        FloppyBluetoothStatus.Idle -> "准备搜索附近设备"
        FloppyBluetoothStatus.PermissionRequired -> "需要蓝牙权限才能搜索附近设备"
        FloppyBluetoothStatus.BluetoothUnavailable -> "当前设备不支持低功耗蓝牙"
        FloppyBluetoothStatus.BluetoothOff -> "请先打开系统蓝牙"
        FloppyBluetoothStatus.LocationOff -> "请先开启定位服务"
        FloppyBluetoothStatus.Scanning -> if (devices.isEmpty()) "正在搜索附近设备..." else "正在搜索，已发现 ${devices.size} 台设备"
        FloppyBluetoothStatus.DevicesFound -> "选择一台设备进行连接"
        FloppyBluetoothStatus.NotFound -> "暂未发现设备，请确认设备处于配对模式，并打开系统定位"
        FloppyBluetoothStatus.Connecting -> "正在连接设备..."
        FloppyBluetoothStatus.Connected -> "设备已连接"
        FloppyBluetoothStatus.ConnectionFailed -> "连接失败，请重试"
    }
}

private fun FloppyBluetoothState.bluetoothActionText(): String {
    return when (status) {
        FloppyBluetoothStatus.PermissionRequired -> "授权蓝牙"
        FloppyBluetoothStatus.BluetoothOff -> "打开蓝牙"
        FloppyBluetoothStatus.LocationOff -> "开启定位"
        FloppyBluetoothStatus.Scanning -> "搜索中"
        FloppyBluetoothStatus.Connecting -> "连接中"
        FloppyBluetoothStatus.DevicesFound,
        FloppyBluetoothStatus.NotFound,
        FloppyBluetoothStatus.ConnectionFailed,
        FloppyBluetoothStatus.Connected -> "重新搜索"
        FloppyBluetoothStatus.BluetoothUnavailable,
        FloppyBluetoothStatus.Idle -> "添加设备"
    }
}

private fun handleBluetoothAction(
    context: Context,
    state: FloppyBluetoothState,
    requestPermissions: () -> Unit,
    startScan: () -> Unit
) {
    when {
        !context.hasBluetoothPermissions() -> requestPermissions()
        state.status == FloppyBluetoothStatus.BluetoothOff -> {
            context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
        }
        state.status == FloppyBluetoothStatus.LocationOff -> {
            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
        else -> startScan()
    }
}

private fun Context.hasBluetoothPermissions(): Boolean {
    return requiredBluetoothPermissions().all { permission ->
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }
}

private fun requiredBluetoothPermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
}

@Composable
private fun BluetoothGlyph(modifier: Modifier = Modifier.size(width = 13.dp, height = 22.dp)) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        val centerX = size.width * 0.48f
        drawLine(
            color = Color.White,
            start = Offset(centerX, size.height * 0.06f),
            end = Offset(centerX, size.height * 0.94f),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round
        )
        val upper = Path().apply {
            moveTo(centerX, size.height * 0.06f)
            lineTo(size.width * 0.92f, size.height * 0.30f)
            lineTo(centerX, size.height * 0.50f)
            lineTo(size.width * 0.10f, size.height * 0.24f)
        }
        val lower = Path().apply {
            moveTo(centerX, size.height * 0.50f)
            lineTo(size.width * 0.92f, size.height * 0.70f)
            lineTo(centerX, size.height * 0.94f)
            lineTo(size.width * 0.10f, size.height * 0.76f)
        }
        drawPath(upper, color = Color.White, style = stroke)
        drawPath(lower, color = Color.White, style = stroke)
    }
}

@Composable
private fun SettingsFloppyAvatar(
    modifier: Modifier = Modifier,
    sleepy: Boolean
) {
    Canvas(modifier = modifier) {
        drawCircle(
            color = Color.White.copy(alpha = 0.16f),
            radius = size.minDimension * 0.42f,
            center = Offset(size.width * 0.52f, size.height * 0.48f)
        )
        drawOval(
            color = Color(0xFF232432),
            topLeft = Offset(size.width * 0.22f, size.height * 0.20f),
            size = Size(size.width * 0.58f, size.height * 0.54f)
        )
        drawOval(
            color = Color(0xFF12131C),
            topLeft = Offset(size.width * 0.08f, size.height * 0.18f),
            size = Size(size.width * 0.23f, size.height * 0.40f)
        )
        drawOval(
            color = Color(0xFF12131C),
            topLeft = Offset(size.width * 0.70f, size.height * 0.18f),
            size = Size(size.width * 0.24f, size.height * 0.42f)
        )
        drawOval(
            color = Color(0xFF3A3B48).copy(alpha = 0.85f),
            topLeft = Offset(size.width * 0.27f, size.height * 0.15f),
            size = Size(size.width * 0.45f, size.height * 0.28f)
        )
        drawRoundRect(
            color = Color(0xFF555DFF),
            topLeft = Offset(size.width * 0.42f, size.height * 0.70f),
            size = Size(size.width * 0.22f, size.height * 0.20f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )
        val leftEyeCenter = Offset(size.width * 0.42f, size.height * 0.45f)
        val rightEyeCenter = Offset(size.width * 0.62f, size.height * 0.45f)
        if (sleepy) {
            drawArc(
                color = Color.White,
                startAngle = 20f,
                sweepAngle = 130f,
                useCenter = false,
                topLeft = Offset(leftEyeCenter.x - size.width * 0.07f, leftEyeCenter.y - size.height * 0.02f),
                size = Size(size.width * 0.14f, size.height * 0.10f),
                style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = Color.White,
                startAngle = 20f,
                sweepAngle = 130f,
                useCenter = false,
                topLeft = Offset(rightEyeCenter.x - size.width * 0.07f, rightEyeCenter.y - size.height * 0.02f),
                size = Size(size.width * 0.14f, size.height * 0.10f),
                style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
            )
        } else {
            drawCircle(Color.White, radius = size.width * 0.075f, center = leftEyeCenter)
            drawCircle(Color.White, radius = size.width * 0.075f, center = rightEyeCenter)
            drawCircle(Color(0xFF171820), radius = size.width * 0.025f, center = Offset(leftEyeCenter.x + size.width * 0.02f, leftEyeCenter.y + size.height * 0.01f))
            drawCircle(Color(0xFF171820), radius = size.width * 0.025f, center = Offset(rightEyeCenter.x - size.width * 0.02f, rightEyeCenter.y + size.height * 0.01f))
        }
        drawOval(
            color = Color(0xFF0E0F17),
            topLeft = Offset(size.width * 0.49f, size.height * 0.53f),
            size = Size(size.width * 0.08f, size.height * 0.05f)
        )
        drawArc(
            color = if (sleepy) Color(0xFF8A8EA4) else Color(0xFFFFB2A8),
            startAngle = 15f,
            sweepAngle = 150f,
            useCenter = false,
            topLeft = Offset(size.width * 0.44f, size.height * 0.57f),
            size = Size(size.width * 0.16f, size.height * 0.11f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

private fun CompanionStyle.description(): String {
    return when (this) {
        CompanionStyle.Gentle -> "温柔、耐心、善解人意，擅长倾听和安慰，帮助你放松心情，安心入睡。"
        CompanionStyle.Patient -> "慢慢陪你梳理情绪，回应更细腻，适合需要被认真听见的夜晚。"
        CompanionStyle.Reassuring -> "用更稳定的节奏和更柔和的表达，帮你把紧绷感一点点放下来。"
        CompanionStyle.Playful -> "轻松一点、偶尔带点俏皮，让睡前陪伴不沉重，也不会太吵。"
        CompanionStyle.Quiet -> "少说话，多留白，用简短回应和安静氛围陪你慢慢入睡。"
        CompanionStyle.Coaching -> "带你做呼吸、放松和想象练习，适合希望被温和引导的时刻。"
        CompanionStyle.Storyteller -> "用故事感陪你进入睡意，像有人在枕边轻轻讲一段小冒险。"
    }
}

private fun agentStatusText(state: FloppyUiState): String {
    return when (state.agentState) {
        AgentState.Idle -> "Floppy正在等你轻触为你推荐"
        AgentState.Listening -> "Floppy 正在听"
        AgentState.Recommending -> "正在推荐"
        AgentState.Generating -> "正在生成"
        AgentState.Ready -> "今晚的音频准备好了"
        AgentState.Playing -> "正在播放"
        AgentState.Paused -> "已暂停"
        AgentState.Failed -> "这次没有顺利完成"
    }
}

private fun formatTime(seconds: Long): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%d:%02d".format(minutes, remainingSeconds)
}

private data class UploadFileInfo(
    val fileName: String,
    val fileType: String,
    val mimeType: String?
)

private fun Context.toUploadFileInfo(uri: Uri): UploadFileInfo {
    val displayName = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) cursor.getString(index) else null
            } else {
                null
            }
        }
        ?.takeIf { it.isNotBlank() }
        ?: uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
        ?: "Selected file"

    val mimeType = contentResolver.getType(uri)
    val fileType = when {
        mimeType?.contains("audio", ignoreCase = true) == true -> "mp3"
        mimeType?.contains("video", ignoreCase = true) == true -> "video"
        mimeType?.contains("pdf", ignoreCase = true) == true -> "pdf"
        mimeType?.contains("text", ignoreCase = true) == true -> "txt"
        displayName.substringAfterLast('.', "").isNotBlank() -> displayName.substringAfterLast('.').lowercase()
        else -> "file"
    }

    return UploadFileInfo(
        fileName = displayName,
        fileType = fileType,
        mimeType = mimeType
    )
}

private fun <T> Set<T>.toggle(item: T): Set<T> {
    return if (contains(item)) this - item else this + item
}
