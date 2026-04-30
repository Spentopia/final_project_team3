package com.ict.spentopia.feature.budget

// 예산 설정 저장 데이터 클래스임
// 월수입/지출목표 저장용
data class BudgetSettingsData(

    // 월 수입
    val monthlyIncome: Int = 500000,

    // 저축 목표
    val savingGoal: Int = 50000,

    // 식비
    val foodBudget: Int = 150000,

    // 교통비
    val transportBudget: Int = 80000,

    // 생활비
    val livingBudget: Int = 120000,

    // 여가/취미비
    val hobbyBudget: Int = 100000
)
