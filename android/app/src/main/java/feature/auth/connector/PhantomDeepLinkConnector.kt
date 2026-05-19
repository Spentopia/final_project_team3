package com.ict.spentopia.feature.auth.connector // 이 파일이 속한 패키지 위치를 적음

import android.content.Context // 현재 화면 정보 타입을 가져옴
import android.content.Intent // Intent 기능을 가져옴
import android.net.Uri // 이미지 주소 타입을 가져옴
import android.util.Log // 로그 찍는 기능을 가져옴
import com.iwebpp.crypto.TweetNaclFast // TweetNaclFast 기능을 가져옴
import org.bitcoinj.core.Base58 // Base58 기능을 가져옴
import org.json.JSONObject // JSONObject 기능을 가져옴
import java.security.SecureRandom // SecureRandom 기능을 가져옴

class PhantomDeepLinkConnector( // PhantomDeepLinkConnector 기능을 묶어둔 클래스 시작
    private val context: Context // 현재 화면 정보를 저장함
) { // 이 블록 안의 내용이 시작됨

    private val tag = "PhantomDeepLink" // tag 값을 저장함
    private val redirectLink = "spentopia://wallet-callback" // redirectLink 값을 저장함
    private val phantomPackageName = "app.phantom" // phantomPackageName 값을 저장함
    private val prefs = context.getSharedPreferences("phantom_deeplink", Context.MODE_PRIVATE) // 토큰을 저장할 간단 저장소를 가져옴
    private val random = SecureRandom() // random 값을 저장함
    private var dappKeyPair = loadDappKeyPair() ?: TweetNaclFast.Box.keyPair() // 나중에 바뀔 수 있는 dappKeyPair 값을 저장함
    private var phantomEncryptionPublicKey: ByteArray? = prefs.getString(KEY_PHANTOM_PUBLIC_KEY, null) // 나중에 바뀔 수 있는 phantomEncryptionPublicKey 값을 저장함
        ?.let { Base58.decode(it) }
    private var session: String? = prefs.getString(KEY_SESSION, null) // 나중에 바뀔 수 있는 로그인 세션을 저장함

    fun connect(): Boolean { // connect 함수를 선언함
        dappKeyPair = TweetNaclFast.Box.keyPair() // 안쪽 UI를 한 영역에 겹쳐 배치함
        phantomEncryptionPublicKey = null // null 값을 phantomEncryptionPublicKey 값에 넣음
        session = null // null 값을 로그인 세션에 넣음
        prefs.edit()
            .putString(KEY_DAPP_SECRET_KEY, Base58.encode(dappKeyPair.secretKey))
            .putString(KEY_DAPP_PUBLIC_KEY, Base58.encode(dappKeyPair.publicKey))
            .remove(KEY_PHANTOM_PUBLIC_KEY)
            .remove(KEY_SESSION)
            .apply()

        val uri = Uri.parse("https://phantom.app/ul/v1/connect") // 이미지 주소를 저장함
            .buildUpon()
            .appendQueryParameter("app_url", "https://spentopia.net")
            .appendQueryParameter("cluster", "devnet")
            .appendQueryParameter(
                "dapp_encryption_public_key",
                Base58.encode(dappKeyPair.publicKey)
            )
            .appendQueryParameter("redirect_link", redirectLink)
            .build()

        return openPhantom(uri) // 이 값을 함수 결과로 돌려줌
    }

    fun signMessage(message: String): Boolean { // signMessage 함수를 선언함
        val currentSession = session ?: return false // currentSession 값을 저장함
        val currentPhantomPublicKey = phantomEncryptionPublicKey ?: return false // currentPhantomPublicKey 값을 저장함
        val payload = JSONObject() // payload 값을 저장함
            .put("message", Base58.encode(message.toByteArray(Charsets.UTF_8)))
            .put("session", currentSession)
            .put("display", "utf8")
            .toString()
        val nonce = newNonce() // 서명용 난수을 저장함
        val encryptedPayload = TweetNaclFast.Box( // encryptedPayload 값을 저장함
            currentPhantomPublicKey,
            dappKeyPair.secretKey
        ).box(payload.toByteArray(Charsets.UTF_8), nonce)

        val uri = Uri.parse("https://phantom.app/ul/v1/signMessage") // 이미지 주소를 저장함
            .buildUpon()
            .appendQueryParameter(
                "dapp_encryption_public_key",
                Base58.encode(dappKeyPair.publicKey)
            )
            .appendQueryParameter("nonce", Base58.encode(nonce))
            .appendQueryParameter("redirect_link", redirectLink)
            .appendQueryParameter("payload", Base58.encode(encryptedPayload))
            .build()

        return openPhantom(uri) // 이 값을 함수 결과로 돌려줌
    }

    private fun openPhantom(uri: Uri): Boolean { // openPhantom 함수를 선언함
        val phantomIntent = Intent(Intent.ACTION_VIEW, uri).apply { // phantomIntent 값을 저장함
            setPackage(phantomPackageName) // set Package 함수를 실행함
        }

        return try { // 이 값을 함수 결과로 돌려줌
            context.startActivity(phantomIntent)
            true
        } catch (_: Exception) { // 이 블록 안의 내용이 시작됨
            false
        }
    }

    fun isConnectCallback(uri: Uri): Boolean { // isConnectCallback 함수를 선언함
        return uri.getQueryParameter("phantom_encryption_public_key") != null && // 이 값을 함수 결과로 돌려줌
            uri.getQueryParameter("data") != null && // ! 값을 정해줌
            uri.getQueryParameter("nonce") != null // ! 값을 정해줌
    }

    fun isSignCallback(uri: Uri): Boolean { // isSignCallback 함수를 선언함
        return uri.getQueryParameter("data") != null && // 이 값을 함수 결과로 돌려줌
            uri.getQueryParameter("nonce") != null && // ! 값을 정해줌
            uri.getQueryParameter("phantom_encryption_public_key") == null // uri.getQueryParameter("phantom_encryption_public_key" 값을 정해줌
    }

    fun isErrorCallback(uri: Uri): Boolean { // isErrorCallback 함수를 선언함
        return uri.getQueryParameter("errorCode") != null || // 이 값을 함수 결과로 돌려줌
            uri.getQueryParameter("errorMessage") != null // ! 값을 정해줌
    }

    fun parseErrorCallback(uri: Uri): String { // parseErrorCallback 함수를 선언함
        val code = uri.getQueryParameter("errorCode").orEmpty() // 인증 코드를 저장함
        val message = uri.getQueryParameter("errorMessage").orEmpty() // 메시지를 저장함
        return listOf(code, message) // 이 값을 함수 결과로 돌려줌
            .filter { it.isNotBlank() }
            .joinToString(": ")
            .ifBlank { "Phantom 지갑 인증이 취소되었거나 실패했습니다." }
    }

    fun savePendingLogin(walletAddress: String, nonce: String) { // 로그인 기능을 실행하는 함수 시작
        prefs.edit()
            .putString(KEY_PENDING_WALLET_ADDRESS, walletAddress)
            .putString(KEY_PENDING_NONCE, nonce)
            .apply()
    }

    fun getPendingWalletAddress(): String? { // 데이터를 불러오는 함수 시작
        return prefs.getString(KEY_PENDING_WALLET_ADDRESS, null) // 이 값을 함수 결과로 돌려줌
    }

    fun getPendingNonce(): String? { // 데이터를 불러오는 함수 시작
        return prefs.getString(KEY_PENDING_NONCE, null) // 이 값을 함수 결과로 돌려줌
    }

    fun clearPendingLogin() { // 로그인 기능을 실행하는 함수 시작
        prefs.edit()
            .remove(KEY_PENDING_WALLET_ADDRESS)
            .remove(KEY_PENDING_NONCE)
            .apply()
    }

    fun parseConnectCallback(uri: Uri): String? { // parseConnectCallback 함수를 선언함
        return try { // 이 값을 함수 결과로 돌려줌
            val phantomPublicKey = uri.getQueryParameter("phantom_encryption_public_key") // phantomPublicKey 값을 저장함
                ?.let { Base58.decode(it) }
                ?: return null
            phantomEncryptionPublicKey = phantomPublicKey // phantomPublicKey 값을 phantomEncryptionPublicKey 값에 넣음
            prefs.edit()
                .putString(KEY_PHANTOM_PUBLIC_KEY, Base58.encode(phantomPublicKey))
                .apply()

            val data = decryptCallback(uri, phantomPublicKey) ?: return null // data 값을 저장함
            Log.d(tag, "connect callback decrypted=$data") // 개발자가 확인할 로그를 찍음
            val json = JSONObject(data) // json 값을 저장함
            session = json.optString("session").takeIf { it.isNotBlank() } // 로그인 세션을 정해줌
            session?.let { // 이 블록 안의 내용이 시작됨
                prefs.edit().putString(KEY_SESSION, it).apply()
            }
            json.optString("public_key").takeIf { it.isNotBlank() }
        } catch (e: Exception) { // 이 블록 안의 내용이 시작됨
            Log.e(tag, "connect callback parse failed", e) // 개발자가 확인할 로그를 찍음
            null
        }
    }

    fun parseSignCallback(uri: Uri): String? { // parseSignCallback 함수를 선언함
        return try { // 이 값을 함수 결과로 돌려줌
            val currentPhantomPublicKey = phantomEncryptionPublicKey ?: return null // currentPhantomPublicKey 값을 저장함
            val data = decryptCallback(uri, currentPhantomPublicKey) ?: return null // data 값을 저장함
            Log.d(tag, "sign callback decrypted=$data") // 개발자가 확인할 로그를 찍음
            JSONObject(data).optString("signature").takeIf { it.isNotBlank() } // JSONObject 함수를 실행함
        } catch (e: Exception) { // 이 블록 안의 내용이 시작됨
            Log.e(tag, "sign callback parse failed", e) // 개발자가 확인할 로그를 찍음
            null
        }
    }

    private fun decryptCallback(uri: Uri, phantomPublicKey: ByteArray): String? { // decryptCallback 함수를 선언함
        val nonce = uri.getQueryParameter("nonce")?.let { Base58.decode(it) } ?: return null // 서명용 난수을 저장함
        val data = uri.getQueryParameter("data")?.let { Base58.decode(it) } ?: return null // data 값을 저장함
        val decrypted = TweetNaclFast.Box( // decrypted 값을 저장함
            phantomPublicKey,
            dappKeyPair.secretKey
        ).open(data, nonce) ?: return null
        return String(decrypted, Charsets.UTF_8) // 이 값을 함수 결과로 돌려줌
    }

    private fun newNonce(): ByteArray { // newNonce 함수를 선언함
        return ByteArray(TweetNaclFast.Box.nonceLength).also { random.nextBytes(it) } // 이 값을 함수 결과로 돌려줌
    }

    private fun loadDappKeyPair(): TweetNaclFast.Box.KeyPair? { // 데이터를 불러오는 함수 시작
        val secretKey = prefs.getString(KEY_DAPP_SECRET_KEY, null) // secretKey 값을 저장함
            ?.let { Base58.decode(it) }
            ?: return null
        return TweetNaclFast.Box.keyPair_fromSecretKey(secretKey) // 이 값을 함수 결과로 돌려줌
    }

    private companion object { // 이 블록 안의 내용이 시작됨
        const val KEY_DAPP_SECRET_KEY = "dapp_secret_key" // KEY_DAPP_SECRET_KEY 값을 저장함
        const val KEY_DAPP_PUBLIC_KEY = "dapp_public_key" // KEY_DAPP_PUBLIC_KEY 값을 저장함
        const val KEY_PHANTOM_PUBLIC_KEY = "phantom_public_key" // KEY_PHANTOM_PUBLIC_KEY 값을 저장함
        const val KEY_SESSION = "session" // KEY_SESSION 값을 저장함
        const val KEY_PENDING_WALLET_ADDRESS = "pending_wallet_address" // 지갑 관련 값을 저장함
        const val KEY_PENDING_NONCE = "pending_nonce" // KEY_PENDING_NONCE 값을 저장함
    }
}
