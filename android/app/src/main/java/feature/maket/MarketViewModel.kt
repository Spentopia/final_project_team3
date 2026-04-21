package com.ict.spentopia.feature.market

import androidx.compose.runtime.getValue // 상태 읽기
import androidx.compose.runtime.mutableStateOf // 상태 저장
import androidx.compose.runtime.setValue // 상태 변경
import androidx.lifecycle.ViewModel // ViewModel 사용

// ------------------------------------------------------------
// MarketViewModel.kt
// ------------------------------------------------------------
// 이 ViewModel은 NFT 마켓 화면의 상태와 로직을 담당합니다.
//
// 역할:
// - 탭 변경
// - 검색어 변경
// - 아이템 목록 관리
//
// 나중에:
// - Supabase API 연결
// - 지갑 연동
// - 구매/판매 로직
// 추가될 예정입니다.
// ------------------------------------------------------------

class MarketViewModel : ViewModel() {

    // --------------------------------------------------------
    // UI 상태
    // --------------------------------------------------------
    var uiState by mutableStateOf(
        MarketUiState(
            marketItems = dummyMarketItems(), // 초기 더미 데이터
            mySellItems = dummyMySellItems()
        )
    )
        private set // 외부에서 직접 수정 못하게 막음

    // --------------------------------------------------------
    // 탭 변경
    // --------------------------------------------------------
    fun onTabChange(tab: MarketTab) {
        uiState = uiState.copy(
            selectedTab = tab // 선택된 탭 변경
        )
    }

    // --------------------------------------------------------
    // 검색어 변경
    // --------------------------------------------------------
    fun onSearchChange(text: String) {
        uiState = uiState.copy(
            searchText = text // 검색어 상태 변경
        )
    }

    // --------------------------------------------------------
    // 더미 데이터 (임시)
    // --------------------------------------------------------
    private fun dummyMarketItems(): List<MarketItemUi> {
        return listOf(
            MarketItemUi("👩", "빨간머리", "판매자: user123", "500 SPT", "에픽", "2시간 전"),
            MarketItemUi("👑", "왕관", "판매자: collector99", "1500 SPT", "전설", "5시간 전"),
            MarketItemUi("🛡️", "갑옷", "판매자: warrior", "2000 SPT", "전설", "1일 전"),
            MarketItemUi("😍", "하트 눈", "판매자: lovely_user", "300 SPT", "레어", "3시간 전")
        )
    }

    private fun dummyMySellItems(): List<MarketItemUi> {
        return listOf(
            MarketItemUi("🎩", "마술 모자", "내 등록 아이템", "700 SPT", "일반", "방금 전"),
            MarketItemUi("🦋", "나비 날개", "내 등록 아이템", "1200 SPT", "에픽", "30분 전")
        )
    }
}