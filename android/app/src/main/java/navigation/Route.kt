package com.ict.spentopia.navigation

sealed class Route(val route: String) {
    data object Login : Route("login")
    data object SignUpStep1 : Route("signup_step1")
    data object SignUpStep2 : Route("signup_step2")
    data object SignUpStep3 : Route("signup_step3")
    data object Home : Route("home")
    data object Ledger : Route("ledger")
    data object MyPage : Route("mypage")
}