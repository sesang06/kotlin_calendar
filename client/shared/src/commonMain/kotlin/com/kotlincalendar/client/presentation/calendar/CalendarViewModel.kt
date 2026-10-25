package com.kotlincalendar.client.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlincalendar.client.domain.repository.EventRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock

class CalendarViewModel(
    private val eventRepository: EventRepository,
    clock: Clock,
    private val timeZone: TimeZone,
) : ViewModel() {
    private val _state = MutableStateFlow(CalendarUiState(today = clock.todayIn(timeZone)))
    val state: StateFlow<CalendarUiState> = _state.asStateFlow()

    private val _effects = Channel<CalendarEffect>(Channel.BUFFERED)
    val effects: Flow<CalendarEffect> = _effects.receiveAsFlow()

    private var loadJob: Job? = null
    private var rangeJob: Job? = null
    private var loadedRange: ClosedRange<LocalDate>? = null

    init {
        loadEvents()
        loadRangeIfNeeded(force = true)
    }

    fun onAction(action: CalendarAction) {
        when (action) {
            CalendarAction.PreviousMonth -> showMonth(_state.value.visibleMonth.minus(1, DateTimeUnit.MONTH))
            CalendarAction.NextMonth -> showMonth(_state.value.visibleMonth.plus(1, DateTimeUnit.MONTH))
            CalendarAction.GoToToday -> selectDate(_state.value.today)
            CalendarAction.PreviousPeriod -> movePeriod(-1)
            CalendarAction.NextPeriod -> movePeriod(1)
            is CalendarAction.ChangeViewMode -> {
                _state.update { it.copy(viewMode = action.mode) }
                loadRangeIfNeeded(force = false)
            }
            is CalendarAction.SelectDate -> selectDate(action.date)
            is CalendarAction.OpenEvent -> sendEffect(CalendarEffect.NavigateToEvent(action.id))
            CalendarAction.CreateEvent -> sendEffect(CalendarEffect.NavigateToCreateEvent(_state.value.selectedDate))
            CalendarAction.Refresh, CalendarAction.Retry -> {
                loadEvents()
                loadRangeIfNeeded(force = true)
            }
        }
    }

    /** 달을 옮기면 그 달에 오늘이 있으면 오늘, 없으면 1일을 선택한다. */
    private fun showMonth(month: LocalDate) {
        val today = _state.value.today
        val date = if (firstDayOfMonth(today) == month) today else month
        selectDate(date)
    }

    private fun movePeriod(direction: Int) {
        val current = _state.value
        when (current.viewMode) {
            CalendarViewMode.MONTH -> {
                showMonth(
                    if (direction > 0) {
                        current.visibleMonth.plus(1, DateTimeUnit.MONTH)
                    } else {
                        current.visibleMonth.minus(1, DateTimeUnit.MONTH)
                    },
                )
            }
            CalendarViewMode.WEEK -> selectDate(current.selectedDate.plus(direction * DAYS_IN_WEEK, DateTimeUnit.DAY))
            CalendarViewMode.DAY -> selectDate(current.selectedDate.plus(direction, DateTimeUnit.DAY))
        }
    }

    private fun selectDate(date: LocalDate) {
        _state.update { it.copy(selectedDate = date, visibleMonth = firstDayOfMonth(date)) }
        loadEvents()
        loadRangeIfNeeded(force = false)
    }

    /** 월간은 그리드 전체, 주간은 그 주. 일간은 하루 목록만 쓰므로 범위를 불러오지 않는다. */
    private fun currentRange(state: CalendarUiState): ClosedRange<LocalDate>? =
        when (state.viewMode) {
            CalendarViewMode.MONTH -> monthGridRange(state.visibleMonth)
            CalendarViewMode.WEEK -> weekRange(state.selectedDate)
            CalendarViewMode.DAY -> null
        }

    /** 범위 일정을 불러와 일정 있는 날 표시와 주간 목록을 만든다. 실패해도 화면 오류로 보지 않는다. */
    private fun loadRangeIfNeeded(force: Boolean) {
        val range = currentRange(_state.value) ?: return
        if (!force && range == loadedRange) return
        loadedRange = range
        rangeJob?.cancel()
        rangeJob =
            viewModelScope.launch {
                val events = eventRepository.getEventsInRange(range.start, range.endInclusive, timeZone.id).getOrNull()
                _state.update {
                    it.copy(
                        eventDays = events?.let { list -> eventDays(list, timeZone, range) } ?: emptySet(),
                        weekDays =
                            if (it.viewMode == CalendarViewMode.WEEK) {
                                groupEventsByDay(events.orEmpty(), timeZone, range)
                            } else {
                                it.weekDays
                            },
                    )
                }
            }
    }

    private fun sendEffect(effect: CalendarEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }

    private fun loadEvents() {
        val date = _state.value.selectedDate
        loadJob?.cancel()
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        loadJob =
            viewModelScope.launch {
                eventRepository
                    .getEvents(date, timeZone.id)
                    .onSuccess { events ->
                        val items =
                            events
                                .sortedBy { it.start }
                                .map { it.toCalendarEventItem(date, timeZone) }
                        _state.update { it.copy(events = items, isLoading = false) }
                    }.onFailure { error ->
                        _state.update {
                            it.copy(
                                events = emptyList(),
                                isLoading = false,
                                errorMessage = error.message ?: "일정을 불러오지 못했습니다.",
                            )
                        }
                    }
            }
    }
}
