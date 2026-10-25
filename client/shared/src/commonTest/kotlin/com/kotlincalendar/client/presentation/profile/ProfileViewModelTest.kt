package com.kotlincalendar.client.presentation.profile

import app.cash.turbine.test
import com.kotlincalendar.client.domain.model.AuthState
import com.kotlincalendar.client.domain.model.User
import com.kotlincalendar.client.fake.FakeAuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val saved = User("u1", "old@example.com", "옛 이름", Instant.parse("2026-10-01T00:00:00Z"))
    private val fresh = saved.copy(email = "new@example.com", displayName = "새 이름")

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun init_showsFreshUserFromServer() =
        runTest(dispatcher) {
            val repo = FakeAuthRepository(AuthState.Authenticated(saved))
            repo.refreshCurrentUserResult = Result.success(fresh)

            val vm = ProfileViewModel(repo, TimeZone.UTC)
            advanceUntilIdle()

            val state = vm.state.value
            assertEquals("새 이름", state.displayName)
            assertEquals("new@example.com", state.email)
            assertEquals("2026년 10월 1일 (목)", state.joinedAt)
            assertFalse(state.isLoading)
            assertNull(state.errorMessage)
        }

    @Test
    fun refreshFailure_keepsSavedUserAndShowsError() =
        runTest(dispatcher) {
            val repo = FakeAuthRepository(AuthState.Authenticated(saved))
            repo.refreshCurrentUserResult = Result.failure(IllegalStateException("network"))

            val vm = ProfileViewModel(repo, TimeZone.UTC)
            advanceUntilIdle()

            assertEquals("옛 이름", vm.state.value.displayName)
            assertEquals("network", vm.state.value.errorMessage)
        }

    @Test
    fun logout_logsOutRepository() =
        runTest(dispatcher) {
            val repo = FakeAuthRepository(AuthState.Authenticated(saved))
            repo.refreshCurrentUserResult = Result.success(saved)
            val vm = ProfileViewModel(repo, TimeZone.UTC)
            advanceUntilIdle()

            vm.onAction(ProfileAction.Logout)
            advanceUntilIdle()

            assertEquals(AuthState.Unauthenticated, repo.authState.value)
        }

    @Test
    fun back_emitsNavigateBack() =
        runTest(dispatcher) {
            val repo = FakeAuthRepository(AuthState.Authenticated(saved))
            val vm = ProfileViewModel(repo, TimeZone.UTC)

            vm.effects.test {
                vm.onAction(ProfileAction.Back)
                assertEquals(ProfileEffect.NavigateBack, awaitItem())
            }
        }
}
