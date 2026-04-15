package com.ict.spentopia.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

// 소비 기록 테이블 Entity
@Entity(tableName = "expenses")
data class ExpenseEntity(

    // 각 소비 기록의 고유 id
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    // 소비 날짜
    // 형식: yyyy-MM-dd
    val date: String,

    // 소비 제목
    val title: String,

    // 소비 카테고리
    val category: String,

    // 소비 금액
    val amount: Int,

    // 메모
    val memo: String,

    // 짧은 일기
    val diary: String
)