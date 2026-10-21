package com.wl.pacebeat.ui

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import com.wl.pacebeat.data.PlayerSettings
import com.wl.pacebeat.data.formatRemaining
import com.wl.pacebeat.data.formatTotal
import com.wl.pacebeat.service.PlaybackState
import com.wl.pacebeat.ui.theme.Background
import com.wl.pacebeat.ui.theme.CardWhite
import com.wl.pacebeat.ui.theme.Cream
import com.wl.pacebeat.ui.theme.Danger
import com.wl.pacebeat.ui.theme.Divider
import com.wl.pacebeat.ui.theme.Ink
import com.wl.pacebeat.ui.theme.InkFaint
import com.wl.pacebeat.ui.theme.InkSoft
import com.wl.pacebeat.ui.theme.Orange
import com.wl.pacebeat.ui.theme.OrangeSoft
import com.wl.pacebeat.ui.theme.Panel
import com.wl.pacebeat.ui.theme.Track
import com.wl.pacebeat.viewmodel.AppViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun RunScreen(
    vm: AppViewModel,
    playback: PlaybackState
) {
    if (playback.completed) {
        CompletionScreen(playback = playback, onDone = vm::dismissCompleted)
        return
    }
    if (playback.current == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("训练已结束", color = InkSoft)
        }
        return
    }

    val context = LocalContext.current
    val view = LocalView.current
    var showSoundPicker by remember { mutableStateOf(false) }
    var showCadenceDialog by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    val pulse = remember { Animatable(1f) }
    LaunchedEffect(playback.beatIndex, playback.index) {
        if (!playback.paused) {
            pulse.snapTo(1.09f)
            pulse.animateTo(1f, animationSpec = tween(180))
        }
    }

    val current = playback.current ?: return
    val next = playback.next
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Cream, Background, Background),
                    startY = 0f,
                    endY = 850f
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        val tight = maxHeight < 760.dp
        val compact = maxHeight < 840.dp
        val orbHeight = when {
            tight -> 150.dp
            compact -> 190.dp
            else -> 228.dp
        }
        val orbOuter = when {
            tight -> 145.dp
            compact -> 180.dp
            else -> 220.dp
        }
        val orbInner = when {
            tight -> 110.dp
            compact -> 135.dp
            else -> 164.dp
        }
        val bpmSize = when {
            tight -> 76.sp
            compact -> 88.sp
            else -> 104.sp
        }
        val controlOuter = when {
            tight -> 58.dp
            compact -> 66.dp
            else -> 74.dp
        }
        val controlInner = when {
            tight -> 50.dp
            compact -> 56.dp
            else -> 62.dp
        }
        val controlItemWidth = when {
            tight -> 88.dp
            compact -> 96.dp
            else -> 104.dp
        }
        val cardRowHeight = when {
            tight -> 48.dp
            compact -> 50.dp
            else -> 56.dp
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
        Spacer(Modifier.height(if (tight) 2.dp else 8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                onClick = vm::backToHome,
                shape = CircleShape,
                color = CardWhite,
                border = BorderStroke(1.dp, Divider),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.KeyboardArrowDown,
                        "收起训练界面",
                        tint = Ink,
                        modifier = Modifier.size(25.dp)
                    )
                }
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    current.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "第 ${playback.index + 1} / ${playback.segments.size} 段${if (playback.paused) " · 已暂停" else ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (playback.paused) Orange else InkFaint
                )
            }
            Text(
                formatRemaining(playback.remainingMs),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                color = Ink
            )
        }

        Spacer(Modifier.height(10.dp))
        SegmentProgress(playback)
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (next != null) {
                "下一段 · ${next.name}  ${next.durationLabel}"
            } else {
                "最后一段 · 完成后将自动结束"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = InkSoft
        )

        Spacer(Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(orbHeight),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(orbOuter)
                    .graphicsLayer {
                        scaleX = pulse.value
                        scaleY = pulse.value
                    }
                    .clip(CircleShape)
                    .background(if (playback.paused) Track.copy(alpha = 0.45f) else Orange.copy(alpha = 0.08f))
            )
            Box(
                modifier = Modifier
                    .size(orbInner)
                    .clip(CircleShape)
                    .background(if (playback.paused) Track.copy(alpha = 0.55f) else Orange.copy(alpha = 0.12f))
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${playback.bpm}",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = bpmSize,
                        lineHeight = bpmSize,
                        letterSpacing = (-5).sp
                    ),
                    color = if (playback.paused) InkFaint else Orange
                )
                Text(
                    "SPM",
                    style = MaterialTheme.typography.titleMedium.copy(
                        letterSpacing = 3.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = InkSoft
                )
                Spacer(Modifier.height(if (tight) 8.dp else 16.dp))
                BeatDots(
                    active = if (playback.paused) -1 else playback.beatIndex,
                    count = playback.beatsPerBar
                )
            }
        }

        PaceCard(
            color = Color(0xFFF1F1F4),
            padding = PaddingValues(
                horizontal = 16.dp,
                vertical = if (tight) 2.dp else 8.dp
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cardRowHeight),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconCircle(Icons.Rounded.GraphicEq, Orange, OrangeSoft, size = 34, iconSize = 18)
                Spacer(Modifier.width(12.dp))
                Text(
                    "语音提示",
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.width(88.dp)
                )
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = playback.voice,
                    onCheckedChange = { vm.setVoice(context, it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Orange,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Track,
                        uncheckedBorderColor = Color.Transparent
                    )
                )
            }
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cardRowHeight),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconCircle(Icons.Rounded.VolumeUp, Orange, OrangeSoft, size = 34, iconSize = 18)
                Spacer(Modifier.width(12.dp))
                Text(
                    "音量增强",
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.width(88.dp)
                )
                Spacer(Modifier.weight(1f))
                SegmentedNumbers(
                    values = listOf(1, 2, 3, 4),
                    selected = playback.multiplier,
                    suffix = "x",
                    modifier = Modifier.width(160.dp)
                ) { vm.setMultiplier(context, it) }
            }
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cardRowHeight)
                    .clip(RoundedCornerShape(13.dp))
                    .clickable { showSoundPicker = true },
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconCircle(Icons.Rounded.MusicNote, Orange, OrangeSoft, size = 34, iconSize = 18)
                Spacer(Modifier.width(12.dp))
                Text(
                    "节拍音乐",
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.width(88.dp)
                )
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardWhite)
                        .clickable { showSoundPicker = true }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "1/${playback.beatsPerBar} · " + playback.soundId.let {
                            com.wl.pacebeat.data.SoundProfile.fromId(it).title
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text("›", style = MaterialTheme.typography.titleMedium, color = InkFaint)
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.VolumeOff, null, tint = InkSoft, modifier = Modifier.size(20.dp))
            PaceSlider(
                value = playback.volume,
                onValueChange = { vm.setVolume(context, it) },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )
            Icon(Icons.Rounded.VolumeUp, null, tint = Ink, modifier = Modifier.size(21.dp))
        }

        Spacer(Modifier.height(8.dp))
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top
        ) {
            RoundAction(
                icon = if (playback.paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                label = if (playback.paused) "继续" else "暂停",
                tint = Color.White,
                background = Ink,
                outerSize = controlOuter,
                innerSize = controlInner,
                itemWidth = controlItemWidth,
                onClick = {
                    if (playback.paused) vm.resume(context) else vm.pause(context)
                }
            )
            RoundAction(
                icon = Icons.Rounded.SkipNext,
                label = "下一段",
                tint = Ink,
                background = CardWhite,
                bordered = true,
                outerSize = controlOuter,
                innerSize = controlInner,
                itemWidth = controlItemWidth,
                onHold = { vm.next(context) }
            )
        }

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top
        ) {
            RoundAction(
                icon = Icons.Rounded.Refresh,
                label = "重置本段",
                tint = Ink,
                background = CardWhite,
                bordered = true,
                outerSize = controlOuter,
                innerSize = controlInner,
                itemWidth = controlItemWidth,
                onClick = { vm.reset(context) }
            )
            RoundAction(
                icon = Icons.Rounded.Stop,
                label = "停止",
                tint = Color.White,
                background = Danger,
                outerSize = controlOuter,
                innerSize = controlInner,
                itemWidth = controlItemWidth,
                onHold = { vm.stop(context) }
            )
            RoundAction(
                icon = Icons.Rounded.Tune,
                label = "步频",
                tint = Ink,
                background = CardWhite,
                bordered = true,
                outerSize = controlOuter,
                innerSize = controlInner,
                itemWidth = controlItemWidth,
                onClick = { showCadenceDialog = true }
            )
        }
        Spacer(Modifier.height(4.dp))
        }
    }

    if (showSoundPicker) {
        SoundPickerSheet(
            selected = com.wl.pacebeat.data.SoundProfile.fromId(playback.soundId),
            beatsPerBar = playback.beatsPerBar,
            onSelect = {
                vm.setSound(context, it)
                showSoundPicker = false
            },
            onBeatsChange = { vm.setBeatsPerBar(context, it) },
            onDismiss = { showSoundPicker = false }
        )
    }

    if (showCadenceDialog) {
        CadenceDialog(
            current = playback.bpm,
            onApply = {
                vm.setBpm(context, it)
                showCadenceDialog = false
            },
            onDismiss = { showCadenceDialog = false }
        )
    }

}

@Composable
private fun SegmentProgress(state: PlaybackState) {
    val total = state.totalSeconds.coerceAtLeast(1)
    val boundaries = remember(state.segments) {
        var acc = 0
        state.segments.dropLast(1).map {
            acc += it.durationSec
            acc.toFloat() / total
        }
    }
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(18.dp)
    ) {
        val cy = size.height / 2f
        val stroke = 6.dp.toPx()
        drawLine(Track, Offset(0f, cy), Offset(size.width, cy), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(
            Orange,
            Offset(0f, cy),
            Offset(size.width * state.progress.coerceIn(0f, 1f), cy),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        boundaries.forEachIndexed { index, fraction ->
            val x = size.width * fraction
            drawCircle(
                color = if (index < state.index) Ink else InkFaint.copy(alpha = 0.55f),
                radius = 4.dp.toPx(),
                center = Offset(x, cy)
            )
            drawCircle(
                color = Background,
                radius = 1.7.dp.toPx(),
                center = Offset(x, cy)
            )
        }
    }
}

@Composable
private fun RoundAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    background: Color,
    bordered: Boolean = false,
    outerSize: Dp = 74.dp,
    innerSize: Dp = 62.dp,
    itemWidth: Dp = 104.dp,
    onClick: (() -> Unit)? = null,
    onHold: (() -> Unit)? = null
) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var triggered by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.width(itemWidth),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(outerSize)
                .clip(CircleShape)
                .then(
                    if (onHold != null) {
                        Modifier.pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    triggered = false
                                    val job = scope.launch {
                                        progress.snapTo(0f)
                                        progress.animateTo(
                                            targetValue = 1f,
                                            animationSpec = tween(1100, easing = LinearEasing)
                                        )
                                        if (progress.value > 0.99f) {
                                            triggered = true
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onHold()
                                            progress.animateTo(0f, animationSpec = tween(160))
                                        }
                                    }
                                    tryAwaitRelease()
                                    if (!triggered) {
                                        job.cancel()
                                        progress.animateTo(0f, animationSpec = tween(140))
                                    }
                                }
                            )
                        }
                    } else {
                        Modifier.clickable { onClick?.invoke() }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (onHold != null) {
                Canvas(Modifier.fillMaxSize()) {
                    if (progress.value > 0.001f) {
                        val stroke = 4.dp.toPx()
                        val inset = stroke / 2f
                        drawArc(
                            color = Orange,
                            startAngle = -90f,
                            sweepAngle = 360f * progress.value,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = Size(size.width - stroke, size.height - stroke),
                            style = Stroke(stroke, cap = StrokeCap.Round)
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .size(innerSize)
                    .clip(CircleShape)
                    .background(background)
                    .then(
                        if (bordered) Modifier.border(1.dp, Divider, CircleShape) else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    label,
                    tint = tint,
                    modifier = Modifier.size(if (innerSize < 58.dp) 21.dp else 24.dp)
                )
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (background == Danger) Danger else InkSoft,
            maxLines = 1
        )
    }
}

@Composable
private fun HorizontalDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Divider.copy(alpha = 0.7f))
    )
}

@Composable
private fun CadenceDialog(
    current: Int,
    onApply: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var bpm by remember { mutableIntStateOf(current) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            shape = RoundedCornerShape(28.dp),
            color = CardWhite
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(OrangeSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Tune, null, tint = Orange, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text("调整步频", style = MaterialTheme.typography.headlineMedium, color = Ink)
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("SPM", style = MaterialTheme.typography.labelLarge, color = InkSoft)
                    Spacer(Modifier.weight(1f))
                    Text(
                        "$bpm",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black
                        ),
                        color = Orange
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StepIcon("−") { bpm = (bpm - 1).coerceAtLeast(100) }
                    PaceSlider(
                        value = bpm.toFloat(),
                        onValueChange = { bpm = it.roundToInt() },
                        valueRange = 100f..240f,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )
                    StepIcon("+") { bpm = (bpm + 1).coerceAtMost(240) }
                }
                Spacer(Modifier.height(18.dp))
                PrimaryPillButton(
                    text = "完成",
                    modifier = Modifier.fillMaxWidth()
                ) { onApply(bpm) }
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onDismiss) { Text("取消", color = InkSoft) }
            }
        }
    }
}

@Composable
private fun StepIcon(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .border(1.dp, Divider, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 21.sp, color = InkSoft)
    }
}

@Composable
private fun CompletionScreen(playback: PlaybackState, onDone: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Cream, Background)))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(70.dp))
            Box(
                modifier = Modifier
                    .size(108.dp)
                    .clip(CircleShape)
                    .background(Orange),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(58.dp))
            }
            Spacer(Modifier.height(26.dp))
            Text(
                "训练完成",
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp),
                color = Ink
            )
            Text(
                "每一步都算数，做得好！",
                style = MaterialTheme.typography.bodyLarge,
                color = InkSoft,
                modifier = Modifier.padding(top = 6.dp)
            )
            Spacer(Modifier.height(32.dp))
            PaceCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconCircle(Icons.Rounded.Timer, Orange, OrangeSoft, size = 42, iconSize = 21)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("本次用时", style = MaterialTheme.typography.bodyMedium, color = InkSoft)
                        Text(
                            formatTotal(
                                if (playback.actualElapsedMs > 0L) {
                                    (playback.actualElapsedMs / 1000L).toInt()
                                } else {
                                    playback.totalSeconds
                                }
                            ),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                            color = Ink
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("训练分段", style = MaterialTheme.typography.bodyMedium, color = InkSoft)
                        Text(
                            "${playback.segments.size} 段",
                            style = MaterialTheme.typography.titleLarge,
                            color = Orange
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Divider)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    playback.planName,
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink
                )
                val weightedSeconds = playback.segments.sumOf { it.durationSec }
                val weightedSpm = if (weightedSeconds == 0) {
                    0
                } else {
                    (playback.segments.sumOf { it.spm * it.durationSec }.toDouble() / weightedSeconds).roundToInt()
                }
                Text(
                    "计划 ${formatTotal(playback.totalSeconds)} · 平均步频 $weightedSpm SPM",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSoft
                )
            }
            Spacer(Modifier.height(28.dp))
            PrimaryPillButton(
                text = "完成",
                modifier = Modifier.fillMaxWidth()
            ) { onDone() }
            Spacer(Modifier.height(24.dp))
        }
    }
}
