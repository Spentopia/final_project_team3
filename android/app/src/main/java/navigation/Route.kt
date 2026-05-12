package com.ict.spentopia.navigation // 이 파일이 속한 패키지 위치를 적음

// ------------------------------------------------------------
// Route.kt
// ------------------------------------------------------------
// 이 파일은 앱에서 사용하는 모든 네비게이션 route를
// 한 곳에서 관리하는 파일입니다.
// ------------------------------------------------------------

sealed class Route(val route: String) { // Route 결과 종류를 정해진 것만 쓰게 묶음

    // 스플래시 화면 route입니다.
    data object Splash : Route("splash")

    // 로그인 화면 route입니다.
    data object Login : Route("login")

    // 로그인 보조 route임
    // 계정 찾기/비번 재설정용
    data object FindEmail : Route("find_email")

    // 비밀번호 찾기 화면 route입니다.
    data object FindPassword : Route("find_password")
    // ------------------------------------

    // 홈 화면 route입니다.
    data object Home : Route("home")

    // 가계부 화면 route입니다.
    data object Ledger : Route("ledger")

    // 마이페이지 화면 route입니다.
    data object MyPage : Route("mypage")

    // 마이페이지+내아바타 통합 route
    data object ProfileAvatar : Route("profile_avatar")

    // 예산 설정 화면 route입니다.
    data object Budget : Route("budget")

    // 소비 분석 화면 route입니다.
    data object Analysis : Route("analysis")

    // 아바타 화면 route입니다.
    data object Avatar : Route("avatar")

    // NFT 마켓 화면 route입니다.
    data object Market : Route("market")

    // 광장 화면 route입니다.
    data object Plaza : Route("plaza")

    // 커뮤니티 메인 화면 route입니다.
    data object Community : Route("community")

    // AI 챗봇 화면 route입니다.
    data object Chatbot : Route("chatbot")

    // 커뮤니티 글쓰기 화면 route입니다.
    data object CommunityWrite : Route("community_write?category={category}&contestId={contestId}") { // 커뮤니티 관련 값을 정해줌
        private const val baseRoute = "community_write" // baseRoute 값을 저장함

        fun createRoute(category: String? = null, contestId: String? = null): String { // 데이터를 저장하는 함수 시작
            val params = buildList { // params 값을 저장함
                if (!category.isNullOrBlank()) add("category=$category") // 조건이 맞는지 확인함
                if (!contestId.isNullOrBlank()) add("contestId=$contestId") // 조건이 맞는지 확인함
            }
            return if (params.isEmpty()) baseRoute else "$baseRoute?${params.joinToString("&")}" // 이 값을 함수 결과로 돌려줌
        }
    }
}
