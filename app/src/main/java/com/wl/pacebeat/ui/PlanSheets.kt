package com.wl.pacebeat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wl.pacebeat.data.PlanConfig
import com.wl.pacebeat.data.PlanPresets
import com.wl.pacebeat.data.PlanType
import com.wl.pacebeat.data.Segment
import com.wl.pacebeat.data.SoundCategory
import com.wl.pacebeat.data.SoundProfile
import com.wl.pacebeat.data.defaultConfig
import com.wl.pacebeat.data.formatClock
import com.wl.pacebeat.ui.theme.Background
import com.wl.pacebeat.ui.theme.CardWhite
import com.wl.pacebeat.ui.theme.Divider
import com.wl.pacebeat.ui.theme.Ink
import com.wl.pacebeat.ui.theme.InkFaint
import com.wl.pacebeat.ui.theme.InkSoft
import com.wl.pacebeat.ui.theme.Orange
import com.wl.pacebeat.ui.theme.OrangeSoft
import com.wl.pacebeat.ui.theme.Panel
import com.wl.pacebeat.ui.theme.Track
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanTypeChooserSheet(
    includeBlank: Boolean,
    onPick: (PlanType) -> Unit,
    onDismiss: () -> Unit
) {
    val types = remember(includeBlank) {
        PlanType.entries.filter { includeBlank || it != PlanType.BLANK }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Background,
        contentColor = Ink,
        dragHandle = { androidx.compose.material3.BottomSheetDefaults.DragHandle(color = Track) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 18.dp)
        ) {
            Text("选择训练类型", style = MaterialTheme.typography.headlineMedium, color = Ink)
            Text(
                "从模板快速开始，或创建自己的空白计划",
                style = MaterialTheme.typography.bodyMedium,
                color = InkSoft,
                modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
            )
            types.chunked(2).forEach { rowTypes ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowTypes.forEach { type ->
                        TypeTile(type = type, modifier = Modifier.weight(1f)) { onPick(type) }
                    }
                    if (rowTypes.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(2.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("取消", color = InkSoft, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun TypeTile(type: PlanType, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(CardWhite)
            .border(1.dp, Divider, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(type.tint().copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(type.icon(), null, tint = type.tint(), modifier = Modifier.size(23.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(type.label, style = MaterialTheme.typography.titleMedium, color = Ink)
        Text(type.subtitle, style = MaterialTheme.typography.bodyMedium, color = InkFaint)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanConfigSheet(
    type: PlanType,
    onApply: (PlanConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var config by remember(type) { mutableStateOf(defaultConfig(type)) }
    val total = PlanPresets.totalSeconds(type, config)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Background,
        contentColor = Ink,
        dragHandle = { androidx.compose.material3.BottomSheetDefaults.DragHandle(color = Track) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 22.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(type.tint().copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(type.icon(), null, tint = type.tint(), modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(type.label, style = MaterialTheme.typography.headlineMedium, color = Ink)
                    Text(type.subtitle, style = MaterialTheme.typography.bodyMedium, color = InkSoft)
                }
            }

            Spacer(Modifier.height(20.dp))

            PaceCard(padding = PaddingValues(16.dp)) {
                if (type == PlanType.INTERVALS) {
                    NumberSetting("重复组数", config.repeats, 2, 10, 1, " 组") {
                        config = config.copy(repeats = it)
                    }
                    Spacer(Modifier.height(14.dp))
                    NumberSetting("冲刺时长", config.fastSeconds, 20, 300, 10, " 秒") {
                        config = config.copy(fastSeconds = it)
                    }
                    Spacer(Modifier.height(14.dp))
                    NumberSetting("恢复时长", config.recoverySeconds, 20, 300, 10, " 秒") {
                        config = config.copy(recoverySeconds = it)
                    }
                } else {
                    NumberSetting("主训练时长", config.mainMinutes, 5, 60, 1, " 分钟") {
                        config = config.copy(mainMinutes = it)
                    }
                }
                Spacer(Modifier.height(18.dp))
                SettingSliderRow(
                    title = if (type == PlanType.INTERVALS) "恢复步频" else "目标步频",
                    value = config.spm,
                    range = 100..240,
                    onChange = { config = config.copy(spm = it, peakSpm = maxOf(it, config.peakSpm)) }
                )
                if (type == PlanType.INTERVALS || type == PlanType.PROGRESSIVE ||
                    type == PlanType.PYRAMID || type == PlanType.FARTLEK
                ) {
                    Spacer(Modifier.height(14.dp))
                    SettingSliderRow(
                        title = if (type == PlanType.INTERVALS) "冲刺步频" else "最高步频",
                        value = config.peakSpm,
                        range = maxOf(100, config.spm)..240,
                        onChange = { config = config.copy(peakSpm = it) }
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CheckLine("热身 5 分钟", config.warmup, Modifier.weight(1f)) {
                        config = config.copy(warmup = it)
                    }
                    CheckLine("冷身 5 分钟", config.cooldown, Modifier.weight(1f)) {
                        config = config.copy(cooldown = it)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            PaceCard(color = OrangeSoft, padding = PaddingValues(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "训练总时长",
                        style = MaterialTheme.typography.labelLarge,
                        color = InkSoft,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        formatClock(total),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = Orange
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    PlanPresets.summary(type, config),
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSoft
                )
            }

            Spacer(Modifier.height(18.dp))
            PrimaryPillButton(
                text = "应用计划",
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Rounded.Add
            ) { onApply(config) }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("取消", color = InkSoft, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun NumberSetting(
    title: String,
    value: Int,
    min: Int,
    max: Int,
    step: Int,
    suffix: String,
    onChange: (Int) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = InkSoft,
            modifier = Modifier.weight(1f)
        )
        NumberStepper(value, min, max, step, suffix, onChange)
    }
}

@Composable
fun NumberStepper(
    value: Int,
    min: Int,
    max: Int,
    step: Int,
    suffix: String,
    onChange: (Int) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StepCircle("+", value < max) { onChange((value + step).coerceAtMost(max)) }
        Text(
            "$value$suffix",
            style = MaterialTheme.typography.titleMedium,
            color = Ink,
            modifier = Modifier.width(74.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        StepCircle("−", value > min) { onChange((value - step).coerceAtLeast(min)) }
    }
}

@Composable
private fun StepCircle(label: String, enabled: Boolean, onClick: () -> Unit) {
    val scope = rememberCoroutineScope()
    val latestOnClick by rememberUpdatedState(onClick)
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .border(1.dp, if (enabled) Divider else Track, CircleShape)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        latestOnClick()
                        val repeat = scope.launch {
                            delay(420)
                            while (true) {
                                latestOnClick()
                                delay(85)
                            }
                        }
                        tryAwaitRelease()
                        repeat.cancel()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 20.sp, color = if (enabled) InkSoft else InkFaint)
    }
}

@Composable
private fun SettingSliderRow(
    title: String,
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = InkSoft)
        Spacer(Modifier.weight(1f))
        Text(
            "$value SPM",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Orange
        )
    }
    PaceSlider(
        value = value.toFloat(),
        onValueChange = { onChange(it.toInt()) },
        valueRange = range.first.toFloat()..range.last.toFloat(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp)
    )
}

@Composable
private fun CheckLine(label: String, checked: Boolean, modifier: Modifier = Modifier, onChange: (Boolean) -> Unit) {
    Row(
        modifier = modifier.clickable { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Orange,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Track,
                uncheckedBorderColor = Color.Transparent
            ),
            modifier = Modifier.size(width = 44.dp, height = 28.dp)
        )
        Spacer(Modifier.width(7.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = InkSoft)
    }
}

@Composable
fun BlankNameDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardWhite,
        shape = RoundedCornerShape(26.dp),
        title = {
            Text("新建计划", style = MaterialTheme.typography.headlineMedium, color = Ink)
        },
        text = {
            Column {
                Text(
                    "先给计划起个名字，然后自由添加训练分段。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSoft
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(24) },
                    placeholder = { Text("例如：周末 LSD", color = InkFaint) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Orange,
                        unfocusedBorderColor = Divider,
                        focusedTextColor = Ink,
                        unfocusedTextColor = Ink,
                        cursorColor = Orange
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
                enabled = name.isNotBlank()
            ) {
                Text("创建", color = if (name.isNotBlank()) Orange else InkFaint, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = InkSoft) }
        }
    )
}

@Composable
fun SegmentEditorDialog(
    initial: Segment?,
    onSave: (Segment) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    val initialDuration = initial?.durationSec ?: 300
    var minutes by remember { mutableStateOf(((initialDuration + 59) / 60).coerceIn(1, 90)) }
    var spm by remember { mutableStateOf(initial?.spm ?: 165) }
    val durationSec = minutes * 60
    val valid = name.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardWhite,
        shape = RoundedCornerShape(26.dp),
        title = {
            Text(if (initial == null) "添加分段" else "编辑分段", style = MaterialTheme.typography.headlineMedium, color = Ink)
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(28) },
                    label = { Text("分段名称", color = InkSoft) },
                    placeholder = { Text("例如：有氧巡航", color = InkFaint) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Orange,
                        unfocusedBorderColor = Divider,
                        focusedTextColor = Ink,
                        unfocusedTextColor = Ink,
                        cursorColor = Orange
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Text("时长（分钟）", style = MaterialTheme.typography.labelLarge, color = InkSoft)
                Spacer(Modifier.height(8.dp))
                NumberStepper(minutes, 1, 90, 1, " 分钟") { minutes = it }
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("目标步频", style = MaterialTheme.typography.labelLarge, color = InkSoft)
                    Spacer(Modifier.weight(1f))
                    Text(
                        "$spm SPM",
                        style = MaterialTheme.typography.titleMedium,
                        color = Orange,
                        fontWeight = FontWeight.Bold
                    )
                }
                PaceSlider(
                    value = spm.toFloat(),
                    onValueChange = { spm = it.toInt() },
                    valueRange = 100f..240f,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "建议：轻松跑 155–170，节奏跑 170–185，间歇冲刺 185–200。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkFaint
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (!valid) return@TextButton
                onSave(
                    Segment(
                        id = initial?.id ?: java.util.UUID.randomUUID().toString(),
                        name = name.trim(),
                        durationSec = durationSec,
                        spm = spm
                    )
                )
            }, enabled = valid) {
                Text("保存", color = if (valid) Orange else InkFaint, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = InkSoft) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundPickerSheet(
    selected: SoundProfile,
    beatsPerBar: Int,
    onSelect: (SoundProfile) -> Unit,
    onBeatsChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Background,
        contentColor = Ink,
        dragHandle = { androidx.compose.material3.BottomSheetDefaults.DragHandle(color = Track) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
        ) {
            Text("节拍音乐", style = MaterialTheme.typography.headlineMedium, color = Ink)
            Text(
                "节拍数决定重音循环。所有音色均保存在本地。",
                style = MaterialTheme.typography.bodyMedium,
                color = InkSoft,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            PaceCard(padding = PaddingValues(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("节拍数", style = MaterialTheme.typography.titleMedium, color = Ink)
                    Spacer(Modifier.weight(1f))
                    Text(
                        "1/$beatsPerBar",
                        style = MaterialTheme.typography.labelLarge,
                        color = Orange
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(2, 3, 4).forEach { beats ->
                        val active = beats == beatsPerBar
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (active) Orange else Panel)
                                .clickable { onBeatsChange(beats) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "1/$beats",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (active) Color.White else InkSoft
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("节拍音色", style = MaterialTheme.typography.titleLarge, color = Ink)
            Spacer(Modifier.height(10.dp))
            SoundCategory.entries.forEach { category ->
                Text(
                    category.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = InkSoft,
                    modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)
                )
            SoundProfile.entries.filter { it.category == category }.forEach { profile ->
                val active = profile.id == selected.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (active) OrangeSoft else CardWhite)
                        .border(
                            width = if (active) 1.5.dp else 1.dp,
                            color = if (active) Orange else Divider,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { onSelect(profile) }
                        .padding(horizontal = 15.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (active) Orange else Panel),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.GraphicEq,
                            null,
                            tint = if (active) Color.White else InkSoft,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            profile.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (active) Orange else Ink
                        )
                        Text(profile.detail, style = MaterialTheme.typography.bodyMedium, color = InkFaint)
                    }
                    if (active) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            tint = Orange,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
            }
        }
    }
}
