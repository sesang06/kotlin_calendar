package com.kotlincalendar.client.presentation.auth.signup

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
class SignUpViewModelTest {
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
    fun changeFields_updatesState() =
        runTest(dispatcher) {
            val vm = SignUpViewModel(repo)

            vm.onAction(SignUpAction.ChangeEmail("test@example.com"))
            vm.onAction(SignUpAction.ChangePassword("password123"))
            vm.onAction(SignUpAction.ChangeDisplayName("Test User"))

            assertEquals("test@example.com", vm.state.value.email)
            assertEquals("password123", vm.state.value.password)
            assertEquals("Test User", vm.state.value.displayName)
        }

    @Test
    fun submit_withBlankFields_setsErrorMessage() =
        runTest(dispatcher) {
            val vm = SignUpViewModel(repo)

            vm.onAction(SignUpAction.Submit)
            assertEquals("모든 항목을 입력해주세요.", vm.state.value.errorMessage)
            assertNull(repo.lastSignUpEmail)
        }

    @Test
    fun submit_success_emitsNavigateToHomeEffect() =
        runTest(dispatcher) {
            repo.signUpResult = Result.success(sampleUser)
            val vm = SignUpViewModel(repo)

            vm.onAction(SignUpAction.ChangeEmail("user@example.com"))
            vm.onAction(SignUpAction.ChangePassword("password123"))
            vm.onAction(SignUpAction.ChangeDisplayName("User"))

            vm.effects.test {
                vm.onAction(SignUpAction.Submit)
                advanceUntilIdle()

                assertEquals(SignUpEffect.NavigateToHome, awaitItem())
                assertFalse(vm.state.value.isLoading)
                assertNull(vm.state.value.errorMessage)
            }

            assertEquals("user@example.com", repo.lastSignUpEmail)
            assertEquals("password123", repo.lastSignUpPassword)
            assertEquals("User", repo.lastSignUpDisplayName)
        }

    @Test
    fun submit_failure_setsErrorMessage() =
        runTest(dispatcher) {
            repo.signUpResult = Result.failure(RuntimeException("Email already exists"))
            val vm = SignUpViewModel(repo)

            vm.onAction(SignUpAction.ChangeEmail("user@example.com"))
            vm.onAction(SignUpAction.ChangePassword("password123"))
            vm.onAction(SignUpAction.ChangeDisplayName("User"))

            vm.onAction(SignUpAction.Submit)
            advanceUntilIdle()

            assertFalse(vm.state.value.isLoading)
            assertEquals("Email already exists", vm.state.value.errorMessage)
        }

    @Test
    fun navigateToLogin_emitsEffect() =
        runTest(dispatcher) {
            val vm = SignUpViewModel(repo)

            vm.effects.test {
                vm.onAction(SignUpAction.NavigateToLogin)
                assertEquals(SignUpEffect.NavigateToLogin, awaitItem())
            }
        }

    @Test
    fun dismissError_clearsErrorMessage() =
        runTest(dispatcher) {
            val vm = SignUpViewModel(repo)

            vm.onAction(SignUpAction.Submit)
            assertTrue(vm.state.value.errorMessage != null)

            vm.onAction(SignUpAction.DismissError)
            assertNull(vm.state.value.errorMessage)
        }
}
