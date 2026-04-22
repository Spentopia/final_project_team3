// 이 파일이 속한 패키지 경로
package com.ict.spentopia.data.remote

// OkHttpClient 가져옴
// -> 실제로 네트워크 통신할 때 사용하는 클라이언트
import okhttp3.OkHttpClient

// HttpLoggingInterceptor 가져옴
// -> 요청/응답 내용을 로그로 확인할 때 씀
import okhttp3.logging.HttpLoggingInterceptor

// Retrofit 가져옴
// -> API 호출을 쉽게 만들어주는 라이브러리
import retrofit2.Retrofit

// GsonConverterFactory 가져옴
// -> JSON 데이터를 코틀린 객체로 바꿔줌
import retrofit2.converter.gson.GsonConverterFactory


// object
// -> 앱에서 하나만 만들어서 공용으로 쓰는 객체
// -> Retrofit 설정을 한 곳에 모아두기 좋음
object RetrofitClient {

    // private
    // -> 이 파일(object) 안에서만 사용 가능
    //
    // const val
    // -> 바뀌지 않는 상수값
    //
    // BASE_URL
    // -> 서버의 기본 주소
    // 예: "http://10.0.2.2:8080/" 또는 "https://api.example.com/"
    //
    // 중요:
    // baseUrl은 보통 마지막에 / 가 있어야 함
    private const val BASE_URL = "https://api.spentopia.com/"  // 서버 넣기

    // loggingInterceptor 변수 만듦
    // -> 네트워크 요청, 응답을 로그로 출력하는 역할
    private val loggingInterceptor = HttpLoggingInterceptor().apply {

        // 로그를 어디까지 볼지 정하는 부분
        //
        // Level.BODY
        // -> 헤더 + 바디까지 전부 출력
        // -> 개발할 때 확인하기 좋음
        // -> 배포할 때는 너무 자세해서 조심해야 함
        level = HttpLoggingInterceptor.Level.BODY
    }

    // OkHttpClient 생성
    // -> Retrofit이 내부적으로 이 클라이언트를 사용해서 서버와 통신함
    private val okHttpClient = OkHttpClient.Builder()

        // 아까 만든 loggingInterceptor 추가
        // -> 그래서 요청/응답 로그를 볼 수 있게 됨
        .addInterceptor(loggingInterceptor)

        // 최종적으로 클라이언트 완성
        .build()

    // walletApi 라는 변수 만듦
    //
    // val
    // -> 한 번 만들어지면 다시 바꾸지 않음
    //
    // by lazy
    // -> 처음 사용할 때만 생성함
    // -> 앱 시작하자마자 만드는 게 아니라
    //    walletApi가 실제 필요할 때 생성됨
    val walletApi: WalletApi by lazy {

        // Retrofit 객체 생성 시작
        Retrofit.Builder()

            // 기본 서버 주소 넣음
            // 예:
            // /auth/wallet/nonce
            // /wallet/link
            // 이런 상대 경로 앞에 BASE_URL이 붙음
            .baseUrl(BASE_URL)

            // OkHttpClient 연결
            // -> 로그 설정 등이 적용된 클라이언트 사용
            .client(okHttpClient)

            // JSON <-> 코틀린 객체 변환 기능 추가
            // 서버가 JSON 주면 자동으로 data class로 바꿔줌
            .addConverterFactory(GsonConverterFactory.create())

            // Retrofit 완성
            .build()

            // WalletApi 인터페이스를 실제 사용할 수 있는 객체로 만들어줌
            //
            // 즉,
            // interface에 적어둔
            // issueWalletNonce(), linkWallet(), unlinkWallet()
            // 같은 함수들을 실제 호출 가능하게 바꿔줌
            .create(WalletApi::class.java)
    }
}