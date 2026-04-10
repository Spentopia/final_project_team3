package com.ict.spentopia.feature.budget

// 예산 설정값을 한 번에 묶어서 다루기 위한 데이터 클래스
// 이 클래스 하나에 현재 예산 상태를 전부 담음
data class BudgetSettingsData(
    // 월 수입
    val monthlyIncome: Int = 500000,

    // 저축 목표 금액
    val savingGoal: Int = 50000,

    // 식비
    val foodBudget: Int = 150000,

    // 교통비
    val transportBudget: Int = 80000,

    // 생활비
    val livingBudget: Int = 120000,

    // 취미/여가비
    val hobbyBudget: Int = 100000
)