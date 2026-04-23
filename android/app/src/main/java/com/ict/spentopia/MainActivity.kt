package com.ict.spentopia

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ict.spentopia.navigation.AppNavGraph
import com.ict.spentopia.ui.theme.SpentopiaTheme
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender

class MainActivity : ComponentActivity() {

    // 지갑 연결 결과를 받을 sender
    // onCreate 시점에 미리 생성해야 함
    private lateinit var walletActivityResultSender: ActivityResultSender

    // Phantom / Backpack / Solflare 딥링크 콜백 URI 저장
    var walletCallbackUri by mutableStateOf<Uri?>(null)
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 중요:
        // ActivityResultSender는 Activity가 RESUMED 된 뒤에 만들면
        // registerForActivityResult 타이밍 오류로 크래시가 남
        // 그래서 onCreate에서 미리 생성
        walletActivityResultSender = ActivityResultSender(this)

        // 앱이 딥링크로 실행된 경우 콜백 URI 저장
        handleWalletCallbackIntent(intent)

        setContent {
            SpentopiaTheme {
                AppNavGraph(
                    walletActivityResultSender = walletActivityResultSender,
                    walletCallbackUri = walletCallbackUri,
                    onWalletCallbackConsumed = {
                        walletCallbackUri = null
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        // 이미 실행 중인 앱으로 딥링크가 다시 들어오는 경우 처리
        handleWalletCallbackIntent(intent)
    }

    private fun handleWalletCallbackIntent(intent: Intent?) {
        val data = intent?.data ?: return

        // 우리 앱 콜백 스킴인지 확인
        if (data.scheme == "spentopia" && data.host == "wallet-callback") {
            walletCallbackUri = data
        }
    }
}