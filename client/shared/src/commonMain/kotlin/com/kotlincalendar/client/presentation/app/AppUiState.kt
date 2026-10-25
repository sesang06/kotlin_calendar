package com.kotlincalendar.client.presentation.app

data class AppUiState(
    val isRestoringSession: Boolean = true,
    /** 앱을 열었을 때 로그인 상태였는지. 첫 화면을 정할 때만 쓴다. */
    val startLoggedIn: Boolean = false,
    /** 현재 로그인 상태. 로그아웃·세션 만료 시 false가 되어 로그인 화면으로 보낸다. */
    val isLoggedIn: Boolean = false,
)
