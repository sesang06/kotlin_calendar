package com.kotlincalendar.client.presentation.event.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kotlincalendar.client.presentation.event.detail.formatKoreanDate
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.Instant

@Composable
fun EventCreateRoute(
    initialDate: LocalDate,
    onCreated: (eventId: String) -> Unit,
    onBack: () -> Unit,
    eventId: String? = null,
    onUpdated: (eventId: String) -> Unit = onCreated,
    viewModel: EventCreateViewModel = koinViewModel { parametersOf(initialDate, eventId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is EventCreateEffect.Created -> onCreated(effect.eventId)
                is EventCreateEffect.Updated -> onUpdated(effect.eventId)
                EventCreateEffect.NavigateBack -> onBack()
            }
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.onAction(EventCreateAction.DismissError)
        }
    }

    EventCreateScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction,
    )
}

private enum class PickerTarget { START_DATE, START_TIME, END_DATE, END_TIME }

@Composable
fun EventCreateScreen(
    state: EventCreateUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onAction: (EventCreateAction) -> Unit,
) {
    var picker by remember { mutableStateOf<PickerTarget?>(null) }
    val enabled = !state.isSaving && !state.isLoadingEvent

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(onClick = { onAction(EventCreateAction.Back) }) {
                        Text("취소")
                    }
                    Text(
                        text = if (state.isEditing) "일정 수정" else "새 일정",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    TextButton(onClick = { onAction(EventCreateAction.Save) }, enabled = enabled) {
                        Text("저장", fontWeight = FontWeight.SemiBold)
                    }
                }

                if (state.isLoadingEvent) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                FormSection {
                    OutlinedTextField(
                        value = state.title,
                        onValueChange = { onAction(EventCreateAction.ChangeTitle(it)) },
                        label = { Text("제목") },
                        placeholder = { Text("예: 팀 회의") },
                        singleLine = true,
                        isError = state.titleError != null,
                        supportingText = state.titleError?.let { error -> { Text(error) } },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = enabled,
                    )
                }

                FormSection {
                    DateTimeRow(
                        label = "시작",
                        date = state.startDate,
                        time = state.startTime,
                        enabled = enabled,
                        onDateClick = { picker = PickerTarget.START_DATE },
                        onTimeClick = { picker = PickerTarget.START_TIME },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    DateTimeRow(
                        label = "종료",
                        date = state.endDate,
                        time = state.endTime,
                        enabled = enabled,
                        onDateClick = { picker = PickerTarget.END_DATE },
                        onTimeClick = { picker = PickerTarget.END_TIME },
                    )
                    state.timeError?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                FormSection {
                    OutlinedTextField(
                        value = state.description,
                        onValueChange = { onAction(EventCreateAction.ChangeDescription(it)) },
                        label = { Text("설명 (선택)") },
                        minLines = 3,
                        isError = state.descriptionError != null,
                        supportingText = state.descriptionError?.let { error -> { Text(error) } },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = enabled,
                    )
                }

                Button(
                    onClick = { onAction(EventCreateAction.Save) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = MaterialTheme.shapes.medium,
                    enabled = enabled,
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(if (state.isEditing) "수정 완료" else "일정 저장")
                    }
                }
            }
        }
    }

    when (picker) {
        PickerTarget.START_DATE -> {
            DatePickerDialogFor(
                initial = state.startDate,
                onDismiss = { picker = null },
                onConfirm = {
                    onAction(EventCreateAction.ChangeStartDate(it))
                    picker = null
                },
            )
        }

        PickerTarget.END_DATE -> {
            DatePickerDialogFor(
                initial = state.endDate,
                onDismiss = { picker = null },
                onConfirm = {
                    onAction(EventCreateAction.ChangeEndDate(it))
                    picker = null
                },
            )
        }

        PickerTarget.START_TIME -> {
            TimePickerDialogFor(
                initial = state.startTime,
                onDismiss = { picker = null },
                onConfirm = {
                    onAction(EventCreateAction.ChangeStartTime(it))
                    picker = null
                },
            )
        }

        PickerTarget.END_TIME -> {
            TimePickerDialogFor(
                initial = state.endTime,
                onDismiss = { picker = null },
                onConfirm = {
                    onAction(EventCreateAction.ChangeEndTime(it))
                    picker = null
                },
            )
        }

        null -> {
            Unit
        }
    }
}

@Composable
private fun FormSection(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun DateTimeRow(
    label: String,
    date: LocalDate,
    time: LocalTime,
    enabled: Boolean,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(40.dp),
        )
        FilledTonalButton(
            onClick = onDateClick,
            enabled = enabled,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.weight(1f),
        ) {
            Text(formatKoreanDate(date), maxLines = 1)
        }
        FilledTonalButton(
            onClick = onTimeClick,
            enabled = enabled,
            shape = MaterialTheme.shapes.small,
        ) {
            Text(time.toHourMinute())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialogFor(
    initial: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    // Material3 DatePicker는 UTC 자정 기준 epoch millis를 쓴다.
    val pickerState =
        rememberDatePickerState(
            initialSelectedDateMillis = initial.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(),
        )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = pickerState.selectedDateMillis
                    if (millis == null) {
                        onDismiss()
                    } else {
                        onConfirm(Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date)
                    }
                },
            ) {
                Text("확인")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        },
    ) {
        DatePicker(state = pickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialogFor(
    initial: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
) {
    val pickerState =
        rememberTimePickerState(
            initialHour = initial.hour,
            initialMinute = initial.minute,
            is24Hour = true,
        )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime(pickerState.hour, pickerState.minute)) }) {
                Text("확인")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        },
        text = { TimePicker(state = pickerState) },
    )
}

private fun LocalTime.toHourMinute(): String = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
