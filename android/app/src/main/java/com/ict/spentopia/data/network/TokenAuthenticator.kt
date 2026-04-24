package com.ict.spentopia.data.network

import android.content.Context
import com.ict.spentopia.data.remote.RetrofitClient
import com.ict.spentopia.data.remote.RefreshTokenRequest
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import kotlinx.coroutines.runBlocking

class TokenAuthenticator(
    private val context: Context
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {

        val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

        val refreshToken = prefs.getString("refresh_token", null) ?: return null

        return try {
            val api = RetrofitClient.walletApi

            val refreshResponse = runBlocking {
                api.refreshToken(
                    clientType = "app",
                    request = RefreshTokenRequest(refreshToken)
                )
            }

            val newAccessToken = refreshResponse.access_token
            val newRefreshToken = refreshResponse.refresh_token

            prefs.edit()
                .putString("access_token", newAccessToken)
                .putString("refresh_token", newRefreshToken)
                .apply()

            response.request.newBuilder()
                .header("Authorization", "Bearer $newAccessToken")
                .build()

        } catch (e: Exception) {

            // refresh 실패 → 강제 로그아웃 상태로 변경
            prefs.edit()
                .clear()
                .putBoolean("force_logout", true)   // 👈신호
                .apply()

            null
        }
    }
}