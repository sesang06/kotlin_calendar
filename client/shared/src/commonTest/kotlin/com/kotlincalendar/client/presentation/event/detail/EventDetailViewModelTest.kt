package com.kotlincalendar.client.presentation.event.detail

import app.cash.turbine.test
import com.kotlincalendar.client.domain.model.Event
import com.kotlincalendar.client.fake.FakeEventRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class EventDetailViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repo = FakeEventRepository()
    private val seoul = UtcOffset(hours = 9).asTimeZone()

    private val sampleEvent =
        Event(
            id = "event-1",
            title = "팀 회의",
            start = Instant.parse("2026-10-25T05:00:00Z"),
            end = Instant.parse("2026-10-25T06:00:00Z"),
            description = "주간 회고",
        )

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun createViewModel() = EventDetailViewModel(eventId = "event-1", eventRepository = repo, timeZone = seoul)

    @Test
    fun init_loadsEvent_showsContent() =
        runTest(dispatcher) {
            repo.getEventResult = Result.success(sampleEvent)
            val vm = createViewModel()

            assertTrue(vm.state.value.isLoading)
            advanceUntilIdle()

            val state = vm.state.value
            assertFalse(state.isLoading)
            assertTrue(state.isLoaded)
            assertNull(state.loadErrorMessage)
            assertEquals("팀 회의", state.title)
            assertEquals("2026년 10월 25일 (일) 14:00 – 15:00", state.timeRangeText)
            assertEquals("주간 회고", state.description)
            assertEquals(listOf("event-1"), repo.requestedEventIds)
        }

    @Test
    fun loadFailure_setsError_thenRetryLoadsEvent() =
        runTest(dispatcher) {
            repo.getEventResult = Result.failure(RuntimeException("network down"))
            val vm = createViewModel()
            advanceUntilIdle()

            assertFalse(vm.state.value.isLoading)
            assertFalse(vm.state.value.isLoaded)
            assertEquals("network down", vm.state.value.loadErrorMessage)

            repo.getEventResult = Result.success(sampleEvent)
            vm.onAction(EventDetailAction.Retry)
            assertTrue(vm.state.value.isLoading)
            assertNull(vm.state.value.loadErrorMessage)
            advanceUntilIdle()

            assertTrue(vm.state.value.isLoaded)
            assertEquals("팀 회의", vm.state.value.title)
            assertEquals(listOf("event-1", "event-1"), repo.requestedEventIds)
        }

    @Test
    fun requestDelete_thenCancel_hidesDialogWithoutDeleting() =
        runTest(dispatcher) {
            repo.getEventResult = Result.success(sampleEvent)
            val vm = createViewModel()
            advanceUntilIdle()

            vm.onAction(EventDetailAction.RequestDelete)
            assertTrue(vm.state.value.showDeleteConfirm)

            vm.onAction(EventDetailAction.CancelDelete)
            advanceUntilIdle()
            assertFalse(vm.state.value.showDeleteConfirm)
            assertTrue(repo.deletedEventIds.isEmpty())
        }

    @Test
    fun requestDelete_beforeLoaded_isIgnored() =
        runTest(dispatcher) {
            repo.getEventResult = Result.failure(RuntimeException("boom"))
            val vm = createViewModel()
            advanceUntilIdle()

            vm.onAction(EventDetailAction.RequestDelete)
            assertFalse(vm.state.value.showDeleteConfirm)
        }

    @Test
    fun confirmDelete_success_emitsDeletedEffect() =
        runTest(dispatcher) {
            repo.getEventResult = Result.success(sampleEvent)
            repo.deleteEventResult = Result.success(Unit)
            val vm = createViewModel()
            advanceUntilIdle()

            vm.effects.test {
                vm.onAction(EventDetailAction.RequestDelete)
                vm.onAction(EventDetailAction.ConfirmDelete)
                assertFalse(vm.state.value.showDeleteConfirm)
                assertTrue(vm.state.value.isDeleting)

                assertEquals(EventDetailEffect.Deleted, awaitItem())
                assertFalse(vm.state.value.isDeleting)
            }
            assertEquals(listOf("event-1"), repo.deletedEventIds)
        }

    @Test
    fun confirmDelete_failure_setsDeleteErrorMessage() =
        runTest(dispatcher) {
            repo.getEventResult = Result.success(sampleEvent)
            repo.deleteEventResult = Result.failure(RuntimeException("forbidden"))
            val vm = createViewModel()
            advanceUntilIdle()

            vm.effects.test {
                vm.onAction(EventDetailAction.RequestDelete)
                vm.onAction(EventDetailAction.ConfirmDelete)
                advanceUntilIdle()

                assertFalse(vm.state.value.isDeleting)
                assertEquals("forbidden", vm.state.value.deleteErrorMessage)
                assertTrue(vm.state.value.isLoaded)
                expectNoEvents()
            }

            vm.onAction(EventDetailAction.DismissError)
            assertNull(vm.state.value.deleteErrorMessage)
        }

    @Test
    fun back_emitsNavigateBackEffect() =
        runTest(dispatcher) {
            repo.getEventResult = Result.success(sampleEvent)
            val vm = createViewModel()

            vm.effects.test {
                vm.onAction(EventDetailAction.Back)
                assertEquals(EventDetailEffect.NavigateBack, awaitItem())
            }
        }

    @Test
    fun edit_afterLoaded_emitsNavigateToEdit() =
        runTest(dispatcher) {
            repo.getEventResult = Result.success(sampleEvent)
            val vm = createViewModel()
            advanceUntilIdle()

            vm.effects.test {
                vm.onAction(EventDetailAction.Edit)
                assertEquals(EventDetailEffect.NavigateToEdit("event-1"), awaitItem())
            }
        }
}
