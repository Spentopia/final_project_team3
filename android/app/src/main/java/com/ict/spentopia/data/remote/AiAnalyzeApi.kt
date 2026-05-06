package com.ict.spentopia.data.remote

import retrofit2.http.Body
import retrofit2.http.POST

data class AnalyzeTransactionRequest(
    val date: String,
    val amount: Int,
    val category: String,
    val type: String
)

data class AnalyzeWeeklyDataRequest(
    val day: String,
    val amount: Float
)

data class AnalyzeMonthlyDataRequest(
    val month: String,
    val amount: Float
)

data class AnalyzeCategoryDataRequest(
    val name: String,
    val amount: Float,
    val value: Float,
    val key: String? = null
)

data class AnalyzeReportRequest(
    val transactions: List<AnalyzeTransactionRequest>,
    val totalExpense: Float,
    val budget: Float,
    val topCategory: String,
    val topCategoryPercent: Float,
    val dailyAverage: Float,
    val expenseChangeRate: Float,
    val budgetUsage: Float,
    val weeklyData: List<AnalyzeWeeklyDataRequest>,
    val monthlyData: List<AnalyzeMonthlyDataRequest>,
    val categoryData: List<AnalyzeCategoryDataRequest>
)

data class AnalyzeReportResponse(
    val good: String,
    val warning: String,
    val advice: String,
    val prediction: String,
    val pattern: String? = null,
    val improvement: String? = null
)

interface AiAnalyzeApi {
    @POST("/api/v1/analyze/report")
    suspend fun analyzeReport(
        @Body request: AnalyzeReportRequest
    ): AnalyzeReportResponse
}
