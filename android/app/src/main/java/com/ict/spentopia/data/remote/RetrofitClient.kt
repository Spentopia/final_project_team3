// 이 파일이 속한 패키지 경로
package com.ict.spentopia.data.remote // 이 파일이 속한 패키지 위치를 적음

// Context 가져옴
// -> SharedPreferences에서 토큰을 읽기 위해 필요함
import android.content.Context // 현재 화면 정보 타입을 가져옴

// BuildConfig에 등록한 API_BASE_URL을 사용합니다.
import com.ict.spentopia.BuildConfig // BuildConfig 기능을 가져옴

// AuthInterceptor 가져옴
// -> access_token을 Authorization 헤더에 자동으로 붙이는 역할
import com.ict.spentopia.data.network.AuthInterceptor // AuthInterceptor 기능을 가져옴

// TokenAuthenticator 가져옴
// -> access_token 만료로 401이 발생했을 때 refresh_token으로 새 토큰을 받는 역할
import com.ict.spentopia.data.network.TokenAuthenticator // TokenAuthenticator 기능을 가져옴

// OkHttpClient 가져옴
// -> 실제로 네트워크 통신할 때 사용하는 클라이언트
import android.util.Log // 로그 찍는 기능을 가져옴
import okhttp3.OkHttpClient // 네트워크 요청 도구를 가져옴
import okhttp3.Interceptor // Interceptor 기능을 가져옴
import okhttp3.Response // Response 기능을 가져옴
import okio.Buffer // Buffer 기능을 가져옴

// Retrofit 가져옴
// -> API 호출을 쉽게 만들어주는 라이브러리
import retrofit2.Retrofit // 서버 통신 설정 도구를 가져옴

// GsonConverterFactory 가져옴
// -> JSON 데이터를 코틀린 객체로 바꿔줌
import retrofit2.converter.gson.GsonConverterFactory // GsonConverterFactory 기능을 가져옴


// object
// -> 앱에서 하나만 만들어서 공용으로 쓰는 객체
// -> Retrofit 설정을 한 곳에 모아두기 좋음
object RetrofitClient { // RetrofitClient를 앱에서 하나만 쓰게 만듦

    // private
    // -> 이 파일(object) 안에서만 사용 가능
    //
    // const val
    // -> 바뀌지 않는 상수값
    //
    // BASE_URLvmfhj
    // -> 서버의 기본 주소
    // 예: "http://10.0.2.2:8080/" 또는 "https://api.example.com/"
    //
    // 중요:
    // baseUrl은 보통 마지막에 / 가 있어야 함
    private val BASE_URL = BuildConfig.API_BASE_URL.let { url -> // BASE_URL 값을 저장함
        if (url.endsWith("/")) url else "$url/" // 조건이 맞는지 확인함
    }

    private val AI_ANALYZE_BASE_URL = BuildConfig.AI_ANALYZE_BASE_URL.let { url -> // AI_ANALYZE_BASE_URL 값을 저장함
        if (url.endsWith("/")) url else "$url/" // 조건이 맞는지 확인함
    }

    // appContext 변수 만듦
    // -> AuthInterceptor에서 SharedPreferences를 읽기 위해 사용함
    // -> Activity Context를 오래 들고 있으면 위험하므로 applicationContext만 저장함
    private lateinit var appContext: Context

    // initialized 변수 만듦
    // -> RetrofitClient.init(context)가 호출됐는지 확인하기 위해 사용함
    private var initialized = false // 나중에 바뀔 수 있는 initialized 값을 저장함

    // init 함수
    // -> 앱 시작 시 또는 RetrofitClient를 쓰기 전에 한 번 호출해야 함
    // -> 예: RetrofitClient.init(applicationContext)
    fun init(context: Context) { // init 함수를 선언함
        appContext = context.applicationContext // appContext 값을 정해줌
        initialized = true // true 값을 initialized 값에 넣음
    }

    // loggingInterceptor 변수 만듦
    // -> 네트워크 요청, 응답을 로그로 출력하는 역할
    private val loggingInterceptor = Interceptor { chain -> // loggingInterceptor 값을 저장함
        val request = chain.request() // 서버 요청값을 저장함
        if (!BuildConfig.DEBUG) { // 조건이 맞는지 확인함
            return@Interceptor chain.proceed(request)
        }

        val requestId = System.currentTimeMillis().toString(16) // requestId 값을 저장함

        logRequest(requestId, request) // log Request 함수를 실행함

        val startNs = System.nanoTime() // startNs 값을 저장함
        val response = try { // 서버 응답을 저장함
            chain.proceed(request)
        } catch (e: Exception) { // 이 블록 안의 내용이 시작됨
            Log.e("SpentopiaNet", "[$requestId] ${request.method} ${request.url} failed: ${e.message}") // 개발자가 확인할 로그를 찍음
            throw e
        }

        logResponse(requestId, response, startNs) // log Response 함수를 실행함
        response
    }

    // OkHttpClient 생성
    // -> Retrofit이 내부적으로 이 클라이언트를 사용해서 서버와 통신함
    private val okHttpClient: OkHttpClient by lazy { // okHttpClient 값을 저장함

        // init(context)가 먼저 호출되지 않으면 에러를 발생시킴
        // -> context 없이 토큰을 읽을 수 없기 때문
        check(initialized) { // 이 블록 안의 내용이 시작됨
            "RetrofitClient.init(context)를 먼저 호출해야 합니다." // 서버 통신 도구를 설정함
        }

        OkHttpClient.Builder() // 서버 통신 도구를 설정함

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

    private fun logRequest(requestId: String, request: okhttp3.Request) { // logRequest 함수를 선언함
        val contentType = request.body?.contentType()?.toString() // contentType 값을 저장함
        Log.d("SpentopiaNet", "[$requestId] --> ${request.method} ${request.url}") // 개발자가 확인할 로그를 찍음
        request.headers.names().forEach { name ->
            val rawValue = request.header(name).orEmpty() // rawValue 값을 저장함
            val value = if (isSensitiveHeader(name)) "***" else rawValue // 입력값을 저장함
            Log.d("SpentopiaNet", "[$requestId] $name: $value") // 개발자가 확인할 로그를 찍음
        }

        if (request.body == null) { // 조건이 맞는지 확인함
            Log.d("SpentopiaNet", "[$requestId] (no body)") // 개발자가 확인할 로그를 찍음
            return
        }

        if (!isLoggableTextContentType(contentType)) { // 조건이 맞는지 확인함
            Log.d("SpentopiaNet", "[$requestId] (body omitted: $contentType)") // 개발자가 확인할 로그를 찍음
            return
        }

        val buffer = Buffer() // buffer 값을 저장함
        request.body?.writeTo(buffer)
        val body = maskSensitiveValues(buffer.readUtf8()) // 본문을 저장함
        Log.d("SpentopiaNet", "[$requestId] body=$body") // 개발자가 확인할 로그를 찍음
    }

    private fun logResponse(requestId: String, response: Response, startNs: Long) { // logResponse 함수를 선언함
        val tookMs = (System.nanoTime() - startNs) / 1_000_000.0 // tookMs 값을 저장함
        val contentType = response.body?.contentType()?.toString() // contentType 값을 저장함
        val code = response.code // 인증 코드를 저장함
        val message = response.message // 메시지를 저장함

        Log.d("SpentopiaNet", "[$requestId] <-- $code $message (${String.format("%.1f", tookMs)}ms) ${response.request.url}") // 개발자가 확인할 로그를 찍음
        response.headers.names().forEach { name ->
            val rawValue = response.header(name).orEmpty() // rawValue 값을 저장함
            val value = if (isSensitiveHeader(name)) "***" else rawValue // 입력값을 저장함
            Log.d("SpentopiaNet", "[$requestId] $name: $value") // 개발자가 확인할 로그를 찍음
        }

        if (response.body == null) { // 조건이 맞는지 확인함
            Log.d("SpentopiaNet", "[$requestId] (no body)") // 개발자가 확인할 로그를 찍음
            return
        }

        if (!isLoggableTextContentType(contentType)) { // 조건이 맞는지 확인함
            Log.d("SpentopiaNet", "[$requestId] (body omitted: $contentType)") // 개발자가 확인할 로그를 찍음
            return
        }

        val maxBytes = 32_768L // maxBytes 값을 저장함
        val peekBody = response.peekBody(maxBytes) // peekBody 값을 저장함
        val bodyText = maskSensitiveValues(peekBody.string()) // bodyText 값을 저장함
        Log.d("SpentopiaNet", "[$requestId] body=$bodyText") // 개발자가 확인할 로그를 찍음
    }

    private fun isSensitiveHeader(name: String): Boolean { // isSensitiveHeader 함수를 선언함
        return when (name.lowercase()) { // 이 값을 함수 결과로 돌려줌
            "authorization", "cookie", "set-cookie", "x-refresh-token", "x-access-token" -> true
            else -> false // 위 조건이 아니면 이쪽을 실행함
        }
    }

    private fun isLoggableTextContentType(contentType: String?): Boolean { // isLoggableTextContentType 함수를 선언함
        val value = contentType?.lowercase() ?: return false // 입력값을 저장함
        if (value.startsWith("image/") || // 조건이 맞는지 확인함
            value.startsWith("audio/") ||
            value.startsWith("video/") ||
            value.startsWith("font/") ||
            value.startsWith("multipart/") ||
            value.contains("octet-stream") ||
            value.contains("zip") ||
            value.contains("gzip") ||
            value.contains("pdf")
        ) { // 이 블록 안의 내용이 시작됨
            return false // 이 값을 함수 결과로 돌려줌
        }

        return value.startsWith("application/json") || // 이 값을 함수 결과로 돌려줌
            value.contains("+json") ||
            value.startsWith("text/") ||
            value.startsWith("application/x-www-form-urlencoded")
    }

    private fun maskSensitiveValues(text: String): String { // maskSensitiveValues 함수를 선언함
        return text // 이 값을 함수 결과로 돌려줌
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
    val walletApi: WalletApi by lazy { // 지갑 관련 값을 저장함

        // Retrofit 객체 생성 시작
        Retrofit.Builder() // 서버 통신 도구를 설정함

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
    val authApi: AuthApi by lazy { // authApi 값을 저장함
        Retrofit.Builder() // 서버 통신 도구를 설정함
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApi::class.java)
    }

    val receiptApi: ReceiptApi by lazy { // receiptApi 값을 저장함
        Retrofit.Builder() // 서버 통신 도구를 설정함
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ReceiptApi::class.java)
    }

    val expenseApi: ExpenseApi by lazy { // 소비 내역 값을 저장함
        Retrofit.Builder() // 서버 통신 도구를 설정함
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ExpenseApi::class.java)
    }

    val chatApi: ChatApi by lazy { // 채팅 관련 값을 저장함
        Retrofit.Builder() // 서버 통신 도구를 설정함
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ChatApi::class.java)
    }

    val communityApi: CommunityApi by lazy { // 커뮤니티 관련 값을 저장함
        Retrofit.Builder() // 서버 통신 도구를 설정함
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CommunityApi::class.java)
    }

    val reportApi: ReportApi by lazy { // reportApi 값을 저장함
        Retrofit.Builder() // 서버 통신 도구를 설정함
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ReportApi::class.java)
    }

    val budgetApi: BudgetApi by lazy { // 예산 관련 값을 저장함
        Retrofit.Builder() // 서버 통신 도구를 설정함
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BudgetApi::class.java)
    }

    val rewardApi: RewardApi by lazy { // rewardApi 값을 저장함
        Retrofit.Builder() // 서버 통신 도구를 설정함
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RewardApi::class.java)
    }

    val notificationApi: NotificationApi by lazy { // notificationApi 값을 저장함
        Retrofit.Builder() // 서버 통신 도구를 설정함
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NotificationApi::class.java)
    }

    val userSettingsApi: UserSettingsApi by lazy { // userSettingsApi 값을 저장함
        Retrofit.Builder() // 서버 통신 도구를 설정함
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(UserSettingsApi::class.java)
    }

    val avatarApi: AvatarApi by lazy { // avatarApi 값을 저장함
        Retrofit.Builder() // 서버 통신 도구를 설정함
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AvatarApi::class.java)
    }

    val aiAnalyzeApi: AiAnalyzeApi by lazy { // aiAnalyzeApi 값을 저장함
        Retrofit.Builder() // 서버 통신 도구를 설정함
            .baseUrl(AI_ANALYZE_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AiAnalyzeApi::class.java)
    }
}
