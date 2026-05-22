package com.ict.spentopia.data.remote // 이 파일이 속한 패키지 위치를 적음

import retrofit2.http.Body // 서버로 보낼 값을 표시하는 도구를 가져옴
import retrofit2.http.GET // GET API 표시를 가져옴
import retrofit2.http.Header // Header API 표시를 가져옴
import retrofit2.http.POST // POST API 표시를 가져옴

// 백엔드 POST /api/reports 요청 body입니다.
// 백엔드 Rust DTO의 GenerateReportRequest와 필드명을 맞췄습니다.
data class GenerateReportRequest( // GenerateReportRequest 데이터를 묶어둘 클래스 시작
    val analysis_kind: String, // report 또는 pattern 값을 백엔드에 보냄
    val report_type: String, // report_type 값을 저장함
    val start_date: String, // start_date 값을 저장함
    val end_date: String, // end_date 값을 저장함
    val transactions: List<AnalyzeTransactionRequest>, // 분석에 사용할 소비 목록을 보냄
    val total_expense: Double, // 기간 총 지출 금액을 보냄
    val budget: Double, // 현재 예산 값을 보냄
    val top_category: String, // 가장 많이 쓴 카테고리를 보냄
    val top_category_percent: Double, // 가장 많이 쓴 카테고리 비율을 보냄
    val daily_average: Double, // 하루 평균 지출을 보냄
    val expense_change_rate: Double, // 지출 변화율을 보냄
    val budget_usage: Double, // 예산 사용률을 보냄
    val category_data: List<AnalyzeCategoryDataRequest> // 카테고리별 소비 데이터를 보냄
)

// 백엔드 /api/reports 응답입니다.
// Android 분석 화면에서는 우선 ai_analysis 문장을 표시합니다.
data class ReportResponse( // ReportResponse 데이터를 묶어둘 클래스 시작
    val id: String, // 아이디를 저장함
    val report_type: String, // report_type 값을 저장함
    val start_date: String, // start_date 값을 저장함
    val end_date: String, // end_date 값을 저장함
    val category_summary: Any?, // category_summary 값을 저장함
    val daily_summary: Any?, // daily_summary 값을 저장함
    val ai_analysis: String?, // ai_analysis 값을 저장함
    val created_at: String? // created_at 값을 저장함
)

data class Solana402Body( // Solana X402 결제 필요 응답을 저장함
    val x402Version: Int, // X402 버전을 저장함
    val error: String, // 오류 메시지를 저장함
    val accepts: List<SolanaPaymentRequirement> // 결제 요구 조건 목록을 저장함
)

data class SolanaPaymentRequirement( // Solana 결제 요구 조건을 저장함
    val scheme: String, // 결제 방식 값을 저장함
    val network: String, // solana-devnet 또는 mainnet-beta 값을 저장함
    val maxAmountRequired: String, // 필요한 결제 금액을 micro 단위 문자열로 저장함
    val resource: String, // 결제 대상 리소스를 저장함
    val description: String, // 결제 설명을 저장함
    val mimeType: String, // 응답 타입을 저장함
    val payTo: String, // 결제 받을 지갑 주소를 저장함
    val maxTimeoutSeconds: Int, // 결제 유효 시간을 저장함
    val asset: String, // USDC mint 주소를 저장함
    val extra: Map<String, Any>? = null // paymentMemo 등 추가 정보를 저장함
)

interface ReportApi { // ReportApi에서 꼭 만들어야 할 함수 규칙을 정함
    // 로그인 보호 API입니다.
    // accessToken은 AuthInterceptor가 SharedPreferences에서 읽어 자동으로 붙입니다.
    @POST("/api/reports") // 서버에 데이터를 보내는 API 주소를 적음
    suspend fun generateReport( // generateReport 함수를 선언함
        @Body request: GenerateReportRequest, // 이 값을 서버 요청 본문에 넣는다는 표시
        @Header("X-PAYMENT") xPayment: String? = null // 결제 후 재호출할 때 X-PAYMENT 헤더를 보냄
    ): AnalyzeReportResponse

    @GET("/api/reports") // 서버에서 데이터를 가져오는 API 주소를 적음
    suspend fun getReports(): List<ReportResponse> // 데이터를 불러오는 함수 시작
}
