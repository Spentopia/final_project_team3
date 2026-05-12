package com.ict.spentopia.data.repository // 이 파일이 속한 패키지 위치를 적음

import com.ict.spentopia.data.local.ExpenseDao // ExpenseDao 기능을 가져옴
import com.ict.spentopia.data.local.ExpenseEntity // ExpenseEntity 기능을 가져옴
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
}
