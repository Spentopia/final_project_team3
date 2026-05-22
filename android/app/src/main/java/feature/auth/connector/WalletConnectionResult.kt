package com.ict.spentopia.feature.auth.connector // 이 파일이 속한 패키지 위치를 적음

sealed class WalletConnectionResult { // WalletConnectionResult 결과 종류를 정해진 것만 쓰게 묶음
    data class Success( // Success 데이터를 묶어둘 클래스 시작
        val walletAddress: String, // 지갑 주소를 저장함
        val authToken: String? = null // 같은 MWA 지갑 세션을 다시 쓰기 위한 토큰을 저장함
    ) : WalletConnectionResult()

    data class Failure( // Failure 데이터를 묶어둘 클래스 시작
        val message: String // 메시지를 저장함
    ) : WalletConnectionResult()
}
