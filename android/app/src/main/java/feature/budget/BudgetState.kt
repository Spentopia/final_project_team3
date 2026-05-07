package com.ict.spentopia.feature.budget

// 예산 값 묶음 데이터 클래스임
// 화면/추천플랜/저장 로직 공용
data class BudgetState(

    // 월 수입
    val monthlyIncome: Long = 0L,

    // 저축 목표
    val savingGoal: Long = 0L,

    // 식비 예산
    val foodBudget: Long = 0L,

    // 교통 예산
    val transportBudget: Long = 0L,

    // 생활 예산
    val livingBudget: Long = 0L,

    // 취미 예산
    val hobbyBudget: Long = 0L
)
