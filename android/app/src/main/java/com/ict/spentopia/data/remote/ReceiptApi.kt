package com.ict.spentopia.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

data class ReceiptOcrResponse(
    val ocr: ReceiptOcrData,
    val expected: ReceiptOcrExpected,
    val verification: ReceiptOcrVerification
)

data class ReceiptOcrData(
    val merchant_name: String?,
    val receipt_date: String?,
    val total_amount: Int?,
    val raw_text: String?,
    val confidence: Double?,
    val error: String?
)

data class ReceiptOcrExpected(
    val date: String,
    val amount: Int
)

data class ReceiptOcrVerification(
    val is_verified: Boolean,
    val date_matched: Boolean,
    val amount_matched: Boolean,
    val is_recent_receipt: Boolean?,
    val reason: String
)

interface ReceiptApi {
    @Multipart
    @POST("/api/v1/receipt/ocr")
    suspend fun verifyReceiptOcr(
        @Query("expense_id") expenseId: String? = null,
        @Part image: MultipartBody.Part,
        @Part("expected_date") expectedDate: RequestBody,
        @Part("expected_amount") expectedAmount: RequestBody
    ): ReceiptOcrResponse
}
