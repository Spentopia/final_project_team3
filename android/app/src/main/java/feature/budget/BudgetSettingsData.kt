package com.ict.spentopia.feature.budget

// 예산 설정 저장 데이터 클래스임
// 월수입/지출목표 저장용
data class BudgetSettingsData(

    // 월 수입
    val monthlyIncome: Long = 500000L,

    // 저축 목표
    val savingGoal: Long = 50000L,

    // 식비
    val foodBudget: Long = 150000L,

    // 교통비
    val transportBudget: Long = 80000L,

    // 생활비
    val livingBudget: Long = 120000L,

    // 여가/취미비
    val hobbyBudget: Long = 100000L
)
