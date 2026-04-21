package com.ict.spentopia.feature.market

// ------------------------------------------------------------
// MarketUiState.kt
// ------------------------------------------------------------
// 이 파일은 NFT 마켓 화면에서 사용하는 모든 상태를
// 한 곳에 모아서 관리하는 데이터 클래스입니다.
//
// 왜 필요하냐?
// - 화면 상태를 한 객체로 묶어서 관리하면
//   ViewModel과 UI 연결이 훨씬 쉬워집니다.
// - 나중에 Supabase 연결 시 구조 유지가 쉽습니다.
// ------------------------------------------------------------

// ------------------------------------------------------------
// 마켓 상단 탭 상태 정의
// ------------------------------------------------------------
enum class MarketTab {
    MARKET, // 전체 마켓
    MY_SELL // 내 판매 목록
}

// ------------------------------------------------------------
// 마켓 아이템 UI 모델
// ------------------------------------------------------------
data class MarketItemUi(
    val emoji: String, // 아이템 대표 이모지
    val title: String, // 아이템 이름
    val seller: String, // 판매자 정보
    val price: String, // 가격
    val rarity: String, // 희귀도
    val time: String // 등록 시간
)

// ------------------------------------------------------------
// 전체 UI 상태
// ------------------------------------------------------------
data class MarketUiState(

    val selectedTab: MarketTab = MarketTab.MARKET, // 현재 선택된 탭
    val searchText: String = "", // 검색어 상태

    val marketItems: List<MarketItemUi> = emptyList(), // 전체 마켓 아이템
    val mySellItems: List<MarketItemUi> = emptyList() // 내 판매 목록
)