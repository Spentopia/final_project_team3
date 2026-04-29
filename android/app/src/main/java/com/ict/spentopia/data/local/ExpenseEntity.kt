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
    val diary: String,

    // 영수증 이미지 Uri 문자열

    val receiptImageUri: String = "",

    // 백엔드 expenses 테이블의 UUID입니다.
    // 로컬 Room id(Long)와 서버 DB id(UUID)는 서로 다르기 때문에 따로 저장합니다.
    val serverExpenseId: String = "",

    // 백엔드 OCR 인증 결과입니다.
    // true면 서버에서도 receipt_verified=true로 반영된 기록입니다.
    val receiptVerified: Boolean = false
)
