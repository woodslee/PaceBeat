package com.wl.pacebeat.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ChangeHistory
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wl.pacebeat.data.PlanType
import com.wl.pacebeat.data.SoundProfile
import com.wl.pacebeat.ui.theme.CardWhite
import com.wl.pacebeat.ui.theme.Divider
import com.wl.pacebeat.ui.theme.Ink
import com.wl.pacebeat.ui.theme.InkFaint
import com.wl.pacebeat.ui.theme.InkSoft
import com.wl.pacebeat.ui.theme.Orange
import com.wl.pacebeat.ui.theme.OrangeSoft
import com.wl.pacebeat.ui.theme.Panel
import com.wl.pacebeat.ui.theme.Track

fun Modifier.dashedBorder(
    color: Color,
    cornerRadius: Dp = 24.dp,
    strokeWidth: Dp = 2.dp,
    dash: Dp = 9.dp,
    gap: Dp = 6.dp
): Modifier = drawBehind {
    val stroke = Stroke(
        width = strokeWidth.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash.toPx(), gap.toPx()), 0f)
    )
    drawRoundRect(
        color = color,
        topLeft = Offset(stroke.width / 2f, stroke.width / 2f),
        size = Size(size.width - stroke.width, size.height - stroke.width),
        cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
        style = stroke
    )
}

@Composable
fun PaceCard(
    modifier: Modifier = Modifier,
    color: Color = CardWhite,
    borderColor: Color? = null,
    corner: Int = 22,
    padding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(corner.dp)
    Column(
        modifier = modifier
            .shadow(
                elevation = 12.dp,
                shape = shape,
                clip = false,
                ambientColor = Color(0x0B000000),
                spotColor = Color(0x10000000)
            )
            .clip(shape)
            .background(color)
            .then(if (borderColor != null) Modifier.border(1.dp, borderColor, shape) else Modifier)
            .padding(padding),
        content = content
    )
}

@Composable
fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    trailing: String? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = Ink,
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelMedium,
                color = InkFaint
            )
        }
    }
}

@Composable
fun IconCircle(
    icon: ImageVector,
    tint: Color,
    background: Color,
    size: Int = 44,
    iconSize: Int = 22
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize.dp)
        )
    }
}

@Composable
fun Badge(text: String, color: Color = Panel, textColor: Color = InkSoft) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = textColor,
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(color)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
fun DashedAction(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .height(58.dp)
            .clip(RoundedCornerShape(18.dp))
            .dashedBorder(Divider, cornerRadius = 18.dp, strokeWidth = 2.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, tint = InkSoft, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(7.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = InkSoft)
    }
}

@Composable
fun PrimaryPillButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(62.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Orange,
            contentColor = Color.White,
            disabledContainerColor = Track,
            disabledContentColor = InkFaint
        ),
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        if (icon != null) {
            Icon(icon, null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
            letterSpacing = 0.8.sp
        )
    }
}

@Composable
fun SegmentedNumbers(
    values: List<Int>,
    selected: Int,
    modifier: Modifier = Modifier,
    suffix: String = "",
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Panel)
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        values.forEach { value ->
            val active = value == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (active) CardWhite else Color.Transparent)
                    .clickable { onSelect(value) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$value$suffix",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (active) Ink else InkSoft
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaceSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f
) {
    Slider(
        value = value.coerceIn(valueRange.start, valueRange.endInclusive),
        onValueChange = onValueChange,
        valueRange = valueRange,
        modifier = modifier,
        thumb = {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Ink)
            )
        },
        track = { sliderState ->
            val span = (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.0001f)
            val fraction = ((sliderState.value - valueRange.start) / span).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(CircleShape)
                    .background(Track)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(Orange)
                )
            }
        },
        colors = SliderDefaults.colors(
            thumbColor = Ink,
            activeTrackColor = Orange,
            inactiveTrackColor = Track,
            activeTickColor = Color.Transparent,
            inactiveTickColor = Color.Transparent
        )
    )
}

@Composable
fun SoundRow(profile: SoundProfile, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardWhite)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(profile.title, style = MaterialTheme.typography.titleMedium, color = Ink)
            Text(profile.detail, style = MaterialTheme.typography.bodyMedium, color = InkFaint)
        }
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Orange)
        )
    }
}

@Composable
fun ValueStepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    min: Int = 100,
    max: Int = 240,
    suffix: String = ""
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StepButton("-", enabled = value > min) { onValueChange((value - 1).coerceAtLeast(min)) }
        Text(
            text = "$value$suffix",
            style = MaterialTheme.typography.titleLarge,
            color = Orange,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(70.dp)
        )
        StepButton("+", enabled = value < max) { onValueChange((value + 1).coerceAtMost(max)) }
    }
}

@Composable
private fun StepButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .border(1.dp, if (enabled) Divider else Track, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = if (enabled) InkSoft else InkFaint
        )
    }
}

@Composable
fun BeatDots(
    active: Int,
    modifier: Modifier = Modifier,
    count: Int = 4
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { index ->
            val accent = index == 0
            val isActive = index == active
            Box(
                modifier = Modifier
                    .height(6.dp)
                    .width(if (isActive) 24.dp else 10.dp)
                    .clip(CircleShape)
                    .background(if (isActive) Orange else if (accent) Orange.copy(alpha = 0.24f) else Track)
            )
        }
    }
}

@Composable
fun SwitchRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconCircle(icon, Orange, OrangeSoft, size = 34, iconSize = 18)
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Ink,
            modifier = Modifier.weight(1f)
        )
        androidx.compose.material3.Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Orange,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Track,
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

fun PlanType.icon(): ImageVector = when (this) {
    PlanType.BLANK -> Icons.Rounded.Add
    PlanType.EASY -> Icons.Rounded.DirectionsRun
    PlanType.AEROBIC -> Icons.Rounded.Favorite
    PlanType.PROGRESSIVE -> Icons.Rounded.ShowChart
    PlanType.TEMPO -> Icons.Rounded.Speed
    PlanType.INTERVALS -> Icons.Rounded.Bolt
    PlanType.PYRAMID -> Icons.Rounded.ChangeHistory
    PlanType.FARTLEK -> Icons.Rounded.Shuffle
}

fun PlanType.tint(): Color = Color(color)

@Composable
fun SimpleTopBar(
    title: String,
    subtitle: String? = null,
    onBack: () -> Unit,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            onClick = onBack,
            shape = CircleShape,
            color = CardWhite,
            border = BorderStroke(1.dp, Divider),
            modifier = Modifier.size(42.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("‹", fontSize = 28.sp, color = Ink, modifier = Modifier.padding(bottom = 4.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = Ink)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = InkSoft)
            }
        }
        trailing?.invoke()
    }
}

val CardBorder = Color(0xFFEDEDEF)
