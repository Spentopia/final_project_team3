package com.ict.spentopia.data.remote // 이 파일이 속한 패키지 위치를 적음

import retrofit2.http.Body // 서버로 보낼 값을 표시하는 도구를 가져옴
import retrofit2.http.GET // GET API 표시를 가져옴
import retrofit2.http.POST // POST API 표시를 가져옴

// Android에서 백엔드 DB에 소비/수입 기록을 저장할 때 보내는 요청입니다.
// 백엔드 dto의 CreateExpenseWebRequest와 이름을 맞춰야 합니다.
data class CreateExpenseRequest( // CreateExpenseRequest 데이터를 묶어둘 클래스 시작
    val date: String, // 날짜을 저장함
    val amount: Int, // 금액을 저장함
    val category: String, // 카테고리을 저장함
    val memo: String?, // 메모을 저장함
    val transactionType: String, // transactionType 값을 저장함
    val diary: String? // diary 값을 저장함
)

// 백엔드가 소비 저장 후 내려주는 응답입니다.
// id는 Supabase expenses 테이블의 UUID라서, OCR 인증 때 expense_id로 다시 사용합니다.
data class ExpenseRemoteResponse( // ExpenseRemoteResponse 데이터를 묶어둘 클래스 시작
    val id: String, // 아이디를 저장함
    val date: String, // 날짜을 저장함
    val amount: Int, // 금액을 저장함
    val category: String, // 카테고리을 저장함
    val memo: String?, // 메모을 저장함
    val transactionType: String?, // transactionType 값을 저장함
    val receiptVerified: Boolean?, // receiptVerified 값을 저장함
    val diary: String? // diary 값을 저장함
)

data class ExpenseListRemoteResponse( // 서버에서 받아오는 지출 목록 응답
    val items: List<ExpenseRemoteResponse>
)

interface ExpenseApi { // ExpenseApi에서 꼭 만들어야 할 함수 규칙을 정함
    @GET("/api/expenses") // 서버에서 소비 목록을 가져오는 API
    suspend fun listExpenses(): ExpenseListRemoteResponse

    @POST("/api/expenses") // 서버에 데이터를 보내는 API 주소를 적음
    suspend fun createExpense( // 데이터를 저장하는 함수 시작
        @Body request: CreateExpenseRequest // 이 값을 서버 요청 본문에 넣는다는 표시
    ): ExpenseRemoteResponse
}
