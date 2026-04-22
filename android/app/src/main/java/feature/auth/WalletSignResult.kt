package feature.auth

data class WalletSignResult(
    val walletAddress: String,
    val signature: String
)