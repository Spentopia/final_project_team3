package com.ict.spentopia

import android.content.Intent
import android.net.Uri
import android.os.Bundle
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

class MainActivity : ComponentActivity() {

    private lateinit var walletActivityResultSender: ActivityResultSender

    var walletCallbackUri by mutableStateOf<Uri?>(null)
        private set

    var kakaoCallbackUri by mutableStateOf<Uri?>(null)
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
}
