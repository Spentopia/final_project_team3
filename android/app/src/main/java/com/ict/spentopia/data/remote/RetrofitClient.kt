// 이 파일이 속한 패키지 경로
package com.ict.spentopia.data.remote

// Context 가져옴
// -> SharedPreferences에서 토큰을 읽기 위해 필요함
import android.content.Context

// BuildConfig에 등록한 API_BASE_URL을 사용합니다.
import com.ict.spentopia.BuildConfig

// AuthInterceptor 가져옴
// -> access_token을 Authorization 헤더에 자동으로 붙이는 역할
import com.ict.spentopia.data.network.AuthInterceptor

// TokenAuthenticator 가져옴
// -> access_token 만료로 401이 발생했을 때 refresh_token으로 새 토큰을 받는 역할
import com.ict.spentopia.data.network.TokenAuthenticator

// OkHttpClient 가져옴
// -> 실제로 네트워크 통신할 때 사용하는 클라이언트
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer

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
    private val BASE_URL = BuildConfig.API_BASE_URL.let { url ->
        if (url.endsWith("/")) url else "$url/"
    }

    private val AI_ANALYZE_BASE_URL = BuildConfig.AI_ANALYZE_BASE_URL.let { url ->
        if (url.endsWith("/")) url else "$url/"
    }

    // appContext 변수 만듦
    // -> AuthInterceptor에서 SharedPreferences를 읽기 위해 사용함
    // -> Activity Context를 오래 들고 있으면 위험하므로 applicationContext만 저장함
    private lateinit var appContext: Context

    // initialized 변수 만듦
    // -> RetrofitClient.init(context)가 호출됐는지 확인하기 위해 사용함
    private var initialized = false

    // init 함수
    // -> 앱 시작 시 또는 RetrofitClient를 쓰기 전에 한 번 호출해야 함
    // -> 예: RetrofitClient.init(applicationContext)
    fun init(context: Context) {
        appContext = context.applicationContext
        initialized = true
    }

    // loggingInterceptor 변수 만듦
    // -> 네트워크 요청, 응답을 로그로 출력하는 역할
    private val loggingInterceptor = Interceptor { chain ->
        val request = chain.request()
        if (!BuildConfig.DEBUG) {
            return@Interceptor chain.proceed(request)
        }

        val requestId = System.currentTimeMillis().toString(16)

        logRequest(requestId, request)

        val startNs = System.nanoTime()
        val response = try {
            chain.proceed(request)
        } catch (e: Exception) {
            Log.e("SpentopiaNet", "[$requestId] ${request.method} ${request.url} failed: ${e.message}")
            throw e
        }

        logResponse(requestId, response, startNs)
        response
    }

    // OkHttpClient 생성
    // -> Retrofit이 내부적으로 이 클라이언트를 사용해서 서버와 통신함
    private val okHttpClient: OkHttpClient by lazy {

        // init(context)가 먼저 호출되지 않으면 에러를 발생시킴
        // -> context 없이 토큰을 읽을 수 없기 때문
        check(initialized) {
            "RetrofitClient.init(context)를 먼저 호출해야 합니다."
        }

        OkHttpClient.Builder()

            // AuthInterceptor 추가
            // -> 저장된 access_token이 있으면 Authorization: Bearer 토큰 형식으로 자동 추가함
            .addInterceptor(AuthInterceptor(appContext))

            // TokenAuthenticator 추가
            // -> access_token이 만료되어 401이 발생하면
            // -> refresh_token으로 새 access_token을 요청하고 기존 요청을 다시 시도함
            .authenticator(TokenAuthenticator(appContext))

            // 아까 만든 loggingInterceptor 추가
            // -> 그래서 요청/응답 로그를 볼 수 있게 됨
            .addInterceptor(loggingInterceptor)

            // 최종적으로 클라이언트 완성
            .build()
    }

    private fun logRequest(requestId: String, request: okhttp3.Request) {
        val contentType = request.body?.contentType()?.toString()
        Log.d("SpentopiaNet", "[$requestId] --> ${request.method} ${request.url}")
        request.headers.names().forEach { name ->
            val rawValue = request.header(name).orEmpty()
            val value = if (isSensitiveHeader(name)) "***" else rawValue
            Log.d("SpentopiaNet", "[$requestId] $name: $value")
        }

        if (request.body == null) {
            Log.d("SpentopiaNet", "[$requestId] (no body)")
            return
        }

        if (!isLoggableTextContentType(contentType)) {
            Log.d("SpentopiaNet", "[$requestId] (body omitted: $contentType)")
            return
        }

        val buffer = Buffer()
        request.body?.writeTo(buffer)
        val body = maskSensitiveValues(buffer.readUtf8())
        Log.d("SpentopiaNet", "[$requestId] body=$body")
    }

    private fun logResponse(requestId: String, response: Response, startNs: Long) {
        val tookMs = (System.nanoTime() - startNs) / 1_000_000.0
        val contentType = response.body?.contentType()?.toString()
        val code = response.code
        val message = response.message

        Log.d("SpentopiaNet", "[$requestId] <-- $code $message (${String.format("%.1f", tookMs)}ms) ${response.request.url}")
        response.headers.names().forEach { name ->
            val rawValue = response.header(name).orEmpty()
            val value = if (isSensitiveHeader(name)) "***" else rawValue
            Log.d("SpentopiaNet", "[$requestId] $name: $value")
        }

        if (response.body == null) {
            Log.d("SpentopiaNet", "[$requestId] (no body)")
            return
        }

        if (!isLoggableTextContentType(contentType)) {
            Log.d("SpentopiaNet", "[$requestId] (body omitted: $contentType)")
            return
        }

        val maxBytes = 32_768L
        val peekBody = response.peekBody(maxBytes)
        val bodyText = maskSensitiveValues(peekBody.string())
        Log.d("SpentopiaNet", "[$requestId] body=$bodyText")
    }

    private fun isSensitiveHeader(name: String): Boolean {
        return when (name.lowercase()) {
            "authorization", "cookie", "set-cookie", "x-refresh-token", "x-access-token" -> true
            else -> false
        }
    }

    private fun isLoggableTextContentType(contentType: String?): Boolean {
        val value = contentType?.lowercase() ?: return false
        if (value.startsWith("image/") ||
            value.startsWith("audio/") ||
            value.startsWith("video/") ||
            value.startsWith("font/") ||
            value.startsWith("multipart/") ||
            value.contains("octet-stream") ||
            value.contains("zip") ||
            value.contains("gzip") ||
            value.contains("pdf")
        ) {
            return false
        }

        return value.startsWith("application/json") ||
            value.contains("+json") ||
            value.startsWith("text/") ||
            value.startsWith("application/x-www-form-urlencoded")
    }

    private fun maskSensitiveValues(text: String): String {
        return text
            .replace(Regex("(?i)(\"?(access_token|refresh_token|token)\"?\\s*:\\s*\")([^\"]+)(\")"), "$1***$4")
            .replace(Regex("(?i)(Authorization\\s*:\\s*Bearer\\s+)(\\S+)"), "$1***")
    }

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
    val authApi: AuthApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApi::class.java)
    }

    val receiptApi: ReceiptApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ReceiptApi::class.java)
    }

    val expenseApi: ExpenseApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ExpenseApi::class.java)
    }

    val chatApi: ChatApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ChatApi::class.java)
    }

    val communityApi: CommunityApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CommunityApi::class.java)
    }

    val reportApi: ReportApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ReportApi::class.java)
    }

    val budgetApi: BudgetApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BudgetApi::class.java)
    }

    val rewardApi: RewardApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RewardApi::class.java)
    }

    val aiAnalyzeApi: AiAnalyzeApi by lazy {
        Retrofit.Builder()
            .baseUrl(AI_ANALYZE_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AiAnalyzeApi::class.java)
    }
}
