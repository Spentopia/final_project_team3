package com.ict.spentopia.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

// 백엔드 POST /api/reports 요청 body입니다.
// 백엔드 Rust DTO의 GenerateReportRequest와 필드명을 맞췄습니다.
data class GenerateReportRequest(
    val report_type: String,
    val start_date: String,
    val end_date: String
)

// 백엔드 /api/reports 응답입니다.
// Android 분석 화면에서는 우선 ai_analysis 문장을 표시합니다.
data class ReportResponse(
    val id: String,
    val report_type: String,
    val start_date: String,
    val end_date: String,
    val category_summary: Any?,
    val daily_summary: Any?,
    val ai_analysis: String?,
    val created_at: String?
)

interface ReportApi {
    // 로그인 보호 API입니다.
    // accessToken은 AuthInterceptor가 SharedPreferences에서 읽어 자동으로 붙입니다.
    @POST("/api/reports")
    suspend fun generateReport(
        @Body request: GenerateReportRequest
    ): ReportResponse

    @GET("/api/reports")
    suspend fun getReports(): List<ReportResponse>
}
