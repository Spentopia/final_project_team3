package com.ict.spentopia.feature.auth.connector

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.iwebpp.crypto.TweetNaclFast
import org.bitcoinj.core.Base58
import org.json.JSONObject
import java.security.SecureRandom

class PhantomDeepLinkConnector(
    private val context: Context
) {

    private val tag = "PhantomDeepLink"
    private val redirectLink = "spentopia://wallet-callback"
    private val phantomPackageName = "app.phantom"
    private val prefs = context.getSharedPreferences("phantom_deeplink", Context.MODE_PRIVATE)
    private val random = SecureRandom()
    private var dappKeyPair = loadDappKeyPair() ?: TweetNaclFast.Box.keyPair()
    private var phantomEncryptionPublicKey: ByteArray? = prefs.getString(KEY_PHANTOM_PUBLIC_KEY, null)
        ?.let { Base58.decode(it) }
    private var session: String? = prefs.getString(KEY_SESSION, null)

    fun connect(): Boolean {
        dappKeyPair = TweetNaclFast.Box.keyPair()
        phantomEncryptionPublicKey = null
        session = null
        prefs.edit()
            .putString(KEY_DAPP_SECRET_KEY, Base58.encode(dappKeyPair.secretKey))
            .putString(KEY_DAPP_PUBLIC_KEY, Base58.encode(dappKeyPair.publicKey))
            .remove(KEY_PHANTOM_PUBLIC_KEY)
            .remove(KEY_SESSION)
            .apply()

        val uri = Uri.parse("https://phantom.app/ul/v1/connect")
            .buildUpon()
            .appendQueryParameter("app_url", "https://spentopia.com")
            .appendQueryParameter("cluster", "devnet")
            .appendQueryParameter(
                "dapp_encryption_public_key",
                Base58.encode(dappKeyPair.publicKey)
            )
            .appendQueryParameter("redirect_link", redirectLink)
            .build()

        return openPhantom(uri)
    }

    fun signMessage(message: String): Boolean {
        val currentSession = session ?: return false
        val currentPhantomPublicKey = phantomEncryptionPublicKey ?: return false
        val payload = JSONObject()
            .put("message", Base58.encode(message.toByteArray(Charsets.UTF_8)))
            .put("session", currentSession)
            .put("display", "utf8")
            .toString()
        val nonce = newNonce()
        val encryptedPayload = TweetNaclFast.Box(
            currentPhantomPublicKey,
            dappKeyPair.secretKey
        ).box(payload.toByteArray(Charsets.UTF_8), nonce)

        val uri = Uri.parse("https://phantom.app/ul/v1/signMessage")
            .buildUpon()
            .appendQueryParameter(
                "dapp_encryption_public_key",
                Base58.encode(dappKeyPair.publicKey)
            )
            .appendQueryParameter("nonce", Base58.encode(nonce))
            .appendQueryParameter("redirect_link", redirectLink)
            .appendQueryParameter("payload", Base58.encode(encryptedPayload))
            .build()

        return openPhantom(uri)
    }

    private fun openPhantom(uri: Uri): Boolean {
        val phantomIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage(phantomPackageName)
        }

        return try {
            context.startActivity(phantomIntent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun isConnectCallback(uri: Uri): Boolean {
        return uri.getQueryParameter("phantom_encryption_public_key") != null &&
            uri.getQueryParameter("data") != null &&
            uri.getQueryParameter("nonce") != null
    }

    fun isSignCallback(uri: Uri): Boolean {
        return uri.getQueryParameter("data") != null &&
            uri.getQueryParameter("nonce") != null &&
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
            .ifBlank { "Phantom 지갑 인증이 취소되었거나 실패했습니다." }
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
            val phantomPublicKey = uri.getQueryParameter("phantom_encryption_public_key")
                ?.let { Base58.decode(it) }
                ?: return null
            phantomEncryptionPublicKey = phantomPublicKey
            prefs.edit()
                .putString(KEY_PHANTOM_PUBLIC_KEY, Base58.encode(phantomPublicKey))
                .apply() // 팬텀 키 연결

            val data = decryptCallback(uri, phantomPublicKey) ?: return null
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
            val currentPhantomPublicKey = phantomEncryptionPublicKey ?: return null
            val data = decryptCallback(uri, currentPhantomPublicKey) ?: return null
            Log.d(tag, "sign callback decrypted=$data")
            JSONObject(data).optString("signature").takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            Log.e(tag, "sign callback parse failed", e)
            null
        }
    }

    private fun decryptCallback(uri: Uri, phantomPublicKey: ByteArray): String? {
        val nonce = uri.getQueryParameter("nonce")?.let { Base58.decode(it) } ?: return null
        val data = uri.getQueryParameter("data")?.let { Base58.decode(it) } ?: return null
        val decrypted = TweetNaclFast.Box(
            phantomPublicKey,
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
        const val KEY_PHANTOM_PUBLIC_KEY = "phantom_public_key"
        const val KEY_SESSION = "session"
        const val KEY_PENDING_WALLET_ADDRESS = "pending_wallet_address"
        const val KEY_PENDING_NONCE = "pending_nonce"
    }
}
