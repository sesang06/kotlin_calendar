package com.kotlincalendar.client.presentation.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kotlincalendar.client.presentation.theme.WeekendColors
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.number
import org.koin.compose.viewmodel.koinViewModel

private val WideLayoutMinWidth = 720.dp
private val WeekdayLabels = listOf("일", "월", "화", "수", "목", "금", "토")

/**
 * 메인 캘린더 화면.
 *
 * @param onCreateEvent 추가 버튼을 누르면 선택된 날짜와 함께 호출된다.
 * @param onEventClick 일정 행을 누르면 일정 id와 함께 호출된다.
 * @param onOpenProfile 내 정보 버튼을 누르면 호출된다.
 * @param refreshKey 값이 바뀔 때마다(null 제외) 선택된 날짜의 일정을 다시 불러온다. 일정 생성·삭제 후 갱신용.
 */
@Composable
fun CalendarRoute(
    onCreateEvent: (LocalDate) -> Unit,
    onEventClick: (String) -> Unit,
    onOpenProfile: () -> Unit,
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
        onOpenProfile = onOpenProfile,
    )
}

@Composable
fun CalendarScreen(
    state: CalendarUiState,
    onAction: (CalendarAction) -> Unit,
    onOpenProfile: () -> Unit = {},
) {
    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAction(CalendarAction.CreateEvent) },
                icon = { Text("+", style = MaterialTheme.typography.titleLarge) },
                text = { Text("일정 추가") },
            )
        },
    ) { paddingValues ->
        BoxWithConstraints(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            val isWide = maxWidth >= WideLayoutMinWidth
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
                AppHeader(onOpenProfile = onOpenProfile)
                Spacer(modifier = Modifier.height(8.dp))
                ViewModeSelector(selected = state.viewMode, onSelect = { onAction(CalendarAction.ChangeViewMode(it)) })
                Spacer(modifier = Modifier.height(12.dp))
                if (state.viewMode != CalendarViewMode.MONTH) {
                    PeriodHeader(title = periodTitle(state), onAction = onAction)
                    Spacer(modifier = Modifier.height(12.dp))
                    if (state.viewMode == CalendarViewMode.WEEK) {
                        WeekList(state = state, onAction = onAction, modifier = Modifier.weight(1f))
                    } else {
                        EventList(state = state, onAction = onAction, modifier = Modifier.weight(1f))
                    }
                } else if (isWide) {
                    Row(modifier = Modifier.fillMaxSize()) {
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
                    MonthCalendar(state = state, onAction = onAction)
                    Spacer(modifier = Modifier.height(20.dp))
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
private fun AppHeader(onOpenProfile: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Kotlin Calendar",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onOpenProfile) {
            Text("내 정보")
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

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
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
                            hasEvents = day.date in state.eventDays,
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
        Text(
            text = "${month.year}년 ${month.month.number}월",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f).padding(start = 8.dp),
        )
        IconButton(onClick = { onAction(CalendarAction.PreviousMonth) }) {
            Text("‹", style = MaterialTheme.typography.headlineSmall)
        }
        IconButton(onClick = { onAction(CalendarAction.NextMonth) }) {
            Text("›", style = MaterialTheme.typography.headlineSmall)
        }
        FilledTonalButton(onClick = { onAction(CalendarAction.GoToToday) }) {
            Text("오늘")
        }
    }
}

@Composable
private fun DayCell(
    day: CalendarDay,
    hasEvents: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val baseColor =
        when (day.date.dayOfWeek) {
            DayOfWeek.SUNDAY -> WeekendColors.Sunday
            DayOfWeek.SATURDAY -> WeekendColors.Saturday
            else -> colors.onSurface
        }
    val background =
        when {
            isSelected -> colors.primary
            isToday -> colors.primaryContainer
            else -> Color.Transparent
        }
    val textColor =
        when {
            isSelected -> colors.onPrimary
            isToday -> colors.onPrimaryContainer
            !day.isInCurrentMonth -> baseColor.copy(alpha = 0.35f)
            else -> baseColor
        }

    Box(
        modifier =
            modifier
                .aspectRatio(1f)
                .padding(3.dp)
                .clip(CircleShape)
                .background(background)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = day.date.day.toString(),
            color = textColor,
            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
            style = MaterialTheme.typography.bodyMedium,
        )
        if (hasEvents) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 5.dp)
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) colors.onPrimary else colors.primary),
            )
        }
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
            text = "${state.selectedDate.month.number}월 ${state.selectedDate.day}일",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = if (state.events.isEmpty()) "일정 없음" else "일정 ${state.events.size}개",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))
        when {
            state.isLoading -> {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
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
                EmptyEvents(onCreate = { onAction(CalendarAction.CreateEvent) })
            }

            else -> {
                // FAB에 가리지 않도록 아래 여백을 둔다.
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 88.dp),
                ) {
                    items(state.events, key = { it.id }) { event ->
                        EventRow(event = event, onClick = { onAction(CalendarAction.OpenEvent(event.id)) })
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyEvents(onCreate: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 28.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "이 날은 일정이 없어요",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onCreate) {
                Text("+ 일정 추가하기")
            }
        }
    }
}

@Composable
private fun EventRow(
    event: CalendarEventItem,
    onClick: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .width(4.dp)
                        .height(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = event.timeRange,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "›",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

private fun weekdayColor(index: Int): Color? =
    when (index) {
        0 -> WeekendColors.Sunday
        DAYS_IN_WEEK - 1 -> WeekendColors.Saturday
        else -> null
    }

@Composable
private fun ViewModeSelector(
    selected: CalendarViewMode,
    onSelect: (CalendarViewMode) -> Unit,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            CalendarViewMode.entries.forEach { mode ->
                val isSelected = mode == selected
                Box(
                    modifier =
                        Modifier
                            .clip(CircleShape)
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { onSelect(mode) }
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = mode.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodHeader(
    title: String,
    onAction: (CalendarAction) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f).padding(start = 4.dp),
        )
        IconButton(onClick = { onAction(CalendarAction.PreviousPeriod) }) {
            Text("‹", style = MaterialTheme.typography.headlineSmall)
        }
        IconButton(onClick = { onAction(CalendarAction.NextPeriod) }) {
            Text("›", style = MaterialTheme.typography.headlineSmall)
        }
        FilledTonalButton(onClick = { onAction(CalendarAction.GoToToday) }) {
            Text("오늘")
        }
    }
}

internal fun periodTitle(state: CalendarUiState): String =
    when (state.viewMode) {
        CalendarViewMode.MONTH -> "${state.visibleMonth.year}년 ${state.visibleMonth.month.number}월"
        CalendarViewMode.WEEK -> {
            val range = weekRange(state.selectedDate)
            val start = range.start
            val end = range.endInclusive
            if (start.month == end.month) {
                "${start.month.number}월 ${start.day}일 – ${end.day}일"
            } else {
                "${start.month.number}월 ${start.day}일 – ${end.month.number}월 ${end.day}일"
            }
        }
        CalendarViewMode.DAY -> {
            val date = state.selectedDate
            "${date.month.number}월 ${date.day}일 (${WeekdayLabels[date.dayOfWeek.isoDayNumber % DAYS_IN_WEEK]})"
        }
    }

@Composable
private fun WeekList(
    state: CalendarUiState,
    onAction: (CalendarAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 88.dp),
    ) {
        items(state.weekDays, key = { it.date.toString() }) { day ->
            WeekDayRow(
                day = day,
                isToday = day.date == state.today,
                isSelected = day.date == state.selectedDate,
                onSelect = { onAction(CalendarAction.SelectDate(day.date)) },
                onOpenEvent = { onAction(CalendarAction.OpenEvent(it)) },
            )
        }
    }
}

@Composable
private fun WeekDayRow(
    day: CalendarWeekDay,
    isToday: Boolean,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onOpenEvent: (String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .clickable(onClick = onSelect),
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) colors.primaryContainer else colors.surfaceContainerLowest,
        tonalElevation = 1.dp,
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            Column(
                modifier = Modifier.width(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val weekdayIndex = day.date.dayOfWeek.isoDayNumber % DAYS_IN_WEEK
                Text(
                    text = WeekdayLabels[weekdayIndex],
                    style = MaterialTheme.typography.labelMedium,
                    color = weekdayColor(weekdayIndex) ?: colors.onSurfaceVariant,
                )
                Text(
                    text = day.date.day.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (isToday) colors.primary else colors.onSurface,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (day.events.isEmpty()) {
                    Text(
                        text = "일정 없음",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
                day.events.forEach { event ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .clickable { onOpenEvent(event.id) }
                                .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(colors.primary),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = event.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = event.timeRange,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
