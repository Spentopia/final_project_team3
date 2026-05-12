package com.ict.spentopia.feature.market // 이 파일이 속한 패키지 위치를 적음

import androidx.compose.runtime.getValue // by로 상태를 읽게 해줌
import androidx.compose.runtime.mutableStateOf // 화면 상태를 만드는 도구를 가져옴
import androidx.compose.runtime.setValue // by로 상태를 바꾸게 해줌
import androidx.lifecycle.ViewModel // ViewModel 기능을 가져옴

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

class MarketViewModel : ViewModel() { // MarketViewModel 기능을 묶어둔 클래스 시작

    // --------------------------------------------------------
    // UI 상태
    // --------------------------------------------------------
    var uiState by mutableStateOf( // 화면에서 바뀔 화면 상태를 저장함
        MarketUiState( // Market Ui State 함수를 실행함
            marketItems = dummyMarketItems(), // 마켓 관련 값을 정해줌
            mySellItems = dummyMySellItems() // mySellItems 값을 정해줌
        )
    )
        private set

    // --------------------------------------------------------
    // 탭 변경
    // --------------------------------------------------------
    fun onTabChange(tab: MarketTab) { // onTabChange 함수를 선언함
        uiState = uiState.copy( // 화면 상태를 정해줌
            selectedTab = tab // tab 값을 selectedTab 값에 넣음
        )
    }

    // --------------------------------------------------------
    // 검색어 변경
    // --------------------------------------------------------
    fun onSearchChange(text: String) { // onSearchChange 함수를 선언함
        uiState = uiState.copy( // 화면 상태를 정해줌
            searchText = text // text 값을 searchText 값에 넣음
        )
    }

    // --------------------------------------------------------
    // 더미 데이터 (임시)
    // --------------------------------------------------------
    private fun dummyMarketItems(): List<MarketItemUi> { // dummyMarketItems 함수를 선언함
        return listOf( // 이 값을 함수 결과로 돌려줌
            MarketItemUi("👩", "빨간머리", "판매자: user123", "500 SPT", "에픽", "2시간 전"), // Market Item Ui 함수를 실행함
            MarketItemUi("👑", "왕관", "판매자: collector99", "1500 SPT", "전설", "5시간 전"), // Market Item Ui 함수를 실행함
            MarketItemUi("🛡️", "갑옷", "판매자: warrior", "2000 SPT", "전설", "1일 전"), // Market Item Ui 함수를 실행함
            MarketItemUi("😍", "하트 눈", "판매자: lovely_user", "300 SPT", "레어", "3시간 전") // Market Item Ui 함수를 실행함
        )
    }

    private fun dummyMySellItems(): List<MarketItemUi> { // dummyMySellItems 함수를 선언함
        return listOf( // 이 값을 함수 결과로 돌려줌
            MarketItemUi("🎩", "마술 모자", "내 등록 아이템", "700 SPT", "일반", "방금 전"), // Market Item Ui 함수를 실행함
            MarketItemUi("🦋", "나비 날개", "내 등록 아이템", "1200 SPT", "에픽", "30분 전") // Market Item Ui 함수를 실행함
        )
    }
}