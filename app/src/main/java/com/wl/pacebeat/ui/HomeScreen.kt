package com.wl.pacebeat.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.core.content.ContextCompat
import com.wl.pacebeat.data.PlanType
import com.wl.pacebeat.data.Segment
import com.wl.pacebeat.data.WorkoutPlan
import com.wl.pacebeat.data.formatRemaining
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
fun HomeScreen(
    vm: AppViewModel,
    playback: PlaybackState
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val plan = vm.plan
    val settings = vm.settings
    val sessionActive = playback.active && !playback.completed
    val listState = rememberLazyListState()
    val reorderScope = rememberCoroutineScope()
    val density = LocalDensity.current
    var draggingSegmentId by remember { mutableStateOf<String?>(null) }
    var draggedIndex by remember { mutableIntStateOf(-1) }
    var segmentDragOffset by remember { mutableFloatStateOf(0f) }
    var dragStepPx by remember { mutableFloatStateOf(0f) }

    fun startReorder(segment: Segment) {
        if (sessionActive) return
        val index = plan.segments.indexOfFirst { it.id == segment.id }
        if (index < 0) return
        draggingSegmentId = segment.id
        draggedIndex = index
        segmentDragOffset = 0f
        val visible = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
        val spacing = with(density) { 12.dp.toPx() }
        dragStepPx = (visible?.size?.toFloat() ?: with(density) { 82.dp.toPx() }) + spacing
    }

    fun dragReorder(delta: Float) {
        if (draggingSegmentId == null) return
        segmentDragOffset += delta
        if (dragStepPx <= 0f) return
        val shift = (segmentDragOffset / dragStepPx).roundToInt()
        if (shift == 0) return
        val from = draggedIndex
        val to = (from + shift).coerceIn(0, plan.segments.lastIndex)
        if (to == from) return
        vm.moveSegment(from, to)
        draggedIndex = to
        segmentDragOffset -= (to - from) * dragStepPx
        val targetVisible = listState.layoutInfo.visibleItemsInfo.any { it.index == to }
        if (!targetVisible) {
            reorderScope.launch { listState.animateScrollToItem(to) }
        }
    }

    fun endReorder() {
        draggingSegmentId = null
        draggedIndex = -1
        segmentDragOffset = 0f
        dragStepPx = 0f
    }

    var showTypePicker by remember { mutableStateOf(false) }
    var pickerIncludesBlank by remember { mutableStateOf(false) }
    var configType by remember { mutableStateOf<PlanType?>(null) }
    var showBlankName by remember { mutableStateOf(false) }
    var showAddSegment by remember { mutableStateOf(false) }
    var editingSegment by remember { mutableStateOf<Segment?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showSoundPicker by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(context, "未授予通知权限，后台播放时可能看不到控制通知", Toast.LENGTH_LONG).show()
        }
        vm.startPlayback(context)
    }

    fun requestStart() {
        if (sessionActive) {
            vm.openRun()
            return
        }
        if (!plan.hasContent) {
            Toast.makeText(context, "请先添加至少一个训练分段", Toast.LENGTH_SHORT).show()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            vm.startPlayback(context)
        }
    }

    fun guardRunning(action: () -> Unit) {
        if (sessionActive) {
            Toast.makeText(context, "训练进行中，请先结束当前训练", Toast.LENGTH_SHORT).show()
        } else {
            action()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Cream, Background, Background),
                    startY = 0f,
                    endY = 900f
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "PaceBeat",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1.2).sp
                        ),
                        color = Ink
                    )
                    Spacer(Modifier.width(9.dp))
                    Badge("RUN", OrangeSoft, Orange)
                }
                Text(
                    "跟着节拍，跑出你的节奏",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSoft,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Surface(
                onClick = { showSettings = true },
                shape = CircleShape,
                color = CardWhite,
                border = BorderStroke(1.dp, Divider),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Settings, "设置", tint = Ink, modifier = Modifier.size(22.dp))
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .height(44.dp)
                    .clip(CircleShape)
                    .background(OrangeSoft)
                    .border(1.5.dp, Orange, CircleShape)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("今日训练", style = MaterialTheme.typography.labelLarge, color = Orange)
            }
            Row(
                modifier = Modifier
                    .height(44.dp)
                    .clip(CircleShape)
                    .dashedBorder(Divider, cornerRadius = 22.dp)
                    .clickable {
                        guardRunning {
                            pickerIncludesBlank = true
                            showTypePicker = true
                        }
                    }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Add, null, tint = InkSoft, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text("新建计划", style = MaterialTheme.typography.labelLarge, color = InkSoft)
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("训练分段", style = MaterialTheme.typography.titleLarge, color = Ink)
            Text(
                "  ·  ${plan.totalLabel}",
                style = MaterialTheme.typography.titleMedium,
                color = InkFaint
            )
            Spacer(Modifier.weight(1f))
            if (plan.segments.size > 1) {
                Text("拖动排序 · 左滑删除", style = MaterialTheme.typography.labelSmall, color = InkFaint)
            }
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (plan.segments.isEmpty()) {
                item(key = "empty") {
                    EmptyPlanCard(onAdd = {
                        guardRunning { showAddSegment = true }
                    })
                }
            }

            itemsIndexed(plan.segments, key = { _, segment -> segment.id }) { _, segment ->
                SegmentCard(
                    segment = segment,
                    dragOffsetY = if (draggingSegmentId == segment.id) segmentDragOffset else 0f,
                    isDragging = draggingSegmentId == segment.id,
                    dragEnabled = !sessionActive,
                    onReorderStart = { startReorder(segment) },
                    onReorder = { dragReorder(it) },
                    onReorderEnd = { endReorder() },
                    onEdit = {
                        guardRunning { editingSegment = segment }
                    },
                    onDelete = {
                        guardRunning {
                            vm.deleteSegment(segment)
                            Toast.makeText(context, "已删除「${segment.name}」", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            item(key = "actions") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DashedAction(
                        text = "添加分段",
                        icon = Icons.Rounded.Add,
                        modifier = Modifier.weight(1f)
                    ) {
                        guardRunning { showAddSegment = true }
                    }
                    DashedAction(
                        text = "更换计划",
                        icon = Icons.Rounded.GraphicEq,
                        modifier = Modifier.weight(1f)
                    ) {
                        guardRunning {
                            pickerIncludesBlank = false
                            showTypePicker = true
                        }
                    }
                }
            }

            if (sessionActive) {
                item(key = "session") {
                    ActiveSessionCard(
                        playback = playback,
                        onOpen = { vm.openRun() }
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.Headphones,
                null,
                tint = InkFaint,
                modifier = Modifier.size(17.dp)
            )
            Spacer(Modifier.width(7.dp))
            Text(
                "建议佩戴耳机，并保持对环境音的感知",
                style = MaterialTheme.typography.labelMedium,
                color = InkFaint
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BeatsButton { showSettings = true }
            Spacer(Modifier.width(14.dp))
            PrimaryPillButton(
                text = when {
                    sessionActive && playback.paused -> "继续训练"
                    sessionActive -> "返回训练"
                    else -> "开始训练"
                },
                icon = Icons.Rounded.PlayArrow,
                enabled = plan.hasContent || sessionActive,
                modifier = Modifier.weight(1f),
                onClick = { requestStart() }
            )
        }
    }

    if (showSettings) {
        SettingsSheet(
            settings = settings,
            onDismiss = { showSettings = false },
            onOpenSound = {
                showSettings = false
                showSoundPicker = true
            },
            onVolume = { vm.setVolume(context, it) },
            onMultiplier = { vm.setMultiplier(context, it) },
            onVoice = { vm.setVoice(context, it) }
        )
    }

    if (showSoundPicker) {
        SoundPickerSheet(
            selected = settings.sound,
            beatsPerBar = settings.beatsPerBar,
            onSelect = {
                vm.setSound(context, it)
                showSoundPicker = false
            },
            onBeatsChange = { vm.setBeatsPerBar(context, it) },
            onDismiss = { showSoundPicker = false }
        )
    }

    if (showTypePicker) {
        PlanTypeChooserSheet(
            includeBlank = pickerIncludesBlank,
            onPick = { type ->
                showTypePicker = false
                if (type == PlanType.BLANK) {
                    showBlankName = true
                } else {
                    configType = type
                }
            },
            onDismiss = { showTypePicker = false }
        )
    }

    configType?.let { type ->
        PlanConfigSheet(
            type = type,
            onApply = { config ->
                vm.applyPreset(type, config)
                configType = null
                Toast.makeText(context, "已应用「${type.label}」计划", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { configType = null }
        )
    }

    if (showBlankName) {
        BlankNameDialog(
            onConfirm = { name ->
                vm.applyPlan(WorkoutPlan(name = name, type = PlanType.BLANK, segments = emptyList()))
                showBlankName = false
                Toast.makeText(context, "计划已创建，开始添加分段吧", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showBlankName = false }
        )
    }

    if (showAddSegment) {
        SegmentEditorDialog(
            initial = null,
            onSave = {
                vm.addSegment(it)
                showAddSegment = false
            },
            onDismiss = { showAddSegment = false }
        )
    }

    editingSegment?.let { segment ->
        SegmentEditorDialog(
            initial = segment,
            onSave = {
                vm.updateSegment(it)
                editingSegment = null
            },
            onDismiss = { editingSegment = null }
        )
    }
}

@Composable
private fun SegmentCard(
    segment: Segment,
    dragOffsetY: Float = 0f,
    isDragging: Boolean = false,
    dragEnabled: Boolean = true,
    onReorderStart: () -> Unit = {},
    onReorder: (Float) -> Unit = {},
    onReorderEnd: () -> Unit = {},
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val density = LocalDensity.current
    val actionWidthPx = with(density) { 98.dp.toPx() }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    var settleTargetPx by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    val animatedOffset by animateFloatAsState(
        targetValue = if (dragging) dragOffsetPx else settleTargetPx,
        animationSpec = tween(durationMillis = if (dragging) 0 else 200),
        label = "segmentSwipeOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(0, dragOffsetY.roundToInt()) }
            .zIndex(if (isDragging) 2f else 0f)
            .clip(RoundedCornerShape(22.dp))
    ) {
        if (animatedOffset < -0.5f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Danger)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(98.dp)
                            .clickable {
                                dragOffsetPx = 0f
                                settleTargetPx = 0f
                                onDelete()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Rounded.DeleteOutline,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(23.dp)
                            )
                            Spacer(Modifier.height(3.dp))
                            Text("删除", color = Color.White, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        PaceCard(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .pointerInput(actionWidthPx) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            dragging = true
                            dragOffsetPx = animatedOffset.coerceIn(-actionWidthPx, 0f)
                            settleTargetPx = dragOffsetPx
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            dragOffsetPx = (dragOffsetPx + dragAmount).coerceIn(-actionWidthPx, 0f)
                        },
                        onDragEnd = {
                            dragging = false
                            settleTargetPx = if (dragOffsetPx < -actionWidthPx * 0.38f) {
                                -actionWidthPx
                            } else {
                                0f
                            }
                        },
                        onDragCancel = {
                            dragging = false
                            settleTargetPx = if (dragOffsetPx < -actionWidthPx * 0.5f) {
                                -actionWidthPx
                            } else {
                                0f
                            }
                        }
                    )
                }
                .clickable {
                    if (settleTargetPx < 0f || animatedOffset < -actionWidthPx * 0.4f) {
                        dragOffsetPx = 0f
                        settleTargetPx = 0f
                    } else {
                        onEdit()
                    }
                },
            padding = PaddingValues(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        segment.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Ink
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "时长 ${segment.durationLabel}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSoft
                    )
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "${segment.spm}",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1).sp
                        ),
                        color = Orange
                    )
                    Text(
                        "SPM",
                        style = MaterialTheme.typography.labelMedium,
                        color = InkFaint,
                        modifier = Modifier.padding(start = 3.dp, bottom = 4.dp)
                    )
                }
                Spacer(Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .pointerInput(dragEnabled, segment.id) {
                            if (!dragEnabled) return@pointerInput
                            detectDragGestures(
                                onDragStart = { onReorderStart() },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    onReorder(dragAmount.y)
                                },
                                onDragEnd = { onReorderEnd() },
                                onDragCancel = { onReorderEnd() }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.DragHandle,
                        contentDescription = "拖动排序",
                        tint = if (dragEnabled) InkFaint else InkFaint.copy(alpha = 0.25f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyPlanCard(onAdd: () -> Unit) {
    PaceCard(
        color = OrangeSoft,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconCircle(Icons.Rounded.GraphicEq, Orange, Color.White, size = 46, iconSize = 24)
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text("还没有训练分段", style = MaterialTheme.typography.titleMedium, color = Ink)
                Text(
                    "添加分段，或从模板生成一份完整计划。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSoft
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        PrimaryPillButton(
            text = "添加第一个分段",
            icon = Icons.Rounded.Add,
            modifier = Modifier.fillMaxWidth(),
            onClick = onAdd
        )
    }
}

@Composable
private fun ActiveSessionCard(playback: PlaybackState, onOpen: () -> Unit) {
    PaceCard(
        color = Color(0xFF191919),
        padding = PaddingValues(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (playback.paused) Orange.copy(alpha = 0.5f) else Orange)
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (playback.paused) "训练已暂停" else "训练进行中",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    "${playback.current?.name ?: "训练"} · ${playback.bpm} SPM · 剩余 ${formatRemaining(playback.remainingMs)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.62f)
                )
            }
            Surface(
                onClick = onOpen,
                shape = CircleShape,
                color = Orange
            ) {
                Text(
                    "返回",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                )
            }
        }
    }
}

@Composable
private fun BeatsButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = CardWhite,
        border = BorderStroke(1.dp, Divider),
        modifier = Modifier
            .size(66.dp)
            .shadow(10.dp, CircleShape)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Rounded.GraphicEq, "节拍设置", tint = Ink, modifier = Modifier.size(23.dp))
            Text(
                "BEATS",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                color = InkSoft
            )
        }
    }
}
