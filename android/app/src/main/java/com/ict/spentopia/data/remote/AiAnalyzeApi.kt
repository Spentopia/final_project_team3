package com.ict.spentopia.data.remote // 이 파일이 속한 패키지 위치를 적음

import com.google.gson.annotations.SerializedName // JSON 필드 이름을 서버 계약에 맞추는 도구를 가져옴
import retrofit2.http.Body // 서버로 보낼 값을 표시하는 도구를 가져옴
import retrofit2.http.POST // POST API 표시를 가져옴

data class AnalyzeTransactionRequest( // AnalyzeTransactionRequest 데이터를 묶어둘 클래스 시작
    val date: String, // 날짜을 저장함
    val amount: Int, // 금액을 저장함
    val category: String, // 카테고리을 저장함
    val type: String // type 값을 저장함
)

data class AnalyzeWeeklyDataRequest( // AnalyzeWeeklyDataRequest 데이터를 묶어둘 클래스 시작
    val day: String, // day 값을 저장함
    val amount: Float // 금액을 저장함
)

data class AnalyzeMonthlyDataRequest( // AnalyzeMonthlyDataRequest 데이터를 묶어둘 클래스 시작
    val month: String, // month 값을 저장함
    val amount: Float // 금액을 저장함
)

data class AnalyzeCategoryDataRequest( // AnalyzeCategoryDataRequest 데이터를 묶어둘 클래스 시작
    val name: String, // name 값을 저장함
    val amount: Float, // 금액을 저장함
    val value: Float, // 입력값을 저장함
    val key: String? = null // key 값을 저장함
)

data class AnalyzeReportRequest( // AnalyzeReportRequest 데이터를 묶어둘 클래스 시작
    @SerializedName("report_type")
    val reportType: String, // weekly 또는 monthly 값을 저장함
    @SerializedName("start_date")
    val startDate: String, // 분석 시작일을 저장함
    @SerializedName("end_date")
    val endDate: String, // 분석 종료일을 저장함
    val transactions: List<AnalyzeTransactionRequest>, // transactions 값을 저장함
    @SerializedName("total_expense")
    val totalExpense: Float, // 소비 내역 값을 저장함
    val budget: Float, // 예산 관련 값을 저장함
    @SerializedName("top_category")
    val topCategory: String, // topCategory 값을 저장함
    @SerializedName("top_category_percent")
    val topCategoryPercent: Float, // topCategoryPercent 값을 저장함
    @SerializedName("daily_average")
    val dailyAverage: Float, // dailyAverage 값을 저장함
    @SerializedName("expense_change_rate")
    val expenseChangeRate: Float, // 소비 내역 값을 저장함
    @SerializedName("budget_usage")
    val budgetUsage: Float, // 예산 관련 값을 저장함
    @SerializedName("weekly_data")
    val weeklyData: List<AnalyzeWeeklyDataRequest>, // weeklyData 값을 저장함
    @SerializedName("monthly_data")
    val monthlyData: List<AnalyzeMonthlyDataRequest>, // monthlyData 값을 저장함
    @SerializedName("category_data")
    val categoryData: List<AnalyzeCategoryDataRequest> // categoryData 값을 저장함
)

data class AnalyzeReportResponse( // AnalyzeReportResponse 데이터를 묶어둘 클래스 시작
    val good: String, // good 값을 저장함
    val warning: String, // warning 값을 저장함
    val advice: String, // advice 값을 저장함
    val prediction: String, // prediction 값을 저장함
    val pattern: String? = null, // pattern 값을 저장함
    val improvement: String? = null // improvement 값을 저장함
)

interface AiAnalyzeApi { // AiAnalyzeApi에서 꼭 만들어야 할 함수 규칙을 정함
    @POST("/api/v1/analyze/report") // 서버에 데이터를 보내는 API 주소를 적음
    suspend fun analyzeReport( // analyzeReport 함수를 선언함
        @Body request: AnalyzeReportRequest // 이 값을 서버 요청 본문에 넣는다는 표시
    ): AnalyzeReportResponse
}
