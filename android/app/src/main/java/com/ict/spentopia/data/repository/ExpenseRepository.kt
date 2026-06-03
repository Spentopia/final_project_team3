package com.ict.spentopia.data.repository // 이 파일이 속한 패키지 위치를 적음

import com.ict.spentopia.data.local.ExpenseDao // ExpenseDao 기능을 가져옴
import com.ict.spentopia.data.local.ExpenseEntity // ExpenseEntity 기능을 가져옴
import com.ict.spentopia.data.remote.RetrofitClient // RetrofitClient 기능을 가져옴
import kotlinx.coroutines.flow.Flow // Flow 기능을 가져옴

// 소비 기록 DB 접근 Repository임
// ViewModel-DAO 사이 중간층
class ExpenseRepository( // ExpenseRepository 기능을 묶어둔 클래스 시작
    // ExpenseDao를 주입받아서 DB 작업에 사용
    private val expenseDao: ExpenseDao // 소비 내역 값을 저장함
) { // 이 블록 안의 내용이 시작됨

    // 전체 소비 목록 조회
    fun getAllExpenses(): Flow<List<ExpenseEntity>> { // 데이터를 불러오는 함수 시작
        return expenseDao.getAllExpenses() // 이 값을 함수 결과로 돌려줌
    }

    // 특정 날짜 소비 목록 조회
    fun getExpensesByDate(date: String): Flow<List<ExpenseEntity>> { // 데이터를 불러오는 함수 시작
        return expenseDao.getExpensesByDate(date) // 이 값을 함수 결과로 돌려줌
    }

    // 특정 월 소비 목록 조회
    // yearMonth 예: "2026-04"
    fun getExpensesByMonth(yearMonth: String): Flow<List<ExpenseEntity>> { // 데이터를 불러오는 함수 시작
        return expenseDao.getExpensesByMonth(yearMonth) // 이 값을 함수 결과로 돌려줌
    }

    // 소비 추가
    suspend fun insertExpense(expense: ExpenseEntity) { // 데이터를 저장하는 함수 시작
        expenseDao.insertExpense(expense)
    }

    // 소비 수정
    suspend fun updateExpense(expense: ExpenseEntity) { // 데이터를 수정하는 함수 시작
        expenseDao.updateExpense(expense)
    }

    // 소비 삭제
    suspend fun deleteExpense(expense: ExpenseEntity) { // 데이터를 삭제하는 함수 시작
        expenseDao.deleteExpense(expense)
    }

    // id로 단일 소비 조회
    suspend fun getExpenseById(id: Long): ExpenseEntity? { // 데이터를 불러오는 함수 시작
        return expenseDao.getExpenseById(id) // 이 값을 함수 결과로 돌려줌
    }

    // 서버에서 소비 목록을 받아와 로컬 DB에 없는 항목만 저장합니다.
    // 웹에서 입력한 가계부가 앱 분석 화면에 반영되도록 하기 위한 동기화 함수입니다.
    suspend fun syncFromServer() {
        val remoteItems = RetrofitClient.expenseApi.listExpenses().items
        if (remoteItems.isEmpty()) return

        val existingIds = expenseDao.getAllServerExpenseIds().toHashSet()

        val newEntities = remoteItems
            .filter { it.id !in existingIds }
            .map { remote ->
                val koreanCategory = toKoreanCategory(remote.category)
                ExpenseEntity(
                    date = remote.date,
                    title = remote.memo?.takeIf { it.isNotBlank() } ?: koreanCategory,
                    category = koreanCategory,
                    amount = remote.amount,
                    memo = remote.memo ?: "",
                    diary = remote.diary ?: "",
                    serverExpenseId = remote.id,
                    receiptVerified = remote.receiptVerified ?: false
                )
            }

        if (newEntities.isNotEmpty()) {
            expenseDao.insertExpenses(newEntities)
        }
    }
}

// 웹에서 사용하는 영어 카테고리를 앱 한국어 카테고리로 변환합니다.
private fun toKoreanCategory(category: String): String = when (category) {
    "food" -> "식비"
    "transport" -> "교통"
    "shopping" -> "쇼핑"
    "cafe" -> "카페"
    "entertainment", "leisure", "hobby" -> "여가"
    "health", "medical" -> "의료"
    "education" -> "교육"
    "utility", "bill" -> "공과금"
    "salary" -> "월급"
    "allowance" -> "용돈"
    "bonus" -> "보너스"
    "side_job" -> "부수입"
    "investment" -> "투자"
    "income_other" -> "기타수입"
    "other" -> "기타"
    else -> category
}
