package com.ict.spentopia.data.remote

import retrofit2.http.Body
import retrofit2.http.POST

// Android에서 백엔드 DB에 소비/수입 기록을 저장할 때 보내는 요청입니다.
// 백엔드 dto의 CreateExpenseWebRequest와 이름을 맞춰야 합니다.
data class CreateExpenseRequest(
    val date: String,
    val amount: Int,
    val category: String,
    val memo: String?,
    val transactionType: String,
    val diary: String?
)

// 백엔드가 소비 저장 후 내려주는 응답입니다.
// id는 Supabase expenses 테이블의 UUID라서, OCR 인증 때 expense_id로 다시 사용합니다.
data class ExpenseRemoteResponse(
    val id: String,
    val date: String,
    val amount: Int,
    val category: String,
    val memo: String?,
    val transactionType: String?,
    val receiptVerified: Boolean?,
    val diary: String?
)

interface ExpenseApi {
    @POST("/api/expenses")
    suspend fun createExpense(
        @Body request: CreateExpenseRequest
    ): ExpenseRemoteResponse
}
