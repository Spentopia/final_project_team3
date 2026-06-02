package com.ict.spentopia.data.repository

import com.funkatronics.encoders.Base58
import com.solana.publickey.ProgramDerivedAddress
import com.solana.publickey.SolanaPublicKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.math.BigInteger

private const val SPT_PROGRAM_ID = "9s5Z96GSLVgVsnj5NAZ1HoxPvaF8Re8B1LeSmcBKQv61"
private const val SPT_MINT_SEED = "spt_token_mint"
private const val SPT_DECIMALS = 1_000_000L
private const val TOKEN_PROGRAM_ID = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA"
private const val ASSOCIATED_TOKEN_PROGRAM_ID = "ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL"
private const val DEVNET_RPC_URL = "https://api.devnet.solana.com"

class SptBalanceRepository {
    private val httpClient = OkHttpClient()

    suspend fun getSptBalance(walletAddress: String): Long = withContext(Dispatchers.IO) {
        if (walletAddress.isBlank()) return@withContext 0L

        val sptMint = deriveSptMintAddress()
        val tokenAccount = deriveAssociatedTokenAddress(walletAddress, sptMint.base58())
        fetchTokenBalance(tokenAccount.base58())
    }

    private suspend fun deriveSptMintAddress(): SolanaPublicKey {
        return ProgramDerivedAddress
            .find(
                listOf(SPT_MINT_SEED.encodeToByteArray()),
                SolanaPublicKey.from(SPT_PROGRAM_ID)
            )
            .getOrNull()
            ?: throw IllegalStateException("SPT 토큰 주소를 계산하지 못했습니다.")
    }

    private suspend fun deriveAssociatedTokenAddress(ownerAddress: String, mintAddress: String): SolanaPublicKey {
        val seeds = listOf(
            Base58.decode(ownerAddress),
            Base58.decode(TOKEN_PROGRAM_ID),
            Base58.decode(mintAddress)
        )

        return ProgramDerivedAddress
            .find(seeds, SolanaPublicKey.from(ASSOCIATED_TOKEN_PROGRAM_ID))
            .getOrNull()
            ?: throw IllegalStateException("SPT 토큰 계정 주소를 계산하지 못했습니다.")
    }

    private fun fetchTokenBalance(tokenAccountAddress: String): Long {
        val requestBody = """
            {
              "jsonrpc": "2.0",
              "id": 1,
              "method": "getTokenAccountBalance",
              "params": ["$tokenAccountAddress"]
            }
        """.trimIndent().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(DEVNET_RPC_URL)
            .post(requestBody)
            .build()

        val response = httpClient.newCall(request).execute()
        response.use { res ->
            val bodyText = res.body?.string().orEmpty()
            if (!res.isSuccessful) {
                throw IllegalStateException("SPT 잔액을 불러오지 못했습니다.")
            }

            val root = JSONObject(bodyText)
            if (root.has("error")) return 0L

            val amountText = root
                .optJSONObject("result")
                ?.optJSONObject("value")
                ?.optString("amount")
                .orEmpty()
                .ifBlank { "0" }

            return BigInteger(amountText)
                .divide(BigInteger.valueOf(SPT_DECIMALS))
                .toLong()
        }
    }
}
