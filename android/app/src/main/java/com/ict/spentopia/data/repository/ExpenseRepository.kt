package com.ict.spentopia.data.repository

import com.ict.spentopia.feature.budget.BudgetDataStore
import com.ict.spentopia.feature.budget.BudgetState
import kotlinx.coroutines.flow.Flow

// 예산 DataStore에 접근하는 Repository 클래스
class BudgetRepository(
    private val budgetDataStore: BudgetDataStore
) {

    // DataStore에 저장된 예산 상태를 Flow로 외부에 전달
    val budgetState: Flow<BudgetState> = budgetDataStore.budgetState
}