package com.ict.spentopia.feature.budget

// 예산 관련 값을 한 번에 담는 데이터 클래스
data class BudgetState(

    // 월 수입
    val monthlyIncome: Int = 0,

    // 저축 목표
    val savingGoal: Int = 0,

    // 식비 예산
    val foodBudget: Int = 0,

    // 교통 예산
    val transportBudget: Int = 0,

    // 생활 예산
    val livingBudget: Int = 0,

    // 취미 예산
    val hobbyBudget: Int = 0
)