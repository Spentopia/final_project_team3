package com.ict.spentopia.data.repository // 이 파일이 속한 패키지 위치를 적음

import com.ict.spentopia.feature.budget.BudgetDataStore // BudgetDataStore 기능을 가져옴
import com.ict.spentopia.feature.budget.BudgetSettingsData // BudgetSettingsData 기능을 가져옴
import kotlinx.coroutines.flow.Flow // Flow 기능을 가져옴

// 예산 DataStore에 접근하는 Repository 클래스
class BudgetDataRepository( // BudgetDataRepository 기능을 묶어둔 클래스 시작
    private val budgetDataStore: BudgetDataStore // 예산 관련 값을 저장함
) { // 이 블록 안의 내용이 시작됨

    // DataStore에 저장된 예산 설정값을 Flow로 외부에 전달
    val budgetSettingsFlow: Flow<BudgetSettingsData> = budgetDataStore.budgetSettingsFlow // 예산 관련 값을 저장함
}