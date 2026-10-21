package com.wl.pacebeat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wl.pacebeat.BuildConfig
import com.wl.pacebeat.data.PlayerSettings
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    settings: PlayerSettings,
    onDismiss: () -> Unit,
    onOpenSound: () -> Unit,
    onVolume: (Float) -> Unit,
    onMultiplier: (Int) -> Unit,
    onVoice: (Boolean) -> Unit
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
                .padding(bottom = 18.dp)
        ) {
            Text("设置", style = MaterialTheme.typography.headlineLarge, color = Ink)
            Text(
                "纯节拍器会与其它音乐共存，不会抢占音频焦点。",
                style = MaterialTheme.typography.bodyMedium,
                color = InkSoft,
                modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
            )

            PaceCard(padding = PaddingValues(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenSound),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconCircle(Icons.Rounded.MusicNote, Orange, OrangeSoft, size = 36, iconSize = 18)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("节拍音乐", style = MaterialTheme.typography.titleMedium, color = Ink)
                        Text(
                            "1/${settings.beatsPerBar} · ${settings.sound.title}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkFaint
                        )
                    }
                    Text("›", style = MaterialTheme.typography.headlineMedium, color = InkFaint)
                }
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconCircle(Icons.Rounded.VolumeUp, Ink, Panel, size = 36, iconSize = 18)
                    Spacer(Modifier.width(12.dp))
                    Text("音量", style = MaterialTheme.typography.titleMedium, color = Ink)
                    Spacer(Modifier.weight(1f))
                    Text(
                        "${(settings.volume * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = Orange
                    )
                }
                PaceSlider(
                    value = settings.volume,
                    onValueChange = onVolume,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp)
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("音量增强", style = MaterialTheme.typography.titleMedium, color = Ink)
                    Spacer(Modifier.weight(1f))
                    SegmentedNumbers(
                        values = listOf(1, 2, 3, 4),
                        selected = settings.multiplier,
                        suffix = "x",
                        modifier = Modifier.width(190.dp)
                    ) { onMultiplier(it) }
                }
                Spacer(Modifier.height(14.dp))
                SwitchRow(
                    icon = Icons.Rounded.Headphones,
                    title = "语音提示",
                    checked = settings.voice,
                    onCheckedChange = onVoice
                )
            }

            Spacer(Modifier.height(14.dp))

            PaceCard(padding = PaddingValues(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Orange),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.GraphicEq, null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.width(13.dp))
                    Column {
                        Text("PaceBeat", style = MaterialTheme.typography.titleLarge, color = Ink)
                        Text(
                            "版本 ${BuildConfig.VERSION_NAME} · 离线运行",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkFaint
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    "所有训练计划与偏好只保存在这台设备上。应用不申请网络权限，也不会上传任何训练数据。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSoft
                )
            }

            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(OrangeSoft)
                    .padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(Icons.Rounded.Info, null, tint = Orange, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(9.dp))
                Text(
                    "跑步时请保持适中音量，留意周围环境；如感到不适请立即停止并休息。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSoft
                )
            }

            Spacer(Modifier.height(6.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("完成", color = InkSoft, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
