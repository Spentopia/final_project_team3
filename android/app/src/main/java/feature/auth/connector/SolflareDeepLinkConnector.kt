package com.ict.spentopia.feature.auth.connector

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.iwebpp.crypto.TweetNaclFast
import org.bitcoinj.core.Base58
import org.json.JSONObject
import java.security.SecureRandom

class SolflareDeepLinkConnector(
    private val context: Context
) {
    private val tag = "SolflareDeepLink"
    private val redirectLink = "spentopia://wallet-callback"
    private val solflarePackageName = "com.solflare.mobile"
    private val prefs = context.getSharedPreferences("solflare_deeplink", Context.MODE_PRIVATE)
    private val random = SecureRandom()
    private var dappKeyPair = loadDappKeyPair() ?: TweetNaclFast.Box.keyPair()
    private var solflareEncryptionPublicKey: ByteArray? = prefs.getString(KEY_SOLFLARE_PUBLIC_KEY, null)
        ?.let { Base58.decode(it) }
    private var session: String? = prefs.getString(KEY_SESSION, null)

    fun connect(): Boolean {
        dappKeyPair = TweetNaclFast.Box.keyPair()
        solflareEncryptionPublicKey = null
        session = null
        prefs.edit()
            .putString(KEY_DAPP_SECRET_KEY, Base58.encode(dappKeyPair.secretKey))
            .putString(KEY_DAPP_PUBLIC_KEY, Base58.encode(dappKeyPair.publicKey))
            .remove(KEY_SOLFLARE_PUBLIC_KEY)
            .remove(KEY_SESSION)
            .apply()

        val uri = Uri.parse("https://solflare.com/ul/v1/connect")
            .buildUpon()
            .appendQueryParameter("app_url", "https://spentopia.net")
            .appendQueryParameter("cluster", "devnet")
            .appendQueryParameter(
                "dapp_encryption_public_key",
                Base58.encode(dappKeyPair.publicKey)
            )
            .appendQueryParameter("redirect_link", redirectLink)
            .build()

        return openSolflare(uri)
    }

    fun signMessage(message: String): Boolean {
        val currentSession = session ?: return false
        val currentSolflarePublicKey = solflareEncryptionPublicKey ?: return false
        val payload = JSONObject()
            .put("message", Base58.encode(message.toByteArray(Charsets.UTF_8)))
            .put("session", currentSession)
            .put("display", "utf8")
            .toString()
        val nonce = newNonce()
        val encryptedPayload = TweetNaclFast.Box(
            currentSolflarePublicKey,
            dappKeyPair.secretKey
        ).box(payload.toByteArray(Charsets.UTF_8), nonce)

        val uri = Uri.parse("https://solflare.com/ul/v1/signMessage")
            .buildUpon()
            .appendQueryParameter(
                "dapp_encryption_public_key",
                Base58.encode(dappKeyPair.publicKey)
            )
            .appendQueryParameter("nonce", Base58.encode(nonce))
            .appendQueryParameter("redirect_link", redirectLink)
            .appendQueryParameter("payload", Base58.encode(encryptedPayload))
            .build()

        return openSolflare(uri)
    }

    fun signAndSendTransaction(serializedTransaction: ByteArray): Boolean {
        return signTransaction(serializedTransaction)
    }

    fun signTransaction(serializedTransaction: ByteArray): Boolean {
        val currentSession = session ?: return false
        val currentSolflarePublicKey = solflareEncryptionPublicKey ?: return false
        val payload = JSONObject()
            .put("transaction", Base58.encode(serializedTransaction))
            .put("session", currentSession)
            .toString()
        val nonce = newNonce()
        val encryptedPayload = TweetNaclFast.Box(
            currentSolflarePublicKey,
            dappKeyPair.secretKey
        ).box(payload.toByteArray(Charsets.UTF_8), nonce)

        val uri = Uri.parse("https://solflare.com/ul/v1/signTransaction")
            .buildUpon()
            .appendQueryParameter(
                "dapp_encryption_public_key",
                Base58.encode(dappKeyPair.publicKey)
            )
            .appendQueryParameter("nonce", Base58.encode(nonce))
            .appendQueryParameter("redirect_link", redirectLink)
            .appendQueryParameter("payload", Base58.encode(encryptedPayload))
            .build()

        return openSolflare(uri)
    }

    private fun openSolflare(uri: Uri): Boolean {
        val solflareIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage(solflarePackageName)
        }

        return try {
            context.startActivity(solflareIntent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun isConnectCallback(uri: Uri): Boolean {
        return uri.getQueryParameter("solflare_encryption_public_key") != null &&
            uri.getQueryParameter("data") != null &&
            uri.getQueryParameter("nonce") != null
    }

    fun isSignCallback(uri: Uri): Boolean {
        return uri.getQueryParameter("data") != null &&
            uri.getQueryParameter("nonce") != null &&
            uri.getQueryParameter("solflare_encryption_public_key") == null &&
            uri.getQueryParameter("phantom_encryption_public_key") == null
    }

    fun isErrorCallback(uri: Uri): Boolean {
        return uri.getQueryParameter("errorCode") != null ||
            uri.getQueryParameter("errorMessage") != null
    }

    fun parseErrorCallback(uri: Uri): String {
        val code = uri.getQueryParameter("errorCode").orEmpty()
        val message = uri.getQueryParameter("errorMessage").orEmpty()
        return listOf(code, message)
            .filter { it.isNotBlank() }
            .joinToString(": ")
            .ifBlank { "Solflare 지갑 인증이 취소되었거나 실패했습니다." }
    }

    fun savePendingLogin(walletAddress: String, nonce: String) {
        prefs.edit()
            .putString(KEY_PENDING_WALLET_ADDRESS, walletAddress)
            .putString(KEY_PENDING_NONCE, nonce)
            .apply()
    }

    fun getPendingWalletAddress(): String? {
        return prefs.getString(KEY_PENDING_WALLET_ADDRESS, null)
    }

    fun getPendingNonce(): String? {
        return prefs.getString(KEY_PENDING_NONCE, null)
    }

    fun clearPendingLogin() {
        prefs.edit()
            .remove(KEY_PENDING_WALLET_ADDRESS)
            .remove(KEY_PENDING_NONCE)
            .apply()
    }

    fun parseConnectCallback(uri: Uri): String? {
        return try {
            val solflarePublicKey = uri.getQueryParameter("solflare_encryption_public_key")
                ?.let { Base58.decode(it) }
                ?: return null
            solflareEncryptionPublicKey = solflarePublicKey
            prefs.edit()
                .putString(KEY_SOLFLARE_PUBLIC_KEY, Base58.encode(solflarePublicKey))
                .apply()

            val data = decryptCallback(uri, solflarePublicKey) ?: return null
            Log.d(tag, "connect callback decrypted=$data")
            val json = JSONObject(data)
            session = json.optString("session").takeIf { it.isNotBlank() }
            session?.let {
                prefs.edit().putString(KEY_SESSION, it).apply()
            }
            json.optString("public_key").takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            Log.e(tag, "connect callback parse failed", e)
            null
        }
    }

    fun parseSignCallback(uri: Uri): String? {
        return try {
            val currentSolflarePublicKey = solflareEncryptionPublicKey ?: return null
            val data = decryptCallback(uri, currentSolflarePublicKey) ?: return null
            Log.d(tag, "sign callback decrypted=$data")
            JSONObject(data).optString("signature").takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            Log.e(tag, "sign callback parse failed", e)
            null
        }
    }

    fun parseSignedTransactionCallback(uri: Uri): String? {
        return try {
            val currentSolflarePublicKey = solflareEncryptionPublicKey ?: return null
            val data = decryptCallback(uri, currentSolflarePublicKey) ?: return null
            Log.d(tag, "transaction callback decrypted=$data")
            JSONObject(data).optString("transaction").takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            Log.e(tag, "transaction callback parse failed", e)
            null
        }
    }

    private fun decryptCallback(uri: Uri, solflarePublicKey: ByteArray): String? {
        val nonce = uri.getQueryParameter("nonce")?.let { Base58.decode(it) } ?: return null
        val data = uri.getQueryParameter("data")?.let { Base58.decode(it) } ?: return null
        val decrypted = TweetNaclFast.Box(
            solflarePublicKey,
            dappKeyPair.secretKey
        ).open(data, nonce) ?: return null
        return String(decrypted, Charsets.UTF_8)
    }

    private fun newNonce(): ByteArray {
        return ByteArray(TweetNaclFast.Box.nonceLength).also { random.nextBytes(it) }
    }

    private fun loadDappKeyPair(): TweetNaclFast.Box.KeyPair? {
        val secretKey = prefs.getString(KEY_DAPP_SECRET_KEY, null)
            ?.let { Base58.decode(it) }
            ?: return null
        return TweetNaclFast.Box.keyPair_fromSecretKey(secretKey)
    }

    private companion object {
        const val KEY_DAPP_SECRET_KEY = "dapp_secret_key"
        const val KEY_DAPP_PUBLIC_KEY = "dapp_public_key"
        const val KEY_SOLFLARE_PUBLIC_KEY = "solflare_public_key"
        const val KEY_SESSION = "session"
        const val KEY_PENDING_WALLET_ADDRESS = "pending_wallet_address"
        const val KEY_PENDING_NONCE = "pending_nonce"
    }
}
