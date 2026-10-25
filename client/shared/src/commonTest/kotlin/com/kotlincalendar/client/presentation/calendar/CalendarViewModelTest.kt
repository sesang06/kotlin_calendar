package com.kotlincalendar.client.presentation.calendar

import app.cash.turbine.test
import com.kotlincalendar.client.domain.model.Event
import com.kotlincalendar.client.fake.FakeEventRepository
import com.kotlincalendar.client.fake.FixedClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repo = FakeEventRepository()
    private val timeZone = TimeZone.UTC
    private val today = LocalDate(2026, 10, 25)
    private val clock = FixedClock(Instant.parse("2026-10-25T03:00:00Z"))

    private fun event(
        id: String,
        title: String,
        startHour: Int,
        endHour: Int,
    ) = Event(
        id = id,
        title = title,
        start = LocalDateTime(2026, 10, 25, startHour, 0).toInstant(timeZone),
        end = LocalDateTime(2026, 10, 25, endHour, 0).toInstant(timeZone),
    )

    private fun createViewModel() = CalendarViewModel(repo, clock, timeZone)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun init_selectsTodayAndLoadsItsEvents() =
        runTest(dispatcher) {
            repo.eventsResult = Result.success(listOf(event("e2", "점심", 12, 13), event("e1", "회의", 9, 10)))
            val vm = createViewModel()

            assertEquals(today, vm.state.value.today)
            assertEquals(today, vm.state.value.selectedDate)
            assertEquals(LocalDate(2026, 10, 1), vm.state.value.visibleMonth)
            assertTrue(vm.state.value.isLoading)

            advanceUntilIdle()

            assertFalse(vm.state.value.isLoading)
            assertEquals(listOf(today), repo.requestedDates)
            assertEquals(
                listOf(
                    CalendarEventItem("e1", "회의", "09:00 - 10:00"),
                    CalendarEventItem("e2", "점심", "12:00 - 13:00"),
                ),
                vm.state.value.events,
            )
        }

    @Test
    fun selectDate_loadsEventsForDate() =
        runTest(dispatcher) {
            val vm = createViewModel()
            advanceUntilIdle()
            val other = LocalDate(2026, 10, 3)

            vm.onAction(CalendarAction.SelectDate(other))
            advanceUntilIdle()

            assertEquals(other, vm.state.value.selectedDate)
            assertEquals(other, repo.requestedDates.last())
        }

    @Test
    fun selectDate_inOtherMonth_movesVisibleMonth() =
        runTest(dispatcher) {
            val vm = createViewModel()

            vm.onAction(CalendarAction.SelectDate(LocalDate(2026, 11, 2)))
            advanceUntilIdle()

            assertEquals(LocalDate(2026, 11, 1), vm.state.value.visibleMonth)
        }

    @Test
    fun nextAndPreviousMonth_changeVisibleMonthAndSelectFirstDay() =
        runTest(dispatcher) {
            val vm = createViewModel()
            advanceUntilIdle()

            vm.onAction(CalendarAction.NextMonth)
            advanceUntilIdle()
            assertEquals(LocalDate(2026, 11, 1), vm.state.value.visibleMonth)
            assertEquals(LocalDate(2026, 11, 1), vm.state.value.selectedDate)
            assertEquals(LocalDate(2026, 11, 1), repo.requestedDates.last())

            vm.onAction(CalendarAction.PreviousMonth)
            vm.onAction(CalendarAction.PreviousMonth)
            advanceUntilIdle()
            assertEquals(LocalDate(2026, 9, 1), vm.state.value.visibleMonth)
            assertEquals(LocalDate(2026, 9, 1), vm.state.value.selectedDate)
        }

    @Test
    fun navigatingBackToCurrentMonth_selectsToday() =
        runTest(dispatcher) {
            val vm = createViewModel()

            vm.onAction(CalendarAction.NextMonth)
            vm.onAction(CalendarAction.PreviousMonth)
            advanceUntilIdle()

            assertEquals(today, vm.state.value.selectedDate)
        }

    @Test
    fun previousMonth_acrossYearBoundary() =
        runTest(dispatcher) {
            val vm = createViewModel()

            vm.onAction(CalendarAction.SelectDate(LocalDate(2026, 1, 15)))
            vm.onAction(CalendarAction.PreviousMonth)
            advanceUntilIdle()

            assertEquals(LocalDate(2025, 12, 1), vm.state.value.visibleMonth)
        }

    @Test
    fun goToToday_returnsToTodayFromOtherMonth() =
        runTest(dispatcher) {
            val vm = createViewModel()
            vm.onAction(CalendarAction.SelectDate(LocalDate(2027, 3, 3)))
            advanceUntilIdle()

            vm.onAction(CalendarAction.GoToToday)
            advanceUntilIdle()

            assertEquals(today, vm.state.value.selectedDate)
            assertEquals(LocalDate(2026, 10, 1), vm.state.value.visibleMonth)
            assertEquals(today, repo.requestedDates.last())
        }

    @Test
    fun repositoryFailure_setsErrorMessage_andRetryReloads() =
        runTest(dispatcher) {
            repo.eventsResult = Result.failure(IllegalStateException("boom"))
            val vm = createViewModel()
            advanceUntilIdle()

            assertEquals("boom", vm.state.value.errorMessage)
            assertFalse(vm.state.value.isLoading)
            assertTrue(
                vm.state.value.events
                    .isEmpty(),
            )

            repo.eventsResult = Result.success(listOf(event("e1", "회의", 9, 10)))
            vm.onAction(CalendarAction.Retry)
            advanceUntilIdle()

            assertNull(vm.state.value.errorMessage)
            assertEquals(1, vm.state.value.events.size)
            assertEquals(2, repo.requestedDates.size)
        }

    @Test
    fun repositoryFailureWithoutMessage_usesDefaultMessage() =
        runTest(dispatcher) {
            repo.eventsResult = Result.failure(IllegalStateException())
            val vm = createViewModel()
            advanceUntilIdle()

            assertEquals("일정을 불러오지 못했습니다.", vm.state.value.errorMessage)
        }

    @Test
    fun refresh_reloadsSelectedDate() =
        runTest(dispatcher) {
            val vm = createViewModel()
            advanceUntilIdle()
            assertTrue(
                vm.state.value.events
                    .isEmpty(),
            )

            repo.eventsResult = Result.success(listOf(event("new", "새 일정", 15, 16)))
            vm.onAction(CalendarAction.Refresh)
            advanceUntilIdle()

            assertEquals(listOf(today, today), repo.requestedDates)
            assertEquals(
                "new",
                vm.state.value.events
                    .single()
                    .id,
            )
        }

    @Test
    fun state_emitsLoadingThenLoaded() =
        runTest(dispatcher) {
            repo.eventsResult = Result.success(listOf(event("e1", "회의", 9, 10)))
            val vm = createViewModel()

            vm.state.test {
                assertTrue(awaitItem().isLoading)
                val loaded = awaitItem()
                assertFalse(loaded.isLoading)
                assertEquals(1, loaded.events.size)
            }
        }

    @Test
    fun openEvent_emitsNavigateToEventEffect() =
        runTest(dispatcher) {
            val vm = createViewModel()

            vm.effects.test {
                vm.onAction(CalendarAction.OpenEvent("e1"))
                assertEquals(CalendarEffect.NavigateToEvent("e1"), awaitItem())
            }
        }

    @Test
    fun createEvent_emitsNavigateToCreateEventWithSelectedDate() =
        runTest(dispatcher) {
            val vm = createViewModel()
            val date = LocalDate(2026, 10, 30)
            vm.onAction(CalendarAction.SelectDate(date))

            vm.effects.test {
                vm.onAction(CalendarAction.CreateEvent)
                assertEquals(CalendarEffect.NavigateToCreateEvent(date), awaitItem())
            }
        }
}
