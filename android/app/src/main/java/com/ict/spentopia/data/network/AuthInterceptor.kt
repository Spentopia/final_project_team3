package com.ict.spentopia.data.network // 이 파일이 속한 패키지 위치를 적음

import android.content.Context // 현재 화면 정보 타입을 가져옴
import okhttp3.Interceptor // Interceptor 기능을 가져옴
import okhttp3.Response // Response 기능을 가져옴

class AuthInterceptor( // AuthInterceptor 기능을 묶어둔 클래스 시작
    private val context: Context // 현재 화면 정보를 저장함
) : Interceptor { // 이 블록 안의 내용이 시작됨

    override fun intercept(chain: Interceptor.Chain): Response { // intercept 함수를 선언함
        // 요청 나가기 직전 access_token 읽음
        // Authorization 헤더 자동 추가
        val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE) // 토큰을 저장할 간단 저장소를 가져옴
        val token = prefs.getString("access_token", null) // 토큰을 저장함

        val request = if (!token.isNullOrBlank()) { // 서버 요청값을 저장함
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else { // 이 블록 안의 내용이 시작됨
            chain.request()
        }

        return chain.proceed(request) // 이 값을 함수 결과로 돌려줌
    }
}
