package com.kotlincalendar.client.presentation.event.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlincalendar.client.domain.repository.EventRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class EventCreateViewModel(
    initialDate: LocalDate,
    private val eventRepository: EventRepository,
    clock: Clock = Clock.System,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel() {
    private val _state = MutableStateFlow(initialState(initialDate, clock.now()))
    val state: StateFlow<EventCreateUiState> = _state.asStateFlow()

    private val _effects = Channel<EventCreateEffect>(Channel.BUFFERED)
    val effects: Flow<EventCreateEffect> = _effects.receiveAsFlow()

    fun onAction(action: EventCreateAction) {
        when (action) {
            is EventCreateAction.ChangeTitle -> {
                _state.update { it.copy(title = action.title, titleError = null) }
            }

            is EventCreateAction.ChangeDescription -> {
                _state.update { it.copy(description = action.description, descriptionError = null) }
            }

            is EventCreateAction.ChangeStartDate -> {
                changeStart(LocalDateTime(action.date, _state.value.startTime))
            }

            is EventCreateAction.ChangeStartTime -> {
                changeStart(LocalDateTime(_state.value.startDate, action.time))
            }

            is EventCreateAction.ChangeEndDate -> {
                _state.update { it.copy(endDate = action.date, timeError = null) }
            }

            is EventCreateAction.ChangeEndTime -> {
                _state.update { it.copy(endTime = action.time, timeError = null) }
            }

            EventCreateAction.Save -> {
                save()
            }

            EventCreateAction.Back -> {
                viewModelScope.launch { _effects.send(EventCreateEffect.NavigateBack) }
            }

            EventCreateAction.DismissError -> {
                _state.update { it.copy(errorMessage = null) }
            }
        }
    }

    /** 시작을 바꾸면 기존 길이(양수가 아니면 1시간)를 유지하도록 종료도 함께 옮긴다. */
    private fun changeStart(newStart: LocalDateTime) {
        _state.update { current ->
            val oldDuration = current.endInstant() - current.startInstant()
            val duration = if (oldDuration > Duration.ZERO) oldDuration else DEFAULT_DURATION
            val newEnd = (newStart.toInstant(timeZone) + duration).toLocalDateTime(timeZone)
            current.copy(
                startDate = newStart.date,
                startTime = newStart.time,
                endDate = newEnd.date,
                endTime = newEnd.time,
                timeError = null,
            )
        }
    }

    private fun save() {
        val current = _state.value
        if (current.isSaving) return

        val title = current.title.trim()
        val start = current.startInstant()
        val end = current.endInstant()
        val validation = validateEventInput(title, current.description, start, end)
        if (!validation.isValid) {
            _state.update {
                it.copy(
                    titleError = validation.titleError,
                    descriptionError = validation.descriptionError,
                    timeError = validation.timeError,
                )
            }
            return
        }

        _state.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            eventRepository
                .createEvent(title = title, startTime = start, endTime = end, description = current.description)
                .onSuccess { event ->
                    _state.update { it.copy(isSaving = false) }
                    _effects.send(EventCreateEffect.Created(event.id))
                }.onFailure { error ->
                    _state.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = error.message ?: "일정을 저장하지 못했습니다.",
                        )
                    }
                }
        }
    }

    private fun EventCreateUiState.startInstant(): Instant = LocalDateTime(startDate, startTime).toInstant(timeZone)

    private fun EventCreateUiState.endInstant(): Instant = LocalDateTime(endDate, endTime).toInstant(timeZone)

    private fun initialState(
        initialDate: LocalDate,
        now: Instant,
    ): EventCreateUiState {
        val nowLocal = now.toLocalDateTime(timeZone)
        // 오늘이면 다음 정시(최대 23시), 다른 날이면 09:00.
        val startHour =
            if (initialDate == nowLocal.date) {
                (nowLocal.hour + 1).coerceAtMost(LAST_START_HOUR)
            } else {
                DEFAULT_START_HOUR
            }
        val start = LocalDateTime(initialDate, LocalTime(startHour, 0))
        val end = (start.toInstant(timeZone) + DEFAULT_DURATION).toLocalDateTime(timeZone)
        return EventCreateUiState(
            startDate = start.date,
            startTime = start.time,
            endDate = end.date,
            endTime = end.time,
        )
    }

    private companion object {
        const val DEFAULT_START_HOUR = 9
        const val LAST_START_HOUR = 23
        val DEFAULT_DURATION = 1.hours
    }
}
