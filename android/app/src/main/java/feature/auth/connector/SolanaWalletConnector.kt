package com.ict.spentopia.feature.auth.connector // 이 파일이 속한 패키지 위치를 적음

import com.solana.mobilewalletadapter.clientlib.ActivityResultSender // ActivityResultSender 기능을 가져옴

interface SolanaWalletConnector { // SolanaWalletConnector에서 꼭 만들어야 할 함수 규칙을 정함
    suspend fun connect( // connect 함수를 선언함
        walletActivityResultSender: ActivityResultSender // 지갑 관련 값을 받음
    ): WalletConnectionResult

    suspend fun signMessage( // signMessage 함수를 선언함
        walletActivityResultSender: ActivityResultSender, // 지갑 관련 값을 받음
        message: ByteArray // 메시지를 받음
    ): WalletSignResult
}