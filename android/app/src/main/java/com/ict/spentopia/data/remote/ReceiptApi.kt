package com.ict.spentopia.data.remote // 이 파일이 속한 패키지 위치를 적음

import okhttp3.MultipartBody // MultipartBody 기능을 가져옴
import okhttp3.RequestBody // RequestBody 기능을 가져옴
import retrofit2.http.Multipart // Multipart 기능을 가져옴
import retrofit2.http.POST // POST API 표시를 가져옴
import retrofit2.http.Part // Part 기능을 가져옴
import retrofit2.http.Query // 주소 뒤에 붙는 요청값 표시를 가져옴

data class ReceiptOcrResponse( // ReceiptOcrResponse 데이터를 묶어둘 클래스 시작
    val ocr: ReceiptOcrData, // ocr 값을 저장함
    val expected: ReceiptOcrExpected, // expected 값을 저장함
    val verification: ReceiptOcrVerification // verification 값을 저장함
)

data class ReceiptOcrData( // ReceiptOcrData 데이터를 묶어둘 클래스 시작
    val merchant_name: String?, // merchant_name 값을 저장함
    val receipt_date: String?, // receipt_date 값을 저장함
    val total_amount: Int?, // total_amount 값을 저장함
    val raw_text: String?, // raw_text 값을 저장함
    val confidence: Double?, // confidence 값을 저장함
    val error: String? // 오류 내용을 저장함
)

data class ReceiptOcrExpected( // ReceiptOcrExpected 데이터를 묶어둘 클래스 시작
    val date: String, // 날짜을 저장함
    val amount: Int // 금액을 저장함
)

data class ReceiptOcrVerification( // ReceiptOcrVerification 데이터를 묶어둘 클래스 시작
    val is_verified: Boolean, // 인증됐는지 저장함
    val date_matched: Boolean, // date_matched 값을 저장함
    val amount_matched: Boolean, // amount_matched 값을 저장함
    val is_recent_receipt: Boolean?, // 최근 영수증인지 저장함
    val reason: String // reason 값을 저장함
)

interface ReceiptApi { // ReceiptApi에서 꼭 만들어야 할 함수 규칙을 정함
    @Multipart // 이 코드에 특별한 역할을 붙이는 표시
    @POST("/api/v1/receipt/ocr") // 서버에 데이터를 보내는 API 주소를 적음
    suspend fun verifyReceiptOcr( // verifyReceiptOcr 함수를 선언함
        @Query("expense_id") expenseId: String? = null, // 이 값을 주소 뒤 요청값으로 보낸다는 표시
        @Part image: MultipartBody.Part, // 이 코드에 특별한 역할을 붙이는 표시
        @Part("expected_date") expectedDate: RequestBody, // 이 코드에 특별한 역할을 붙이는 표시
        @Part("expected_amount") expectedAmount: RequestBody // 이 코드에 특별한 역할을 붙이는 표시
    ): ReceiptOcrResponse
}
