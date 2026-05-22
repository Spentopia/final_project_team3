package com.ict.spentopia.feature.analysis.payment

import android.net.Uri
import android.util.Base64
import android.util.Log
import com.funkatronics.encoders.Base58
import com.google.gson.Gson
import com.ict.spentopia.data.remote.Solana402Body
import com.ict.spentopia.data.remote.SolanaPaymentRequirement
import com.ict.spentopia.feature.auth.wallet.SolanaWalletType
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solana.mobilewalletadapter.clientlib.ConnectionIdentity
import com.solana.mobilewalletadapter.clientlib.MobileWalletAdapter
import com.solana.mobilewalletadapter.clientlib.Solana
import com.solana.mobilewalletadapter.clientlib.TransactionResult
import com.solana.mobilewalletadapter.clientlib.successPayload
import com.solana.publickey.ProgramDerivedAddress
import com.solana.publickey.SolanaPublicKey
import com.solana.transaction.AccountMeta
import com.solana.transaction.Message
import com.solana.transaction.Transaction
import com.solana.transaction.TransactionInstruction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val TOKEN_PROGRAM_ID = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA"
private const val ASSOCIATED_TOKEN_PROGRAM_ID = "ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL"
private const val SYSTEM_PROGRAM_ID = "11111111111111111111111111111111"
private const val MEMO_PROGRAM_ID = "MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr"
private const val DEVNET_RPC_URL = "https://api.devnet.solana.com"
private const val MAINNET_RPC_URL = "https://api.mainnet-beta.solana.com"

data class X402PaymentResult(
    val paymentHeader: String,
    val signature: String,
    val network: String
)

data class PreparedX402Payment(
    val serializedTransaction: ByteArray,
    val network: String
)

class SolanaX402PaymentSender {
    private val gson = Gson()
    private val httpClient = OkHttpClient()

    suspend fun sendPayment(
        walletActivityResultSender: ActivityResultSender,
        walletProvider: String,
        walletAddress: String,
        walletAuthToken: String,
        body: Solana402Body
    ): Result<X402PaymentResult> {
        return try {
            val requirement = body.accepts.firstOrNull { requirement ->
                requirement.network == "solana-devnet" || requirement.network == "mainnet-beta"
            } ?: return Result.failure(IllegalStateException("결제 정보를 불러오지 못했습니다."))

            val paymentMemo = requirement.extra
                ?.get("paymentMemo")
                ?.toString()
                .orEmpty()

            if (paymentMemo.isBlank()) {
                return Result.failure(IllegalStateException("결제 요청을 확인하지 못했습니다."))
            }

            val signature = signAndSendUsdcPayment(
                walletActivityResultSender = walletActivityResultSender,
                walletType = resolveWalletType(walletProvider),
                walletAddress = walletAddress,
                walletAuthToken = walletAuthToken,
                requirement = requirement,
                paymentMemo = paymentMemo
            )

            Result.success(
                X402PaymentResult(
                    paymentHeader = encodePaymentHeader(
                        signature = signature,
                        buyerAddress = walletAddress,
                        network = requirement.network
                    ),
                    signature = signature,
                    network = requirement.network
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun preparePaymentTransaction(
        walletAddress: String,
        body: Solana402Body
    ): Result<PreparedX402Payment> {
        return try {
            val requirement = body.accepts.firstOrNull { requirement ->
                requirement.network == "solana-devnet" || requirement.network == "mainnet-beta"
            } ?: return Result.failure(IllegalStateException("결제 정보를 불러오지 못했습니다."))

            val paymentMemo = requirement.extra
                ?.get("paymentMemo")
                ?.toString()
                .orEmpty()

            if (paymentMemo.isBlank()) {
                return Result.failure(IllegalStateException("결제 요청을 확인하지 못했습니다."))
            }

            val blockhash = fetchLatestBlockhash(requirement.network)
            val sender = SolanaPublicKey.from(walletAddress)
            val transaction = buildUsdcPaymentTransaction(
                blockhash = blockhash,
                sender = sender,
                senderAddress = walletAddress,
                recipientAddress = requirement.payTo,
                mintAddress = requirement.asset,
                amount = requirement.maxAmountRequired.toLongOrNull()
                    ?: throw IllegalStateException("결제 금액을 확인하지 못했습니다."),
                paymentMemo = paymentMemo
            )

            Result.success(
                PreparedX402Payment(
                    serializedTransaction = transaction.serialize(),
                    network = requirement.network
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun buildPaymentHeader(signature: String, buyerAddress: String, network: String): String {
        return encodePaymentHeader(
            signature = signature,
            buyerAddress = buyerAddress,
            network = network
        )
    }

    suspend fun sendSignedTransaction(network: String, signedTransaction: ByteArray): String = withContext(Dispatchers.IO) {
        Log.d("SpentopiaPayment", "sendTransaction start network=$network bytes=${signedTransaction.size}")
        val transactionBase64 = Base64.encodeToString(signedTransaction, Base64.NO_WRAP)
        val requestBody = """
            {
              "jsonrpc": "2.0",
              "id": 1,
              "method": "sendTransaction",
              "params": [
                "$transactionBase64",
                {
                  "encoding": "base64",
                  "skipPreflight": false,
                  "preflightCommitment": "confirmed",
                  "maxRetries": 3
                }
              ]
            }
        """.trimIndent().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(resolveRpcUrl(network))
            .post(requestBody)
            .build()

        val response = httpClient.newCall(request).execute()
        response.use { res ->
            val bodyText = res.body?.string().orEmpty()
            if (!res.isSuccessful) {
                Log.e("SpentopiaPayment", "sendTransaction http failed code=${res.code} body=$bodyText")
                throw IllegalStateException("결제 트랜잭션을 전송하지 못했습니다.")
            }

            val root = gson.fromJson(bodyText, Map::class.java)
            val error = root["error"] as? Map<*, *>
            if (error != null) {
                val message = error["message"] as? String
                Log.e("SpentopiaPayment", "sendTransaction rpc error=$error")
                if (message.orEmpty().lowercase().contains("insufficient") ||
                    message.orEmpty().lowercase().contains("custom program error: 0x1")
                ) {
                    throw IllegalStateException("USDC 또는 SOL 잔액이 부족합니다. 지갑에서 잔액을 확인해주세요.")
                }
                throw IllegalStateException(message ?: "결제 트랜잭션을 전송하지 못했습니다.")
            }

            val signature = root["result"] as? String
                ?: throw IllegalStateException("결제 트랜잭션 서명 결과를 확인하지 못했습니다.")
            Log.d("SpentopiaPayment", "sendTransaction success signature=$signature")
            signature
        }
    }

    suspend fun waitForSignatureConfirmed(network: String, signature: String) {
        val deadline = System.currentTimeMillis() + 60_000L
        while (System.currentTimeMillis() < deadline) {
            val status = fetchSignatureStatus(network, signature)
            val err = status?.get("err")
            if (err != null) {
                throw IllegalStateException("결제 트랜잭션이 실패했습니다. 지갑 내역을 확인한 뒤 다시 시도해주세요.")
            }

            val confirmationStatus = status?.get("confirmationStatus") as? String
            if (confirmationStatus == "confirmed" || confirmationStatus == "finalized") {
                return
            }

            delay(1500L)
        }

        throw IllegalStateException("결제 확인이 지연되고 있습니다. 잠시 후 다시 시도해주세요.")
    }

    private suspend fun signAndSendUsdcPayment(
        walletActivityResultSender: ActivityResultSender,
        walletType: SolanaWalletType,
        walletAddress: String,
        walletAuthToken: String,
        requirement: SolanaPaymentRequirement,
        paymentMemo: String
    ): String {
        val walletAdapter = MobileWalletAdapter(
            connectionIdentity = ConnectionIdentity(
                identityUri = Uri.parse("https://spentopia.net"),
                iconUri = Uri.parse("icon.png"),
                identityName = "Spentopia"
            )
        )
        walletAdapter.blockchain = if (requirement.network == "mainnet-beta") {
            Solana.Mainnet
        } else {
            Solana.Devnet
        }
        if (walletAuthToken.isNotBlank()) {
            walletAdapter.authToken = walletAuthToken
        }

        val result = walletAdapter.transact(walletActivityResultSender) { authResult ->
            val account = authResult.accounts.first()
            val sender = SolanaPublicKey(account.publicKey)
            val connectedAddress = Base58.encodeToString(account.publicKey)

            if (connectedAddress != walletAddress) {
                throw IllegalStateException("연결된 지갑 주소가 현재 계정의 지갑과 다릅니다.")
            }

            val blockhash = fetchLatestBlockhash(requirement.network)

            val transaction = buildUsdcPaymentTransaction(
                blockhash = blockhash,
                sender = sender,
                senderAddress = walletAddress,
                recipientAddress = requirement.payTo,
                mintAddress = requirement.asset,
                amount = requirement.maxAmountRequired.toLongOrNull()
                    ?: throw IllegalStateException("결제 금액을 확인하지 못했습니다."),
                paymentMemo = paymentMemo
            )

            signTransactions(arrayOf(transaction.serialize()))
        }

        return when (result) {
            is TransactionResult.Success -> {
                val signedTransaction = result.successPayload?.signedPayloads?.firstOrNull()
                    ?: throw IllegalStateException("결제 서명 결과가 비어 있습니다.")
                sendSignedTransaction(requirement.network, signedTransaction)
            }

            is TransactionResult.NoWalletFound -> {
                val walletName = if (walletType == SolanaWalletType.SOLFLARE) "Solflare" else "Phantom"
                throw IllegalStateException("${walletName} 지갑 앱을 찾을 수 없습니다.")
            }

            is TransactionResult.Failure -> {
                throw IllegalStateException(result.e.message ?: "지갑에서 결제가 취소되었습니다.")
            }
        }
    }

    private suspend fun buildUsdcPaymentTransaction(
        blockhash: String,
        sender: SolanaPublicKey,
        senderAddress: String,
        recipientAddress: String,
        mintAddress: String,
        amount: Long,
        paymentMemo: String
    ): Transaction {
        if (amount <= 0L) {
            throw IllegalStateException("결제 금액을 확인하지 못했습니다.")
        }

        val mint = SolanaPublicKey.from(mintAddress)
        val recipient = SolanaPublicKey.from(recipientAddress)
        val senderAta = deriveAssociatedTokenAddress(senderAddress, mintAddress)
        val recipientAta = deriveAssociatedTokenAddress(recipientAddress, mintAddress)

        val message = Message.Builder()
            .addInstruction(createMemoInstruction(paymentMemo))
            .addInstruction(createAssociatedTokenAccountIdempotentInstruction(sender, recipientAta, recipient, mint))
            .addInstruction(createTransferCheckedInstruction(senderAta, mint, recipientAta, sender, amount))
            .setRecentBlockhash(blockhash)
            .build()
        logMessageIndexes(message)

        return Transaction(message)
    }

    private fun logMessageIndexes(message: Message) {
        val accountKeys = message.accounts.map { it.base58() }
        Log.d("SpentopiaPayment", "tx accountKeys size=${accountKeys.size} keys=$accountKeys")
        message.instructions.forEachIndexed { index, instruction ->
            val programIndex = instruction.programIdIndex.toInt()
            val accountIndexes = instruction.accountIndices.map { it.toInt() and 0xFF }
            val indexesValid = programIndex in accountKeys.indices &&
                accountIndexes.all { accountIndex -> accountIndex in accountKeys.indices }
            Log.d(
                "SpentopiaPayment",
                "tx ix=$index programIdIndex=$programIndex accounts=$accountIndexes indexesValid=$indexesValid"
            )
        }
    }

    private fun createMemoInstruction(paymentMemo: String): TransactionInstruction {
        return TransactionInstruction(
            SolanaPublicKey.from(MEMO_PROGRAM_ID),
            emptyList(),
            paymentMemo.encodeToByteArray()
        )
    }

    private fun createAssociatedTokenAccountIdempotentInstruction(
        payer: SolanaPublicKey,
        associatedTokenAccount: SolanaPublicKey,
        owner: SolanaPublicKey,
        mint: SolanaPublicKey
    ): TransactionInstruction {
        return TransactionInstruction(
            SolanaPublicKey.from(ASSOCIATED_TOKEN_PROGRAM_ID),
            listOf(
                AccountMeta(payer, true, true),
                AccountMeta(associatedTokenAccount, false, true),
                AccountMeta(owner, false, false),
                AccountMeta(mint, false, false),
                AccountMeta(SolanaPublicKey.from(SYSTEM_PROGRAM_ID), false, false),
                AccountMeta(SolanaPublicKey.from(TOKEN_PROGRAM_ID), false, false)
            ),
            byteArrayOf(1)
        )
    }

    private fun createTransferCheckedInstruction(
        source: SolanaPublicKey,
        mint: SolanaPublicKey,
        destination: SolanaPublicKey,
        owner: SolanaPublicKey,
        amount: Long
    ): TransactionInstruction {
        val data = ByteBuffer.allocate(10)
            .order(ByteOrder.LITTLE_ENDIAN)
            .put(12.toByte())
            .putLong(amount)
            .put(6.toByte())
            .array()

        return TransactionInstruction(
            SolanaPublicKey.from(TOKEN_PROGRAM_ID),
            listOf(
                AccountMeta(source, false, true),
                AccountMeta(mint, false, false),
                AccountMeta(destination, false, true),
                AccountMeta(owner, true, true)
            ),
            data
        )
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
            ?: throw IllegalStateException("토큰 계정 주소를 계산하지 못했습니다.")
    }

    private fun encodePaymentHeader(signature: String, buyerAddress: String, network: String): String {
        val payload = gson.toJson(
            mapOf(
                "signature" to signature,
                "buyerAddress" to buyerAddress,
                "network" to network
            )
        )
        return Base64.encodeToString(payload.encodeToByteArray(), Base64.NO_WRAP)
    }

    private fun resolveRpcUrl(network: String): String {
        return if (network == "mainnet-beta") MAINNET_RPC_URL else DEVNET_RPC_URL
    }

    private suspend fun fetchLatestBlockhash(network: String): String = withContext(Dispatchers.IO) {
        val requestBody = """
            {
              "jsonrpc": "2.0",
              "id": 1,
              "method": "getLatestBlockhash",
              "params": [{"commitment": "confirmed"}]
            }
        """.trimIndent().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(resolveRpcUrl(network))
            .post(requestBody)
            .build()

        val response = httpClient.newCall(request).execute()
        response.use { res ->
            if (!res.isSuccessful) {
                throw IllegalStateException("Solana 네트워크 상태를 불러오지 못했습니다.")
            }

            val bodyText = res.body?.string().orEmpty()
            val root = gson.fromJson(bodyText, Map::class.java)
            val result = root["result"] as? Map<*, *>
            val value = result?.get("value") as? Map<*, *>
            value?.get("blockhash") as? String
                ?: throw IllegalStateException("Solana blockhash를 확인하지 못했습니다.")
        }
    }

    private suspend fun fetchSignatureStatus(network: String, signature: String): Map<*, *>? = withContext(Dispatchers.IO) {
        val requestBody = """
            {
              "jsonrpc": "2.0",
              "id": 1,
              "method": "getSignatureStatuses",
              "params": [["$signature"]]
            }
        """.trimIndent().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(resolveRpcUrl(network))
            .post(requestBody)
            .build()

        val response = httpClient.newCall(request).execute()
        response.use { res ->
            if (!res.isSuccessful) {
                throw IllegalStateException("Solana 결제 확인 상태를 불러오지 못했습니다.")
            }

            val bodyText = res.body?.string().orEmpty()
            val root = gson.fromJson(bodyText, Map::class.java)
            val result = root["result"] as? Map<*, *>
            val value = result?.get("value") as? List<*>
            value?.firstOrNull() as? Map<*, *>
        }
    }

    private fun resolveWalletType(walletProvider: String): SolanaWalletType {
        return if (walletProvider.equals("SOLFLARE", ignoreCase = true)) {
            SolanaWalletType.SOLFLARE
        } else {
            SolanaWalletType.PHANTOM
        }
    }
}
