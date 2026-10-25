package com.kotlincalendar.client

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kotlincalendar.client.presentation.app.AppViewModel
import com.kotlincalendar.client.presentation.auth.login.LoginRoute
import com.kotlincalendar.client.presentation.auth.signup.SignUpRoute
import com.kotlincalendar.client.presentation.calendar.CalendarRoute
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Serializable
data object LoginDestination

@Serializable
data object SignUpDestination

@Serializable
data object CalendarDestination

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
                        onCreateEvent = {},
                        onEventClick = {},
                    )
                }
            }
        }
    }
}
