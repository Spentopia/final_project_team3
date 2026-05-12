package com.ict.spentopia.data.local // 이 파일이 속한 패키지 위치를 적음

import androidx.room.Entity // Entity 기능을 가져옴
import androidx.room.PrimaryKey // PrimaryKey 기능을 가져옴

// 소비 기록 테이블 Entity임
// 홈/분석/보상 로직 공통 데이터
@Entity(tableName = "expenses") // 이 클래스가 DB 테이블이라는 표시
data class ExpenseEntity( // ExpenseEntity 데이터를 묶어둘 클래스 시작

    // 각 소비 기록의 고유 id
    @PrimaryKey(autoGenerate = true) // 이 코드에 특별한 역할을 붙이는 표시
    val id: Long = 0L, // 아이디를 저장함

    // 소비 날짜
    // 형식: yyyy-MM-dd
    val date: String, // 날짜을 저장함

    // 소비 제목
    val title: String, // 제목을 저장함

    // 소비 카테고리
    val category: String, // 카테고리을 저장함

    // 소비 금액
    val amount: Int, // 금액을 저장함

    // 메모
    val memo: String, // 메모을 저장함

    // 짧은 일기
    val diary: String, // diary 값을 저장함

    // 영수증 이미지 Uri 문자열

    val receiptImageUri: String = "", // receiptImageUri 값을 저장함

    // 백엔드 expenses 테이블의 UUID입니다.
    // 로컬 Room id(Long)와 서버 DB id(UUID)는 서로 다르기 때문에 따로 저장합니다.
    val serverExpenseId: String = "", // 소비 내역 값을 저장함

    // 백엔드 OCR 인증 결과입니다.
    // true면 서버에서도 receipt_verified=true로 반영된 기록입니다.
    val receiptVerified: Boolean = false // receiptVerified 값을 저장함
)
