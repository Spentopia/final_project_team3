package com.ict.spentopia // 이 파일이 속한 패키지 위치를 적음

import android.content.Intent // 앱으로 들어온 실행 정보를 가져옴
import android.content.pm.PackageManager // 앱 패키지 정보를 확인하는 도구를 가져옴
import android.net.Uri // 딥링크 주소를 담는 Uri 타입을 가져옴
import android.os.Build // 안드로이드 버전 정보를 가져옴
import android.os.Bundle // 화면이 다시 만들어질 때 넘겨받는 데이터 타입을 가져옴
import android.util.Log // 로그 찍는 기능을 가져옴
import androidx.activity.ComponentActivity // Compose 화면을 띄울 Activity 기본 클래스를 가져옴
import androidx.activity.compose.setContent // Compose UI를 화면에 붙이는 기능을 가져옴
import androidx.compose.runtime.getValue // by로 상태를 읽게 해줌
import androidx.compose.runtime.mutableStateOf // 화면 상태를 만드는 도구를 가져옴
import androidx.compose.runtime.remember // 값을 기억하는 Compose 도구를 가져옴
import androidx.compose.runtime.setValue // by로 상태를 바꾸게 해줌
import com.ict.spentopia.data.remote.RetrofitClient // 서버 통신 설정 객체를 가져옴
import com.ict.spentopia.navigation.AppNavGraph // 앱 화면 이동 구조를 가져옴
import com.ict.spentopia.ui.theme.SpentopiaTheme // 앱 전체 테마를 가져옴
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender // 솔라나 지갑 앱 호출 결과를 받는 도구를 가져옴
import java.security.MessageDigest // SHA1 해시를 만들 때 쓰는 도구를 가져옴

// 앱 진입점임
// package/SHA1 확인 후 네트워크/지갑 초기화하고 NavGraph 진입
class MainActivity : ComponentActivity() { // 앱이 처음 시작되는 메인 화면 클래스 시작

    companion object { // MainActivity 안에서 같이 쓰는 고정값을 모아둠
        private const val PREFS_NAME = "auth_prefs" // 로그인/테마 값을 저장할 저장소 이름
        private const val KEY_THEME_IS_DARK = "theme_is_dark" // 다크모드 여부를 저장할 키 이름
    }

    private lateinit var walletActivityResultSender: ActivityResultSender

    var walletCallbackUri by mutableStateOf<Uri?>(null) // 지갑 앱에서 돌아온 딥링크 주소를 저장함
        private set

    var kakaoCallbackUri by mutableStateOf<Uri?>(null) // 카카오 로그인 후 돌아온 딥링크 주소를 저장함
        private set

    override fun onCreate(savedInstanceState: Bundle?) { // 앱 화면이 처음 만들어질 때 실행되는 함수 시작
        if (BuildConfig.DEBUG) { // 조건이 맞는지 확인함
            Log.d("Spentopia", "APP_NEW_BUILD_RUNNING") // 새 빌드가 실행됐는지 로그로 확인함
        }
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
        logRuntimeSha1() // 현재 앱의 패키지명과 SHA1 값을 로그로 찍음

        // 네트워크 준비 먼저 함
        // RetrofitClient.init() 선행 필요함
        RetrofitClient.init(applicationContext) // 서버 통신을 앱 전체에서 쓸 수 있게 먼저 준비함
        walletActivityResultSender = ActivityResultSender(this) // 지갑 앱을 열고 결과를 받을 도구를 준비함

        handleCallbackIntent(intent) // 새로 들어온 딥링크 주소를 확인함

        // 테마 선택값 저장됨
        val themePrefs = applicationContext.getSharedPreferences(PREFS_NAME, MODE_PRIVATE) // 저장해둔 테마 값을 꺼낼 저장소를 가져옴

        setContent { // 여기부터 Compose 화면을 그림
            // 앱 재실행 후에도 유지되도록 SharedPreferences에서 읽은 초기값을 사용합니다.
            var isDarkTheme by remember { // 화면이 다시 그려져도 다크모드 선택값을 기억함
                mutableStateOf(themePrefs.getBoolean(KEY_THEME_IS_DARK, false)) // 저장된 다크모드 값을 화면 상태로 만듦
            }

            SpentopiaTheme( // 앱 전체에 Spentopia 테마를 적용함
                darkTheme = isDarkTheme // 현재 다크모드 값을 테마에 넘김
            ) { // 이 블록 안의 내용이 시작됨
                AppNavGraph( // 로그인, 홈, 마켓 같은 화면 이동 구조를 띄움
                    walletActivityResultSender = walletActivityResultSender, // 지갑 앱 호출 도구를 화면 이동 쪽으로 넘김
                    walletCallbackUri = walletCallbackUri, // 지갑 콜백 주소를 화면 이동 쪽으로 넘김
                    onWalletCallbackConsumed = { // 지갑 콜백 처리가 끝났을 때 실행함
                        walletCallbackUri = null // 처리 끝난 지갑 콜백 주소를 비움
                    },
                    kakaoCallbackUri = kakaoCallbackUri, // 카카오 콜백 주소를 화면 이동 쪽으로 넘김
                    onKakaoCallbackConsumed = { // 카카오 콜백 처리가 끝났을 때 실행함
                        kakaoCallbackUri = null // 처리 끝난 카카오 콜백 주소를 비움
                    },
                    isDarkTheme = isDarkTheme, // 현재 다크모드 상태를 넘김
                    onThemeChange = { newIsDarkTheme -> // 사용자가 테마를 바꿨을 때 실행함
                        isDarkTheme = newIsDarkTheme // 새 다크모드 값을 화면 상태에 저장함
                        themePrefs.edit()
                            .putBoolean(KEY_THEME_IS_DARK, newIsDarkTheme)
                            .apply()
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) { // 앱이 이미 켜진 상태에서 새 딥링크가 들어오면 실행됨
        super.onNewIntent(intent)
        setIntent(intent) // 새로 들어온 실행 정보를 Activity에 다시 넣음
        handleCallbackIntent(intent) // 새로 들어온 딥링크 주소를 확인함
    }

    private fun handleCallbackIntent(intent: Intent?) { // 지갑/카카오 딥링크인지 확인하는 함수
        val data = intent?.data ?: return // 딥링크 주소가 없으면 여기서 끝냄

        if (data.scheme == "spentopia" && data.host == "wallet-callback") { // 지갑 콜백 주소인지 확인함
            walletCallbackUri = data // 지갑 콜백 주소를 저장해서 화면 쪽에서 처리하게 함
        }

        if (data.scheme == "spentopia" && data.host == "kakao-callback") { // 카카오 콜백 주소인지 확인함
            kakaoCallbackUri = data // 카카오 콜백 주소를 저장해서 화면 쪽에서 처리하게 함
        }
    }

    // =====================================================
    // 현재 실행 중인 APK의 실제 packageName / SHA1 확인용
    // -----------------------------------------------------
    // Google Cloud Console의 Android OAuth Client에 등록된
    // Package name / SHA-1과 100% 일치해야 Google 로그인이 됩니다.
    // =====================================================
    private fun logRuntimeSha1() { // 구글 로그인 설정 확인용 SHA1 값을 찍는 함수
        try { // 오류가 날 수 있는 코드를 먼저 시도함
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { // 현재 앱 패키지 정보를 가져옴
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else { // 안드로이드 9 미만이면 예전 방식으로 가져옴
                @Suppress("DEPRECATION") // 예전 방식 경고를 여기서는 무시함
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNATURES
                )
            }

            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { // 앱 서명 정보를 가져옴
                packageInfo.signingInfo?.apkContentsSigners
            } else { // 안드로이드 9 미만이면 예전 방식으로 가져옴
                @Suppress("DEPRECATION") // 예전 방식 경고를 여기서는 무시함
                packageInfo.signatures
            }

            signatures?.forEach { signature ->
                val messageDigest = MessageDigest.getInstance("SHA1") // SHA1 값을 만들 도구를 준비함
                val sha1 = messageDigest.digest(signature.toByteArray()) // 앱 서명값을 SHA1 문자열로 바꿈
                    .joinToString(":") { "%02X".format(it) }

                if (BuildConfig.DEBUG) { // 디버그 빌드일 때만 로그를 찍음
                    Log.d("Spentopia", "RUNTIME packageName=$packageName") // 현재 앱 패키지명을 로그로 보여줌
                    Log.d("Spentopia", "RUNTIME SHA1=$sha1") // 현재 앱 SHA1 값을 로그로 보여줌
                }
            }
        } catch (e: Exception) { // SHA1 확인 중 오류가 나면 여기서 처리함
            Log.e("Spentopia", "RUNTIME SHA1 확인 실패", e) // SHA1 확인 실패 로그를 찍음
        }
    }
}
