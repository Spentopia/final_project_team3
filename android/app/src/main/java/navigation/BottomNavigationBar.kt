package com.ict.spentopia.navigation

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomNavigationBar(navController: NavController) {

    val items = listOf(
        Route.Home,
        Route.Budget,
        Route.Analysis,
        Route.Community,
        Route.MyPage
    )

    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route

    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        launchSingleTop = true
                    }
                },
                icon = {
                    Text(getBottomMenuLabel(item))
                }
            )
        }
    }
}

private fun getBottomMenuLabel(route: Route): String {
    return when (route) {
        Route.Home -> "홈"
        Route.Budget -> "예산"
        Route.Analysis -> "분석"
        Route.Community -> "커뮤니티"
        Route.MyPage -> "마이"
        else -> ""
    }
}