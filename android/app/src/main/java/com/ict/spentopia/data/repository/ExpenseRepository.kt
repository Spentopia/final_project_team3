package com.ict.spentopia.data.repository

import com.ict.spentopia.data.local.ExpenseDao
import com.ict.spentopia.data.local.ExpenseEntity
import kotlinx.coroutines.flow.Flow

// 소비 기록 DB 접근 Repository임
// ViewModel-DAO 사이 중간층
class ExpenseRepository(
    // ExpenseDao를 주입받아서 DB 작업에 사용
    private val expenseDao: ExpenseDao
) {

    // 전체 소비 목록 조회
    fun getAllExpenses(): Flow<List<ExpenseEntity>> {
        return expenseDao.getAllExpenses()
    }

    // 특정 날짜 소비 목록 조회
    fun getExpensesByDate(date: String): Flow<List<ExpenseEntity>> {
        return expenseDao.getExpensesByDate(date)
    }

    // 특정 월 소비 목록 조회
    // yearMonth 예: "2026-04"
    fun getExpensesByMonth(yearMonth: String): Flow<List<ExpenseEntity>> {
        return expenseDao.getExpensesByMonth(yearMonth)
    }

    // 소비 추가
    suspend fun insertExpense(expense: ExpenseEntity) {
        expenseDao.insertExpense(expense)
    }

    // 소비 수정
    suspend fun updateExpense(expense: ExpenseEntity) {
        expenseDao.updateExpense(expense)
    }

    // 소비 삭제
    suspend fun deleteExpense(expense: ExpenseEntity) {
        expenseDao.deleteExpense(expense)
    }

    // id로 단일 소비 조회
    suspend fun getExpenseById(id: Long): ExpenseEntity? {
        return expenseDao.getExpenseById(id)
    }
}
