package com.ict.spentopia

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.ict.spentopia.data.remote.RetrofitClient
import com.ict.spentopia.data.remote.SupabaseClient
import com.ict.spentopia.navigation.AppNavGraph
import com.ict.spentopia.ui.theme.SpentopiaTheme
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import java.security.MessageDigest

class MainActivity : ComponentActivity() {

    private lateinit var walletActivityResultSender: ActivityResultSender

    var walletCallbackUri by mutableStateOf<Uri?>(null)
        private set

    var kakaoCallbackUri by mutableStateOf<Uri?>(null)
    private set

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.e("Spentopia", "APP_NEW_BUILD_RUNNING")
        super.onCreate(savedInstanceState)

        // =====================================================
        // Google 로그인 Error 10 원인 확인용 로그
        // -----------------------------------------------------
        // Logcat에서 아래 값이 Google Cloud Android OAuth Client와
        // 완전히 같은지 확인해야 합니다.
        //
        // RUNTIME packageName=com.ict.spentopia
        // RUNTIME SHA1=D7:9D:AE:10:67:9B:77:5A:A9:44:C9:51:F4:20:7B:16:2A:72:6A:11
        // =====================================================
        logRuntimeSha1()

        // 네트워크 요청 준비를 위해 가장 먼저 실행
        RetrofitClient.init(applicationContext)
        SupabaseClient.client
        walletActivityResultSender = ActivityResultSender(this)

        handleCallbackIntent(intent)

        setContent {
            // 앱이 실행 중일 때 다크모드/라이트모드 선택 상태를 기억합니다.
            var isDarkTheme by rememberSaveable {
                mutableStateOf(false)
            }

            SpentopiaTheme(
                darkTheme = isDarkTheme
            ) {
                AppNavGraph(
                    walletActivityResultSender = walletActivityResultSender,
                    walletCallbackUri = walletCallbackUri,
                    onWalletCallbackConsumed = {
                        walletCallbackUri = null
                    },
                    kakaoCallbackUri = kakaoCallbackUri,
                    onKakaoCallbackConsumed = {
                        kakaoCallbackUri = null
                    },
                    isDarkTheme = isDarkTheme,
                    onThemeChange = { newIsDarkTheme ->
                        isDarkTheme = newIsDarkTheme
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleCallbackIntent(intent)
    }

    private fun handleCallbackIntent(intent: Intent?) {
        val data = intent?.data ?: return

        if (data.scheme == "spentopia" && data.host == "wallet-callback") {
            walletCallbackUri = data
        }

        if (data.scheme == "spentopia" && data.host == "kakao-callback") {
            kakaoCallbackUri = data
        }
    }

    // =====================================================
    // 현재 실행 중인 APK의 실제 packageName / SHA1 확인용
    // -----------------------------------------------------
    // Google Cloud Console의 Android OAuth Client에 등록된
    // Package name / SHA-1과 100% 일치해야 Google 로그인이 됩니다.
    // =====================================================
    private fun logRuntimeSha1() {
        try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNATURES
                )
            }

            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            signatures?.forEach { signature ->
                val messageDigest = MessageDigest.getInstance("SHA1")
                val sha1 = messageDigest.digest(signature.toByteArray())
                    .joinToString(":") { "%02X".format(it) }

                Log.e("Spentopia", "RUNTIME packageName=$packageName")
                Log.e("Spentopia", "RUNTIME SHA1=$sha1")
            }
        } catch (e: Exception) {
            Log.e("Spentopia", "RUNTIME SHA1 확인 실패", e)
        }
    }
}
