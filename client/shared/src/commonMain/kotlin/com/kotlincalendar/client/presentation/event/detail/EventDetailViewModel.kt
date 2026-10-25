package com.kotlincalendar.client.presentation.event.detail

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
import kotlinx.datetime.TimeZone

class EventDetailViewModel(
    private val eventId: String,
    private val eventRepository: EventRepository,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel() {
    private val _state = MutableStateFlow(EventDetailUiState())
    val state: StateFlow<EventDetailUiState> = _state.asStateFlow()

    private val _effects = Channel<EventDetailEffect>(Channel.BUFFERED)
    val effects: Flow<EventDetailEffect> = _effects.receiveAsFlow()

    init {
        load()
    }

    fun onAction(action: EventDetailAction) {
        when (action) {
            EventDetailAction.Retry -> load()
            EventDetailAction.Back -> {
                viewModelScope.launch {
                    _effects.send(EventDetailEffect.NavigateBack)
                }
            }
            EventDetailAction.RequestDelete -> {
                val current = _state.value
                if (current.isLoaded && !current.isDeleting) {
                    _state.update { it.copy(showDeleteConfirm = true, deleteErrorMessage = null) }
                }
            }
            EventDetailAction.CancelDelete -> _state.update { it.copy(showDeleteConfirm = false) }
            EventDetailAction.ConfirmDelete -> delete()
            EventDetailAction.DismissError -> _state.update { it.copy(deleteErrorMessage = null) }
        }
    }

    private fun load() {
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true, loadErrorMessage = null) }
        viewModelScope.launch {
            eventRepository
                .getEvent(eventId)
                .onSuccess { event ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isLoaded = true,
                            title = event.title,
                            timeRangeText = formatEventRange(event.start, event.end, timeZone),
                            description = event.description,
                        )
                    }
                }.onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            loadErrorMessage = error.message ?: "일정을 불러오지 못했습니다.",
                        )
                    }
                }
        }
    }

    private fun delete() {
        if (_state.value.isDeleting) return
        _state.update { it.copy(showDeleteConfirm = false, isDeleting = true, deleteErrorMessage = null) }
        viewModelScope.launch {
            eventRepository
                .deleteEvent(eventId)
                .onSuccess {
                    _state.update { it.copy(isDeleting = false) }
                    _effects.send(EventDetailEffect.Deleted)
                }.onFailure { error ->
                    _state.update {
                        it.copy(
                            isDeleting = false,
                            deleteErrorMessage = error.message ?: "일정을 삭제하지 못했습니다.",
                        )
                    }
                }
        }
    }
}
