package com.ict.spentopia.navigation

// 하단 네비게이션 바 UI
import androidx.compose.material3.NavigationBar

// 하단 메뉴 한 칸씩 만드는 아이템
import androidx.compose.material3.NavigationBarItem

// 메뉴 이름을 글자로 보여주기 위한 Text
import androidx.compose.material3.Text

// Compose 함수 표시
import androidx.compose.runtime.Composable

// 화면 이동을 관리하는 네비게이션 컨트롤러
import androidx.navigation.NavController

// 현재 어떤 화면에 있는지 상태로 확인할 때 사용
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomNavigationBar(navController: NavController) {

    // 하단 바에 보여줄 메뉴 목록
    val items = listOf(
        Route.Home,
        Route.Budget,
        Route.Analysis,
        Route.Community,
        Route.MyPage
    )

    // 현재 네비게이션 상태 가져옴
    val navBackStackEntry = navController.currentBackStackEntryAsState()

    // 현재 보고 있는 화면 route 저장
    val currentRoute = navBackStackEntry.value?.destination?.route

    // 하단 네비게이션 바 영역
    NavigationBar {

        // 메뉴 목록을 하나씩 꺼내서 하단 버튼으로 만듦
        items.forEach { item ->
            NavigationBarItem(

                // 지금 보고 있는 화면이면 선택된 상태로 표시
                selected = currentRoute == item.route,

                // 메뉴 누르면 해당 화면으로 이동
                onClick = {
                    navController.navigate(item.route) {
                        // 같은 화면이 중복으로 쌓이지 않게 설정
                        launchSingleTop = true
                    }
                },

                // 아이콘 자리 대신 글자 라벨 표시
                icon = {
                    Text(getBottomMenuLabel(item))
                }
            )
        }
    }
}

// route 값에 따라 하단 메뉴 이름 반환
private fun getBottomMenuLabel(route: Route): String {
    return when (route) {

        // 홈 메뉴 이름
        Route.Home -> "홈"

        // 예산 메뉴 이름
        Route.Budget -> "예산"

        // 분석 메뉴 이름
        Route.Analysis -> "분석"

        // 커뮤니티 메뉴 이름
        Route.Community -> "커뮤니티"

        // 마이페이지 메뉴 이름
        Route.MyPage -> "마이"

        // 해당 없는 경우 빈 문자열 반환
        else -> ""
    }
}