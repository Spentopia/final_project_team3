package com.ict.spentopia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ict.spentopia.navigation.AppNavGraph
import com.ict.spentopia.ui.theme.SpentopiaTheme
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender

class MainActivity : ComponentActivity() {

    // 지갑 연결 결과를 받을 sender
    // onCreate 시점에 미리 생성해야 함
    private lateinit var walletActivityResultSender: ActivityResultSender

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 중요:
        // ActivityResultSender는 Activity가 RESUMED 된 뒤에 만들면
        // registerForActivityResult 타이밍 오류로 크래시가 남
        // 그래서 onCreate에서 미리 생성
        walletActivityResultSender = ActivityResultSender(this)

        setContent {
            SpentopiaTheme {
                AppNavGraph(
                    walletActivityResultSender = walletActivityResultSender
                )
            }
        }
    }
}