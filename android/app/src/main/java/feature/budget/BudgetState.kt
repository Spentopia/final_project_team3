package com.ict.spentopia.feature.budget // 이 파일이 속한 패키지 위치를 적음

// 예산 값 묶음 데이터 클래스임
// 화면/추천플랜/저장 로직 공용
data class BudgetState( // BudgetState 데이터를 묶어둘 클래스 시작

    // 월 수입
    val monthlyIncome: Long = 0L, // 월 수입을 저장함

    // 저축 목표
    val savingGoal: Long = 0L, // 저축 목표을 저장함

    // 식비 예산
    val foodBudget: Long = 0L, // 식비 예산을 저장함

    // 교통 예산
    val transportBudget: Long = 0L, // 교통비 예산을 저장함

    // 생활 예산
    val livingBudget: Long = 0L, // 생활비 예산을 저장함

    // 취미 예산
    val hobbyBudget: Long = 0L // 취미 예산을 저장함
)
