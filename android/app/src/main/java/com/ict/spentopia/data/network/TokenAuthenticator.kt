package com.ict.spentopia.data.network // 이 파일이 속한 패키지 위치를 적음

import android.content.Context // 현재 화면 정보 타입을 가져옴
import androidx.core.content.edit // edit 기능을 가져옴
import com.ict.spentopia.data.remote.RetrofitClient // RetrofitClient 기능을 가져옴
import com.ict.spentopia.data.remote.RefreshTokenRequest // RefreshTokenRequest 기능을 가져옴
import okhttp3.Authenticator // Authenticator 기능을 가져옴
import okhttp3.Request // Request 기능을 가져옴
import okhttp3.Response // Response 기능을 가져옴
import okhttp3.Route // Route 기능을 가져옴
import kotlinx.coroutines.runBlocking // runBlocking 기능을 가져옴

class TokenAuthenticator( // TokenAuthenticator 기능을 묶어둔 클래스 시작
    private val context: Context // 현재 화면 정보를 저장함
) : Authenticator { // 이 블록 안의 내용이 시작됨

    override fun authenticate(route: Route?, response: Response): Request? { // authenticate 함수를 선언함
        // 401이면 access_token 만료 가능성 있음
        // refresh_token으로 재발급 시도함
        val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE) // 토큰을 저장할 간단 저장소를 가져옴

        val refreshToken = prefs.getString("refresh_token", null) ?: return null // 갱신 토큰을 저장함

        return try { // 이 값을 함수 결과로 돌려줌
            val api = RetrofitClient.walletApi // 서버 API을 저장함

            val refreshResponse = runBlocking { // refreshResponse 값을 저장함
                api.refreshToken(
                    clientType = "app", // clientType 값을 정해줌
                    request = RefreshTokenRequest(refreshToken) // 서버 요청값을 정해줌
                )
            }

            val newAccessToken = refreshResponse.access_token // 토큰 값을 저장함
            val newRefreshToken = refreshResponse.refresh_token // 토큰 값을 저장함

            prefs.edit()
                .putString("access_token", newAccessToken)
                .putString("refresh_token", newRefreshToken)
                .apply()

            response.request.newBuilder()
                .header("Authorization", "Bearer $newAccessToken")
                .build()

        } catch (e: Exception) { // 이 블록 안의 내용이 시작됨

            // refresh 실패 -> 강제 로그아웃 플래그
            prefs.edit()
                .clear()
                .putBoolean("force_logout", true)
                .apply()

            null
        }
    }
}
