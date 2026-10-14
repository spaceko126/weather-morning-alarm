package com.weathermorning.alarm.presentation.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.weathermorning.alarm.R
import java.time.LocalTime
import kotlin.math.abs
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

private val ITEM_HEIGHT = 68.dp
private const val VISIBLE_ITEMS = 5
private const val SIDE_ITEMS = VISIBLE_ITEMS / 2

/**
 * Repeats each range enough times to feel endless while staying a finite list,
 * so scrolling from 00 to 59 never hits a wall.
 */
private const val WHEEL_LOOPS = 400

@Composable
internal fun TimeWheelDialog(
    initialTime: LocalTime,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedHour by remember { mutableIntStateOf(initialTime.hour) }
    var selectedMinute by remember { mutableIntStateOf(initialTime.minute) }
    var manualEntry by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF090B10),
            contentColor = Color.White,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF111827),
                                Color(0xFF071C22),
                                Color(0xFF090B10),
                            ),
                        ),
                    ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    DialogHeader(
                        manualEntry = manualEntry,
                        onToggleMode = { manualEntry = !manualEntry },
                        onDismiss = onDismiss,
                        onSave = { onConfirm(LocalTime.of(selectedHour, selectedMinute)) },
                    )

                    if (manualEntry) {
                        ManualTimeEntry(
                            hour = selectedHour,
                            minute = selectedMinute,
                            onHourChange = { selectedHour = it },
                            onMinuteChange = { selectedMinute = it },
                        )
                    } else {
                        TimeWheels(
                            hour = selectedHour,
                            minute = selectedMinute,
                            onHourChange = { selectedHour = it },
                            onMinuteChange = { selectedMinute = it },
                        )
                    }

                    WeatherTimeFooter(
                        hour = selectedHour,
                        minute = selectedMinute,
                        onDismiss = onDismiss,
                        onSave = { onConfirm(LocalTime.of(selectedHour, selectedMinute)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeWheels(
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = stringResource(R.string.time_wheel_hint_v2),
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFB9C8D3),
            textAlign = TextAlign.Center,
        )
        Box(contentAlignment = Alignment.Center) {
            // Selection band sits behind both wheels so the centred numbers
            // read as one selected row rather than two separate highlights.
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ITEM_HEIGHT),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF18222B),
                border = BorderStroke(1.dp, Color(0xFFFFD6B0)),
                content = {},
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WheelColumn(
                    value = hour,
                    range = 0..23,
                    onValueChange = onHourChange,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = ":",
                    modifier = Modifier.width(36.dp),
                    fontSize = 44.sp,
                    lineHeight = 50.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFFD6B0),
                    textAlign = TextAlign.Center,
                )
                WheelColumn(
                    value = minute,
                    range = 0..59,
                    onValueChange = onMinuteChange,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun WheelColumn(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val size = range.last - range.first + 1
    val itemCount = size * WHEEL_LOOPS
    // Start in the middle so there is room to scroll both ways.
    val firstIndex = remember(range) { (itemCount / 2) - ((itemCount / 2) % size) }
    val listState = rememberLazyListState(firstIndex + (value - range.first))
    val flingBehavior = rememberSnapFlingBehavior(listState)
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // Derived from the layout rather than firstVisibleItemIndex, so the centre
    // slot is found the same way whatever the content padding does.
    val centeredIndex by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val centre = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo
                .minByOrNull { abs((it.offset + it.size / 2) - centre) }
                ?.index
                ?: listState.firstVisibleItemIndex
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { centeredIndex }
            .drop(1)
            .collect { index ->
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onValueChange(range.first + (index % size))
            }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.height(ITEM_HEIGHT * VISIBLE_ITEMS),
        flingBehavior = flingBehavior,
        // Padding of two rows puts index 0 in the centre slot, which makes
        // firstVisibleItemIndex the selected item once snapped.
        contentPadding = PaddingValues(vertical = ITEM_HEIGHT * SIDE_ITEMS),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items(itemCount) { index ->
            val itemValue = range.first + (index % size)
            val selected = index == centeredIndex
            Box(
                modifier = Modifier
                    .height(ITEM_HEIGHT)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        scope.launch { listState.animateScrollToItem(index) }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "%02d".format(itemValue),
                    fontSize = if (selected) 46.sp else 28.sp,
                    lineHeight = if (selected) 52.sp else 34.sp,
                    fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
                    color = if (selected) Color.White else Color(0xFF627180),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun ManualTimeEntry(
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TimeNumberField(
            value = hour,
            maxValue = 23,
            label = stringResource(R.string.time_input_hour),
            onValueChange = onHourChange,
            modifier = Modifier.weight(1f),
        )
        TimeNumberField(
            value = minute,
            maxValue = 59,
            label = stringResource(R.string.time_input_minute),
            onValueChange = onMinuteChange,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TimeNumberField(
    value: Int,
    maxValue: Int,
    label: String,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf("%02d".format(value)) }
    OutlinedTextField(
        value = text,
        onValueChange = { raw ->
            val digits = raw.filter(Char::isDigit).take(2)
            val parsed = digits.toIntOrNull()
            when {
                // Out of range snaps to the limit instead of being dropped, so
                // the field never shows a number the alarm did not take.
                parsed != null && parsed > maxValue -> {
                    text = "%02d".format(maxValue)
                    onValueChange(maxValue)
                }
                parsed != null -> {
                    text = digits
                    onValueChange(parsed)
                }
                else -> text = digits
            }
        },
        modifier = modifier,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = MaterialTheme.typography.displaySmall.copy(textAlign = TextAlign.Center),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Color(0xFFFFD6B0),
            unfocusedBorderColor = Color(0xFF334155),
            focusedLabelColor = Color(0xFFFFD6B0),
            unfocusedLabelColor = Color(0xFFB9C8D3),
            cursorColor = Color(0xFFFFD6B0),
        ),
    )
}

@Composable
private fun DialogHeader(
    manualEntry: Boolean,
    onToggleMode: () -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButtonLike(
                text = stringResource(R.string.cancel),
                onClick = onDismiss,
            )
            Text(
                text = stringResource(R.string.alarm_edit),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
            TextButtonLike(
                text = stringResource(R.string.save),
                onClick = onSave,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            TextButtonLike(
                text = stringResource(
                    if (manualEntry) R.string.time_input_wheel else R.string.time_input_manual,
                ),
                onClick = onToggleMode,
            )
        }
    }
}

@Composable
private fun WeatherTimeFooter(
    hour: Int,
    minute: Int,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF111827).copy(alpha = 0.94f),
        border = BorderStroke(1.dp, Color(0xFF263647)),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "%02d:%02d".format(hour, minute),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = Color.White,
            )
            Text(
                text = stringResource(R.string.weather_alarm_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFB9C8D3),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FooterAction(
                    text = stringResource(R.string.cancel),
                    emphasized = false,
                    modifier = Modifier.weight(1f),
                    onClick = onDismiss,
                )
                FooterAction(
                    text = stringResource(R.string.save),
                    emphasized = true,
                    modifier = Modifier.weight(1f),
                    onClick = onSave,
                )
            }
        }
    }
}

@Composable
private fun TextButtonLike(
    text: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent,
        contentColor = Color(0xFFFFD6B0),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun FooterAction(
    text: String,
    emphasized: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(54.dp),
        shape = RoundedCornerShape(8.dp),
        color = if (emphasized) Color(0xFFFFD6B0) else Color(0xFF1F2937),
        contentColor = if (emphasized) Color(0xFF251307) else Color.White,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
