package com.ict.spentopia.navigation

// ------------------------------------------------------------
// Route.kt
// ------------------------------------------------------------
// 이 파일은 앱에서 사용하는 모든 네비게이션 route를
// 한 곳에서 관리하는 파일입니다.
//
// 이번 수정 내용:
// - 커뮤니티 글쓰기 화면 route 유지
// - 커뮤니티 상세 화면 route 추가
//
// 왜 상세 route를 따로 만드나?
// - 게시글 카드 클릭 시 상세 화면으로 이동해야 합니다.
// - 어떤 게시글을 눌렀는지 구분하려면 postId가 필요합니다.
// - 그래서 route 안에 postId를 넣어 전달합니다.
// ------------------------------------------------------------

sealed class Route(val route: String) {

    // 로그인 화면 route입니다.
    data object Login : Route("login")

    // 회원가입 1단계 화면 route입니다.
    data object SignUpStep1 : Route("signup_step1")

    // 회원가입 2단계 화면 route입니다.
    data object SignUpStep2 : Route("signup_step2")

    // 회원가입 3단계 화면 route입니다.
    data object SignUpStep3 : Route("signup_step3")

    // 홈 화면 route입니다.
    data object Home : Route("home")

    // 가계부 화면 route입니다.
    data object Ledger : Route("ledger")

    // 마이페이지 화면 route입니다.
    data object MyPage : Route("mypage")

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

    // 커뮤니티 글쓰기 화면 route입니다.
    data object CommunityWrite : Route("community_write")

    // ------------------------------------------------------------
    // 커뮤니티 상세 화면 route입니다.
    // ------------------------------------------------------------
    // {postId} 부분은 "어떤 게시글인지" 구분하기 위한 자리입니다.
    // 예를 들면 실제 이동할 때는 아래처럼 됩니다.
    //
    // community_detail/3
    // community_detail/10
    //
    // 이렇게 postId를 함께 넘겨야 상세 화면에서
    // "3번 글", "10번 글"을 구분할 수 있습니다.
    // ------------------------------------------------------------
    data object CommunityDetail : Route("community_detail/{postId}") {

        // --------------------------------------------------------
        // createRoute 함수는 실제 이동할 때 사용할 route 문자열을 만들어줍니다.
        //
        // 예:
        // createRoute(3)
        // -> "community_detail/3"
        // --------------------------------------------------------
        fun createRoute(postId: Int): String {
            return "community_detail/$postId"
        }
    }
}