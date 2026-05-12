package com.ict.spentopia.navigation // 이 파일이 속한 패키지 위치를 적음

// 하단 네비게이션 바 UI
import androidx.compose.material3.NavigationBar // NavigationBar 기능을 가져옴

// 하단 메뉴 한 칸씩 만드는 아이템
import androidx.compose.material3.NavigationBarItem // NavigationBarItem 기능을 가져옴
import androidx.compose.material3.NavigationBarItemDefaults // NavigationBarItemDefaults 기능을 가져옴
import androidx.compose.material3.MaterialTheme // MaterialTheme 기능을 가져옴

// 메뉴 이름을 글자로 보여주기 위한 Text
import androidx.compose.material3.Text // 글자 표시 컴포넌트를 가져옴

// Compose 함수 표시
import androidx.compose.runtime.Composable // Compose 화면 함수 표시를 가져옴

// 화면 이동을 관리하는 네비게이션 컨트롤러
import androidx.navigation.NavController // NavController 기능을 가져옴

// 현재 어떤 화면에 있는지 상태로 확인할 때 사용
import androidx.navigation.compose.currentBackStackEntryAsState // currentBackStackEntryAsState 기능을 가져옴

@Composable // 이 함수가 화면 UI를 그린다는 표시
fun BottomNavigationBar(navController: NavController) { // BottomNavigationBar 함수를 선언함

    // 하단 탭 진입점
    val items = listOf( // items 값을 저장함
        Route.Home,
        Route.Budget,
        Route.Analysis,
        Route.Community,
        Route.ProfileAvatar
    )

    // 현재 탭 상태 읽음
    val navBackStackEntry = navController.currentBackStackEntryAsState() // navBackStackEntry 값을 저장함

    // 현재 보고 있는 화면 route 저장
    val currentRoute = navBackStackEntry.value?.destination?.route // currentRoute 값을 저장함

    // 하단 네비게이션 바 영역
    NavigationBar( // Navigation Bar 함수를 실행함
        containerColor = MaterialTheme.colorScheme.surface // containerColor 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨

        // 메뉴 목록을 하나씩 꺼내서 하단 버튼으로 만듦
        items.forEach { item ->
            NavigationBarItem( // Navigation Bar Item 함수를 실행함

                // 지금 보고 있는 화면이면 선택된 상태로 표시
                selected = currentRoute == item.route, // selected 값을 정해줌

                // 메뉴 누르면 해당 화면으로 이동
                onClick = { // 눌렀을 때 실행할 함수를 정해줌
                    navController.navigate(item.route) { // 다른 화면으로 이동함
                        // 같은 화면이 중복으로 쌓이지 않게 설정
                        launchSingleTop = true // true 값을 launchSingleTop 값에 넣음
                    }
                },

                // 아이콘 자리 대신 글자 라벨 표시
                icon = { // icon 값을 정해줌
                    Text(getBottomMenuLabel(item)) // 화면에 글자를 보여줌
                },
                colors = NavigationBarItemDefaults.colors( // colors 값을 정해줌
                    selectedIconColor = MaterialTheme.colorScheme.primary, // selectedIconColor 값을 정해줌
                    selectedTextColor = MaterialTheme.colorScheme.primary, // selectedTextColor 값을 정해줌
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer, // indicatorColor 값을 정해줌
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant, // unselectedIconColor 값을 정해줌
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant // unselectedTextColor 값을 정해줌
                )
            )
        }
    }
}

// route 값에 따라 하단 메뉴 이름 반환
private fun getBottomMenuLabel(route: Route): String { // 데이터를 불러오는 함수 시작
    return when (route) { // 이 값을 함수 결과로 돌려줌

        // 홈 메뉴 이름
        Route.Home -> "홈"

        // 예산 메뉴 이름
        Route.Budget -> "예산"

        // 분석 메뉴 이름
        Route.Analysis -> "분석"

        // 커뮤니티 메뉴 이름
        Route.Community -> "커뮤니티"

        // 마이페이지 메뉴 이름
        Route.ProfileAvatar -> "마이"

        // 해당 없는 경우 빈 문자열 반환
        else -> "" // 위 조건이 아니면 이쪽을 실행함
    }
}
