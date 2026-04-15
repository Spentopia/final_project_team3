package com.ict.spentopia.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// 소비 기록 DB 접근용 DAO
@Dao
interface ExpenseDao {

    // 전체 소비 목록 조회
    @Query("SELECT * FROM expenses ORDER BY date DESC, id DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    // 특정 날짜 소비 목록 조회
    @Query("SELECT * FROM expenses WHERE date = :date ORDER BY id DESC")
    fun getExpensesByDate(date: String): Flow<List<ExpenseEntity>>

    // 특정 월 소비 목록 조회
    // 예: 2026-04
    @Query("SELECT * FROM expenses WHERE substr(date, 1, 7) = :yearMonth ORDER BY date DESC, id DESC")
    fun getExpensesByMonth(yearMonth: String): Flow<List<ExpenseEntity>>

    // 단일 소비 조회
    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    suspend fun getExpenseById(id: Long): ExpenseEntity?

    // 소비 추가
    @Insert
    suspend fun insertExpense(expense: ExpenseEntity)

    // 소비 수정
    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    // 소비 삭제
    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)
}