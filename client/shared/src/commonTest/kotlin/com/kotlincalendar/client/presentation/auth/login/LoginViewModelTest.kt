package com.kotlincalendar.client.presentation.auth.login

import app.cash.turbine.test
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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repo = FakeAuthRepository()

    private val sampleUser =
        User(
            id = "user-1",
            email = "user@example.com",
            displayName = "User",
            createdAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
        )

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun changeEmailAndPassword_updatesState() =
        runTest(dispatcher) {
            val vm = LoginViewModel(repo)

            vm.onAction(LoginAction.ChangeEmail("test@example.com"))
            vm.onAction(LoginAction.ChangePassword("password123"))

            assertEquals("test@example.com", vm.state.value.email)
            assertEquals("password123", vm.state.value.password)
        }

    @Test
    fun submit_withBlankEmailOrPassword_setsErrorMessage() =
        runTest(dispatcher) {
            val vm = LoginViewModel(repo)

            vm.onAction(LoginAction.Submit)
            assertEquals("이메일과 비밀번호를 입력해주세요.", vm.state.value.errorMessage)
            assertNull(repo.lastLoginEmail)
        }

    @Test
    fun submit_success_emitsNavigateToHomeEffect() =
        runTest(dispatcher) {
            repo.loginResult = Result.success(sampleUser)
            val vm = LoginViewModel(repo)

            vm.onAction(LoginAction.ChangeEmail("user@example.com"))
            vm.onAction(LoginAction.ChangePassword("password123"))

            vm.effects.test {
                vm.onAction(LoginAction.Submit)
                advanceUntilIdle()

                assertEquals(LoginEffect.NavigateToHome, awaitItem())
                assertFalse(vm.state.value.isLoading)
                assertNull(vm.state.value.errorMessage)
            }

            assertEquals("user@example.com", repo.lastLoginEmail)
            assertEquals("password123", repo.lastLoginPassword)
        }

    @Test
    fun submit_failure_setsErrorMessage() =
        runTest(dispatcher) {
            repo.loginResult = Result.failure(RuntimeException("Invalid credentials"))
            val vm = LoginViewModel(repo)

            vm.onAction(LoginAction.ChangeEmail("user@example.com"))
            vm.onAction(LoginAction.ChangePassword("wrong-password"))

            vm.onAction(LoginAction.Submit)
            advanceUntilIdle()

            assertFalse(vm.state.value.isLoading)
            assertEquals("Invalid credentials", vm.state.value.errorMessage)
        }

    @Test
    fun navigateToSignUp_emitsEffect() =
        runTest(dispatcher) {
            val vm = LoginViewModel(repo)

            vm.effects.test {
                vm.onAction(LoginAction.NavigateToSignUp)
                assertEquals(LoginEffect.NavigateToSignUp, awaitItem())
            }
        }

    @Test
    fun dismissError_clearsErrorMessage() =
        runTest(dispatcher) {
            val vm = LoginViewModel(repo)

            vm.onAction(LoginAction.Submit)
            assertTrue(vm.state.value.errorMessage != null)

            vm.onAction(LoginAction.DismissError)
            assertNull(vm.state.value.errorMessage)
        }
}
