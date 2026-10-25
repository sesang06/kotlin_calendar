package com.kotlincalendar.client

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kotlincalendar.client.presentation.auth.login.LoginRoute
import com.kotlincalendar.client.presentation.auth.signup.SignUpRoute
import kotlinx.serialization.Serializable

@Serializable
data object LoginDestination

@Serializable
data object SignUpDestination

@Serializable
data object CalendarDestination

@Composable
fun App() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            val navController = rememberNavController()

            NavHost(
                navController = navController,
                startDestination = LoginDestination,
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
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "캘린더 화면",
                            style = MaterialTheme.typography.headlineMedium,
                        )
                    }
                }
            }
        }
    }
}
