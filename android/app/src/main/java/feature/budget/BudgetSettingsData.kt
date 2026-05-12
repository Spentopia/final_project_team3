package com.ict.spentopia.feature.budget // 이 파일이 속한 패키지 위치를 적음

// 예산 설정 저장 데이터 클래스임
// 월수입/지출목표 저장용
data class BudgetSettingsData( // BudgetSettingsData 데이터를 묶어둘 클래스 시작

    // 월 수입
    val monthlyIncome: Long = 500000L, // 월 수입을 저장함

    // 저축 목표
    val savingGoal: Long = 50000L, // 저축 목표을 저장함

    // 식비
    val foodBudget: Long = 150000L, // 식비 예산을 저장함

    // 교통비
    val transportBudget: Long = 80000L, // 교통비 예산을 저장함

    // 생활비
    val livingBudget: Long = 120000L, // 생활비 예산을 저장함

    // 여가/취미비
    val hobbyBudget: Long = 100000L // 취미 예산을 저장함
)
