package com.kotlincalendar.client.presentation.app

import com.kotlincalendar.client.domain.model.User
import com.kotlincalendar.client.fake.FakeAuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repo = FakeAuthRepository()
    private val sampleUser = User("user-1", "user@example.com", "User", Instant.fromEpochMilliseconds(0))

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun startsRestoring() {
        val vm = AppViewModel(repo)

        assertTrue(vm.state.value.isRestoringSession)
    }

    @Test
    fun savedSession_startsLoggedIn() =
        runTest(dispatcher) {
            repo.restoredUser = sampleUser
            val vm = AppViewModel(repo)

            advanceUntilIdle()

            assertEquals(AppUiState(isRestoringSession = false, startLoggedIn = true, isLoggedIn = true), vm.state.value)
        }

    @Test
    fun logout_setsLoggedOutButKeepsStartDestination() =
        runTest(dispatcher) {
            repo.restoredUser = sampleUser
            val vm = AppViewModel(repo)
            advanceUntilIdle()

            vm.logout()
            advanceUntilIdle()

            assertEquals(AppUiState(isRestoringSession = false, startLoggedIn = true, isLoggedIn = false), vm.state.value)
        }

    @Test
    fun loginAfterStart_setsLoggedIn() =
        runTest(dispatcher) {
            val vm = AppViewModel(repo)
            advanceUntilIdle()

            repo.loginResult = Result.success(sampleUser)
            repo.login("user@example.com", "pw")
            advanceUntilIdle()

            assertTrue(vm.state.value.isLoggedIn)
        }

    @Test
    fun noSavedSession_startsLoggedOut() =
        runTest(dispatcher) {
            val vm = AppViewModel(repo)

            advanceUntilIdle()

            assertEquals(AppUiState(isRestoringSession = false, startLoggedIn = false, isLoggedIn = false), vm.state.value)
        }
}
