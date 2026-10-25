package com.kotlincalendar.client.presentation.event.create

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
import kotlinx.datetime.FixedOffsetTimeZone
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class EventCreateViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repo = FakeEventRepository()

    // tz 데이터베이스가 없는 타깃(wasmJs)에서도 돌도록 UTC+9 고정 오프셋 (KST).
    private val timeZone: TimeZone = FixedOffsetTimeZone(UtcOffset(hours = 9))

    // 2026-10-25 14:30 KST
    private val clock = FixedClock(Instant.parse("2026-10-25T05:30:00Z"))
    private val today = LocalDate(2026, 10, 25)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(initialDate: LocalDate = today) = EventCreateViewModel(initialDate, repo, clock, timeZone)

    @Test
    fun today_defaultsToNextFullHour() {
        val state = viewModel().state.value

        assertEquals(today, state.startDate)
        assertEquals(LocalTime(15, 0), state.startTime)
        assertEquals(today, state.endDate)
        assertEquals(LocalTime(16, 0), state.endTime)
    }

    @Test
    fun otherDate_defaultsToNineAm() {
        val date = LocalDate(2026, 11, 3)
        val state = viewModel(date).state.value

        assertEquals(date, state.startDate)
        assertEquals(LocalTime(9, 0), state.startTime)
        assertEquals(date, state.endDate)
        assertEquals(LocalTime(10, 0), state.endTime)
    }

    @Test
    fun todayLateNight_startsAt23AndEndsNextDay() {
        // 2026-10-25 23:30 KST
        val lateClock = FixedClock(Instant.parse("2026-10-25T14:30:00Z"))
        val state = EventCreateViewModel(today, repo, lateClock, timeZone).state.value

        assertEquals(LocalTime(23, 0), state.startTime)
        assertEquals(LocalDate(2026, 10, 26), state.endDate)
        assertEquals(LocalTime(0, 0), state.endTime)
    }

    @Test
    fun changeStartTime_keepsDuration() {
        val vm = viewModel()

        vm.onAction(EventCreateAction.ChangeEndTime(LocalTime(17, 0))) // 15:00~17:00 (2시간)
        vm.onAction(EventCreateAction.ChangeStartTime(LocalTime(18, 0)))

        assertEquals(LocalTime(18, 0), vm.state.value.startTime)
        assertEquals(LocalTime(20, 0), vm.state.value.endTime)
    }

    @Test
    fun save_withBlankTitle_setsTitleErrorAndDoesNotCallRepository() =
        runTest(dispatcher) {
            val vm = viewModel()

            vm.onAction(EventCreateAction.ChangeTitle("   "))
            vm.onAction(EventCreateAction.Save)
            advanceUntilIdle()

            assertEquals("제목을 입력해주세요.", vm.state.value.titleError)
            assertFalse(vm.state.value.isSaving)
            assertTrue(repo.createdEvents.isEmpty())
        }

    @Test
    fun save_withEndBeforeStart_setsTimeError() =
        runTest(dispatcher) {
            val vm = viewModel()

            vm.onAction(EventCreateAction.ChangeTitle("회의"))
            vm.onAction(EventCreateAction.ChangeEndTime(LocalTime(14, 0)))
            vm.onAction(EventCreateAction.Save)
            advanceUntilIdle()

            assertEquals("종료 시간은 시작 시간보다 늦어야 합니다.", vm.state.value.timeError)
            assertNull(vm.state.value.titleError)
            assertTrue(repo.createdEvents.isEmpty())
        }

    @Test
    fun changeTitle_clearsTitleError() {
        val vm = viewModel()

        vm.onAction(EventCreateAction.Save)
        assertNotNull(vm.state.value.titleError)

        vm.onAction(EventCreateAction.ChangeTitle("회의"))
        assertNull(vm.state.value.titleError)
    }

    @Test
    fun save_success_callsRepositoryAndEmitsCreated() =
        runTest(dispatcher) {
            repo.createEventResult =
                Result.success(
                    Event(
                        id = "event-1",
                        title = "회의",
                        start = Instant.parse("2026-10-25T06:00:00Z"),
                        end = Instant.parse("2026-10-25T07:00:00Z"),
                    ),
                )
            val vm = viewModel()
            vm.onAction(EventCreateAction.ChangeTitle("  회의  "))
            vm.onAction(EventCreateAction.ChangeDescription("메모"))

            vm.effects.test {
                vm.onAction(EventCreateAction.Save)
                assertTrue(vm.state.value.isSaving)
                advanceUntilIdle()

                assertEquals(EventCreateEffect.Created("event-1"), awaitItem())
                assertFalse(vm.state.value.isSaving)
            }

            val call = repo.createdEvents.single()
            assertEquals("회의", call.title)
            assertEquals("메모", call.description)
            assertEquals(Instant.parse("2026-10-25T06:00:00Z"), call.startTime) // 15:00 KST
            assertEquals(Instant.parse("2026-10-25T07:00:00Z"), call.endTime) // 16:00 KST
        }

    @Test
    fun save_failure_setsErrorMessage() =
        runTest(dispatcher) {
            repo.createEventResult = Result.failure(RuntimeException("서버 오류"))
            val vm = viewModel()
            vm.onAction(EventCreateAction.ChangeTitle("회의"))

            vm.onAction(EventCreateAction.Save)
            advanceUntilIdle()

            assertFalse(vm.state.value.isSaving)
            assertEquals("서버 오류", vm.state.value.errorMessage)

            vm.onAction(EventCreateAction.DismissError)
            assertNull(vm.state.value.errorMessage)
        }

    @Test
    fun back_emitsNavigateBack() =
        runTest(dispatcher) {
            val vm = viewModel()

            vm.effects.test {
                vm.onAction(EventCreateAction.Back)
                assertEquals(EventCreateEffect.NavigateBack, awaitItem())
            }
        }

    private val existingEvent =
        Event(
            id = "e1",
            title = "기존 회의",
            // 2026-11-03 10:00~11:30 KST
            start = Instant.parse("2026-11-03T01:00:00Z"),
            end = Instant.parse("2026-11-03T02:30:00Z"),
            description = "메모",
        )

    @Test
    fun editMode_loadsEventIntoForm() =
        runTest(dispatcher) {
            repo.getEventResult = Result.success(existingEvent)
            val vm = EventCreateViewModel(today, repo, clock, timeZone, eventId = "e1")

            assertTrue(vm.state.value.isEditing)
            assertTrue(vm.state.value.isLoadingEvent)
            advanceUntilIdle()

            val state = vm.state.value
            assertFalse(state.isLoadingEvent)
            assertEquals("기존 회의", state.title)
            assertEquals("메모", state.description)
            assertEquals(LocalDate(2026, 11, 3), state.startDate)
            assertEquals(LocalTime(10, 0), state.startTime)
            assertEquals(LocalTime(11, 30), state.endTime)
        }

    @Test
    fun editMode_save_updatesEventAndEmitsUpdated() =
        runTest(dispatcher) {
            repo.getEventResult = Result.success(existingEvent)
            repo.updateEventResult = Result.success(existingEvent.copy(title = "바뀐 회의"))
            val vm = EventCreateViewModel(today, repo, clock, timeZone, eventId = "e1")
            advanceUntilIdle()

            vm.effects.test {
                vm.onAction(EventCreateAction.ChangeTitle("바뀐 회의"))
                vm.onAction(EventCreateAction.Save)
                advanceUntilIdle()

                assertEquals(EventCreateEffect.Updated("e1"), awaitItem())
            }
            assertTrue(repo.createdEvents.isEmpty())
            val (id, call) = repo.updatedEvents.single()
            assertEquals("e1", id)
            assertEquals("바뀐 회의", call.title)
            assertEquals(existingEvent.start, call.startTime)
            assertEquals(existingEvent.end, call.endTime)
            assertEquals("메모", call.description)
        }

    @Test
    fun editMode_loadFailure_setsErrorAndBlocksSave() =
        runTest(dispatcher) {
            repo.getEventResult = Result.failure(IllegalStateException("없음"))
            val vm = EventCreateViewModel(today, repo, clock, timeZone, eventId = "e1")
            advanceUntilIdle()

            assertEquals("없음", vm.state.value.errorMessage)
            assertFalse(vm.state.value.isLoadingEvent)
        }
}
