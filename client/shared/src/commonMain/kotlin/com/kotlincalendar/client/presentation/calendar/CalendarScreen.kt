package com.kotlincalendar.client.presentation.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import org.koin.compose.viewmodel.koinViewModel

private val WideLayoutMinWidth = 720.dp
private val CompactWeekHeight = 48.dp
private val DayNumberSize = 32.dp
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
            if (maxWidth >= WideLayoutMinWidth) {
                // 넓은 화면: 달력이 남는 높이를 채우고, 오른쪽에 선택한 날의 일정 패널.
                Row(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    MonthCalendar(
                        state = state,
                        onAction = onAction,
                        fillHeight = true,
                        modifier = Modifier.weight(1.6f).fillMaxHeight(),
                    )
                    AgendaPanel(
                        state = state,
                        onAction = onAction,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    MonthCalendar(state = state, onAction = onAction, fillHeight = false)
                    AgendaPanel(
                        state = state,
                        onAction = onAction,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/**
 * @param fillHeight true면 주(week) 행들이 남는 높이를 나눠 갖고 날짜는 칸 위쪽에 놓인다.
 *   false면 주 행 높이가 [CompactWeekHeight]로 고정된다.
 */
@Composable
private fun MonthCalendar(
    state: CalendarUiState,
    onAction: (CalendarAction) -> Unit,
    fillHeight: Boolean,
    modifier: Modifier = Modifier,
) {
    val weeks = remember(state.visibleMonth) { buildMonthGrid(state.visibleMonth) }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            MonthHeader(month = state.visibleMonth, onAction = onAction)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                WeekdayLabels.forEachIndexed { index, label ->
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = weekdayColor(index) ?: MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Column(modifier = if (fillHeight) Modifier.weight(1f) else Modifier) {
                weeks.forEach { week ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .then(if (fillHeight) Modifier.weight(1f) else Modifier.height(CompactWeekHeight)),
                    ) {
                        week.forEach { day ->
                            DayCell(
                                day = day,
                                isToday = day.date == state.today,
                                isSelected = day.date == state.selectedDate,
                                alignTop = fillHeight,
                                onClick = { onAction(CalendarAction.SelectDate(day.date)) },
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                        }
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
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f).padding(start = 8.dp),
        )
        OutlinedButton(onClick = { onAction(CalendarAction.GoToToday) }) {
            Text("오늘")
        }
        Spacer(modifier = Modifier.width(4.dp))
        IconButton(onClick = { onAction(CalendarAction.PreviousMonth) }) {
            Text("‹", style = MaterialTheme.typography.headlineSmall)
        }
        IconButton(onClick = { onAction(CalendarAction.NextMonth) }) {
            Text("›", style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun DayCell(
    day: CalendarDay,
    isToday: Boolean,
    isSelected: Boolean,
    alignTop: Boolean,
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
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClick)
                .padding(4.dp),
        contentAlignment = if (alignTop) Alignment.TopCenter else Alignment.Center,
    ) {
        // 날짜 원은 칸 크기와 상관없이 고정 크기.
        Box(
            modifier =
                Modifier
                    .size(DayNumberSize)
                    .clip(CircleShape)
                    .then(if (isSelected) Modifier.background(colors.primary) else Modifier)
                    .then(if (isToday && !isSelected) Modifier.border(BorderStroke(1.5.dp, colors.primary), CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = day.date.day.toString(),
                color = if (isToday && !isSelected) colors.primary else textColor,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun AgendaPanel(
    state: CalendarUiState,
    onAction: (CalendarAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            Text(
                text = "${state.selectedDate.month.number}월 ${state.selectedDate.day}일",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = koreanDayOfWeek(state.selectedDate.dayOfWeek) + if (state.selectedDate == state.today) " · 오늘" else "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))
            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                state.errorMessage != null -> ErrorBox(message = state.errorMessage, onRetry = { onAction(CalendarAction.Retry) })

                state.events.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "일정이 없습니다",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "‘일정 추가’로 새 일정을 만들어 보세요.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
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
}

@Composable
private fun ErrorBox(
    message: String,
    onRetry: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "일정을 불러오지 못했습니다",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            TextButton(onClick = onRetry, modifier = Modifier.align(Alignment.End)) {
                Text("다시 시도")
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
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier =
                    Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary),
            )
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
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
        }
    }
}

private fun weekdayColor(index: Int): Color? =
    when (index) {
        0 -> SundayColor
        DAYS_IN_WEEK - 1 -> SaturdayColor
        else -> null
    }

private fun koreanDayOfWeek(dayOfWeek: DayOfWeek): String =
    when (dayOfWeek) {
        DayOfWeek.MONDAY -> "월요일"
        DayOfWeek.TUESDAY -> "화요일"
        DayOfWeek.WEDNESDAY -> "수요일"
        DayOfWeek.THURSDAY -> "목요일"
        DayOfWeek.FRIDAY -> "금요일"
        DayOfWeek.SATURDAY -> "토요일"
        DayOfWeek.SUNDAY -> "일요일"
    }
