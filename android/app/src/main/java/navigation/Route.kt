package com.ict.spentopia.navigation

// ------------------------------------------------------------
// Route.kt
// ------------------------------------------------------------
// 이 파일은 앱에서 사용하는 모든 네비게이션 route를
// 한 곳에서 관리하는 파일입니다.
// ------------------------------------------------------------

sealed class Route(val route: String) {

    // 스플래시 화면 route입니다.
    data object Splash : Route("splash")

    // 로그인 화면 route입니다.
    data object Login : Route("login")

    // --- 추가된 부분: 이메일/비밀번호 찾기 ---
    // 이메일 찾기 화면 route입니다.
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

    // 수정  : 마이 페이지 + 내 아바타 통합 화면 route 추가
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
    data object CommunityWrite : Route("community_write")

    // ------------------------------------------------------------
    // 커뮤니티 상세 화면 route입니다.
    // ------------------------------------------------------------
    data object CommunityDetail : Route("community_detail/{postId}") {
        fun createRoute(postId: Int): String {
            return "community_detail/$postId"
        }
    }
}
