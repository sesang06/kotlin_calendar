package com.kotlincalendar.client.presentation.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import org.koin.compose.viewmodel.koinViewModel

private val WideLayoutMinWidth = 720.dp
private val WeekdayLabels = listOf("일", "월", "화", "수", "목", "금", "토")
private val SundayColor = Color(0xFFD32F2F)
private val SaturdayColor = Color(0xFF1976D2)

/**
 * 메인 캘린더 화면.
 *
 * @param onCreateEvent 추가 버튼을 누르면 선택된 날짜와 함께 호출된다.
 * @param onEventClick 일정 행을 누르면 일정 id와 함께 호출된다.
 * @param refreshKey 값이 바뀔 때마다(null 제외) 선택된 날짜의 일정을 다시 불러온다. 일정 생성·삭제 후 갱신용.
 */
@Composable
fun CalendarRoute(
    onCreateEvent: (LocalDate) -> Unit,
    onEventClick: (String) -> Unit,
    refreshKey: Any? = null,
    viewModel: CalendarViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is CalendarEffect.NavigateToCreateEvent -> onCreateEvent(effect.date)
                is CalendarEffect.NavigateToEvent -> onEventClick(effect.id)
            }
        }
    }

    LaunchedEffect(viewModel, refreshKey) {
        if (refreshKey != null) viewModel.onAction(CalendarAction.Refresh)
    }

    CalendarScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}

@Composable
fun CalendarScreen(
    state: CalendarUiState,
    onAction: (CalendarAction) -> Unit,
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { onAction(CalendarAction.CreateEvent) }) {
                Text("+", style = MaterialTheme.typography.headlineSmall)
            }
        },
    ) { paddingValues ->
        BoxWithConstraints(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            if (maxWidth >= WideLayoutMinWidth) {
                Row(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    MonthCalendar(
                        state = state,
                        onAction = onAction,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(24.dp))
                    EventList(
                        state = state,
                        onAction = onAction,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    MonthCalendar(state = state, onAction = onAction)
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    EventList(
                        state = state,
                        onAction = onAction,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthCalendar(
    state: CalendarUiState,
    onAction: (CalendarAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val weeks = remember(state.visibleMonth) { buildMonthGrid(state.visibleMonth) }

    Column(modifier = modifier) {
        MonthHeader(month = state.visibleMonth, onAction = onAction)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            WeekdayLabels.forEachIndexed { index, label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = weekdayColor(index) ?: MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        weeks.forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    DayCell(
                        day = day,
                        isToday = day.date == state.today,
                        isSelected = day.date == state.selectedDate,
                        onClick = { onAction(CalendarAction.SelectDate(day.date)) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(
    month: LocalDate,
    onAction: (CalendarAction) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = { onAction(CalendarAction.PreviousMonth) }) {
            Text("<")
        }
        Text(
            text = "${month.year}년 ${month.month.number}월",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = { onAction(CalendarAction.NextMonth) }) {
            Text(">")
        }
        OutlinedButton(onClick = { onAction(CalendarAction.GoToToday) }) {
            Text("오늘")
        }
    }
}

@Composable
private fun DayCell(
    day: CalendarDay,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val baseColor =
        when (day.date.dayOfWeek) {
            DayOfWeek.SUNDAY -> SundayColor
            DayOfWeek.SATURDAY -> SaturdayColor
            else -> colors.onSurface
        }
    val textColor =
        when {
            isSelected -> colors.onPrimary
            !day.isInCurrentMonth -> baseColor.copy(alpha = 0.38f)
            else -> baseColor
        }

    Box(
        modifier =
            modifier
                .aspectRatio(1f)
                .padding(2.dp)
                .clip(CircleShape)
                .then(if (isSelected) Modifier.background(colors.primary) else Modifier)
                .then(if (isToday && !isSelected) Modifier.border(BorderStroke(1.5.dp, colors.primary), CircleShape) else Modifier)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = day.date.day.toString(),
            color = textColor,
            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun EventList(
    state: CalendarUiState,
    onAction: (CalendarAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = "${state.selectedDate.month.number}월 ${state.selectedDate.day}일 일정",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        when {
            state.isLoading -> {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            state.errorMessage != null -> {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = state.errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                    Button(onClick = { onAction(CalendarAction.Retry) }) {
                        Text("다시 시도")
                    }
                }
            }

            state.events.isEmpty() -> {
                Text(
                    text = "일정이 없습니다.",
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            else -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.events, key = { it.id }) { event ->
                        EventRow(event = event, onClick = { onAction(CalendarAction.OpenEvent(event.id)) })
                    }
                }
            }
        }
    }
}

@Composable
private fun EventRow(
    event: CalendarEventItem,
    onClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = event.timeRange,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = event.title,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

private fun weekdayColor(index: Int): Color? =
    when (index) {
        0 -> SundayColor
        DAYS_IN_WEEK - 1 -> SaturdayColor
        else -> null
    }
