package com.kotlincalendar.client

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.kotlincalendar.client.presentation.app.AppViewModel
import com.kotlincalendar.client.presentation.auth.login.LoginRoute
import com.kotlincalendar.client.presentation.auth.signup.SignUpRoute
import com.kotlincalendar.client.presentation.calendar.CalendarRoute
import com.kotlincalendar.client.presentation.event.create.EventCreateRoute
import com.kotlincalendar.client.presentation.event.detail.EventDetailRoute
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable
data object LoginDestination

@Serializable
data object SignUpDestination

@Serializable
data object CalendarDestination

/** [date]는 `yyyy-MM-dd`. 내비게이션 인자는 직렬화 가능한 기본 타입만 쓴다. */
@Serializable
data class EventCreateDestination(
    val date: String,
)

@Serializable
data class EventDetailDestination(
    val id: String,
)

@Composable
fun App(appViewModel: AppViewModel = koinViewModel()) {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            val appState by appViewModel.state.collectAsStateWithLifecycle()
            if (appState.isRestoringSession) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Surface
            }

            val navController = rememberNavController()
            // 일정 생성·삭제 후 캘린더가 다시 불러오도록 바꾸는 값. null이면 처음 진입이라 새로고침하지 않는다.
            var calendarRefreshKey by remember { mutableStateOf<Int?>(null) }
            val refreshCalendar = { calendarRefreshKey = (calendarRefreshKey ?: 0) + 1 }

            NavHost(
                navController = navController,
                startDestination = if (appState.isLoggedIn) CalendarDestination else LoginDestination,
            ) {
                composable<LoginDestination> {
                    LoginRoute(
                        onNavigateToHome = {
                            navController.navigate(CalendarDestination) {
                                popUpTo(LoginDestination) { inclusive = true }
                            }
                        },
                        onNavigateToSignUp = {
                            navController.navigate(SignUpDestination)
                        },
                    )
                }

                composable<SignUpDestination> {
                    SignUpRoute(
                        onNavigateToHome = {
                            navController.navigate(CalendarDestination) {
                                popUpTo(LoginDestination) { inclusive = true }
                            }
                        },
                        onNavigateToLogin = {
                            navController.popBackStack()
                        },
                    )
                }

                composable<CalendarDestination> {
                    CalendarRoute(
                        onCreateEvent = { date -> navController.navigate(EventCreateDestination(date.toString())) },
                        onEventClick = { id -> navController.navigate(EventDetailDestination(id)) },
                        refreshKey = calendarRefreshKey,
                    )
                }

                composable<EventCreateDestination> { entry ->
                    val destination = entry.toRoute<EventCreateDestination>()
                    EventCreateRoute(
                        initialDate = LocalDate.parse(destination.date),
                        onCreated = {
                            refreshCalendar()
                            navController.popBackStack()
                        },
                        onBack = { navController.popBackStack() },
                    )
                }

                composable<EventDetailDestination> { entry ->
                    val destination = entry.toRoute<EventDetailDestination>()
                    EventDetailRoute(
                        viewModel = koinViewModel { parametersOf(destination.id) },
                        onBack = { navController.popBackStack() },
                        onDeleted = {
                            refreshCalendar()
                            navController.popBackStack()
                        },
                    )
                }
            }
        }
    }
}
