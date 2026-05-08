package com.ict.spentopia.feature.auth.wallet

enum class SolanaWalletType(
    val title: String,
    val description: String
) {
    PHANTOM("PHANTOM", "연결하려면 클릭"),
    SOLFLARE("SOLFLARE", "연결하려면 클릭"),
    BACKPACK("BACKPACK", "연결하려면 클릭")
}
