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
    private var monthJob: Job? = null

    init {
        loadEvents()
        loadMonth()
    }

    fun onAction(action: CalendarAction) {
        when (action) {
            CalendarAction.PreviousMonth -> showMonth(_state.value.visibleMonth.minus(1, DateTimeUnit.MONTH))
            CalendarAction.NextMonth -> showMonth(_state.value.visibleMonth.plus(1, DateTimeUnit.MONTH))
            CalendarAction.GoToToday -> selectDate(_state.value.today)
            is CalendarAction.SelectDate -> selectDate(action.date)
            is CalendarAction.OpenEvent -> sendEffect(CalendarEffect.NavigateToEvent(action.id))
            CalendarAction.CreateEvent -> sendEffect(CalendarEffect.NavigateToCreateEvent(_state.value.selectedDate))
            CalendarAction.Refresh, CalendarAction.Retry -> {
                loadEvents()
                loadMonth()
            }
        }
    }

    /** 달을 옮기면 그 달에 오늘이 있으면 오늘, 없으면 1일을 선택한다. */
    private fun showMonth(month: LocalDate) {
        val today = _state.value.today
        val date = if (firstDayOfMonth(today) == month) today else month
        selectDate(date)
    }

    private fun selectDate(date: LocalDate) {
        val previousMonth = _state.value.visibleMonth
        _state.update { it.copy(selectedDate = date, visibleMonth = firstDayOfMonth(date)) }
        loadEvents()
        if (firstDayOfMonth(date) != previousMonth) loadMonth()
    }

    /** 보이는 달 그리드 전체의 일정을 불러와 일정이 있는 날을 표시한다. 실패해도 화면 오류로 보지 않는다. */
    private fun loadMonth() {
        val range = monthGridRange(_state.value.visibleMonth)
        monthJob?.cancel()
        monthJob =
            viewModelScope.launch {
                eventRepository
                    .getEventsInRange(range.start, range.endInclusive, timeZone.id)
                    .onSuccess { events ->
                        _state.update { it.copy(eventDays = eventDays(events, timeZone, range)) }
                    }.onFailure {
                        _state.update { it.copy(eventDays = emptySet()) }
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
