package com.ict.spentopia.data.local // 이 파일이 속한 패키지 위치를 적음

import androidx.room.Dao // Dao 기능을 가져옴
import androidx.room.Delete // Delete 기능을 가져옴
import androidx.room.Insert // Insert 기능을 가져옴
import androidx.room.Query // DB 조회 SQL문 표시를 가져옴
import androidx.room.Update // Update 기능을 가져옴
import kotlinx.coroutines.flow.Flow // Flow 기능을 가져옴

// 소비 기록 DB 접근용 DAO입니다.
// Room이 실제 SQL 쿼리를 실행하는 진입점입니다.
@Dao // 이 인터페이스가 DB 작업용이라는 표시
interface ExpenseDao { // ExpenseDao에서 꼭 만들어야 할 함수 규칙을 정함

    // 전체 소비 목록 조회
    @Query("SELECT * FROM expenses ORDER BY date DESC, id DESC") // DB에서 전체 소비 목록을 가져오는 SQL문
    fun getAllExpenses(): Flow<List<ExpenseEntity>> // 데이터를 불러오는 함수 시작

    // 특정 날짜 소비 목록 조회
    @Query("SELECT * FROM expenses WHERE date = :date ORDER BY id DESC") // DB에서 선택한 날짜 소비를 가져오는 SQL문
    fun getExpensesByDate(date: String): Flow<List<ExpenseEntity>> // 데이터를 불러오는 함수 시작

    // 특정 월 소비 목록 조회
    // 예: 2026-04
    @Query("SELECT * FROM expenses WHERE substr(date, 1, 7) = :yearMonth ORDER BY date DESC, id DESC") // DB에서 선택한 월 소비를 가져오는 SQL문
    fun getExpensesByMonth(yearMonth: String): Flow<List<ExpenseEntity>> // 데이터를 불러오는 함수 시작

    // 단일 소비 조회
    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1") // DB에서 아이디에 맞는 소비 하나를 가져오는 SQL문
    suspend fun getExpenseById(id: Long): ExpenseEntity? // 데이터를 불러오는 함수 시작

    // 소비 추가
    @Insert // DB에 데이터를 추가하는 기능이라는 표시
    suspend fun insertExpense(expense: ExpenseEntity) // 데이터를 저장하는 함수 시작

    // 소비 수정
    @Update // DB에 저장된 데이터를 수정하는 기능이라는 표시
    suspend fun updateExpense(expense: ExpenseEntity) // 데이터를 수정하는 함수 시작

    // 소비 삭제
    @Delete // DB에서 데이터를 삭제하는 기능이라는 표시
    suspend fun deleteExpense(expense: ExpenseEntity) // 데이터를 삭제하는 함수 시작
}
