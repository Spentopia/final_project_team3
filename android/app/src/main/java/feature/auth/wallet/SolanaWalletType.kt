package com.ict.spentopia.feature.auth.wallet // 이 파일이 속한 패키지 위치를 적음

enum class SolanaWalletType( // SolanaWalletType에서 고를 수 있는 값들을 묶음
    val title: String, // 제목을 저장함
    val description: String // description 값을 저장함
) { // 이 블록 안의 내용이 시작됨
    PHANTOM("PHANTOM", "연결하려면 클릭"), // PHANTOM 함수를 실행함
    SOLFLARE("SOLFLARE", "연결하려면 클릭"), // SOLFLARE 함수를 실행함
    BACKPACK("BACKPACK", "연결하려면 클릭") // BACKPACK 함수를 실행함
}
