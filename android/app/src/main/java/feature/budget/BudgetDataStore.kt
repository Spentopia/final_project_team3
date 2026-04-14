package com.ict.spentopia.feature.budget

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Context에 DataStore를 연결하는 확장 프로퍼티
// "budget_settings"라는 이름으로 로컬 저장소를 만듦
private val Context.budgetDataStore by preferencesDataStore(name = "budget_settings")

// 예산 데이터를 저장하고 불러오는 클래스
class BudgetDataStore(private val context: Context) {

    companion object {
        // 각 값들을 저장할 때 사용할 key들
        private val MONTHLY_INCOME = intPreferencesKey("monthly_income")
        private val SAVING_GOAL = intPreferencesKey("saving_goal")
        private val FOOD_BUDGET = intPreferencesKey("food_budget")
        private val TRANSPORT_BUDGET = intPreferencesKey("transport_budget")
        private val LIVING_BUDGET = intPreferencesKey("living_budget")
        private val HOBBY_BUDGET = intPreferencesKey("hobby_budget")
    }

    // 기존 방식
    // 저장된 예산 데이터를 BudgetSettingsData 형태로 읽어오는 Flow
    val budgetSettingsFlow: Flow<BudgetSettingsData> =
        context.budgetDataStore.data.map { preferences ->
            BudgetSettingsData(
                // 저장된 값이 없으면 기본값 사용
                monthlyIncome = preferences[MONTHLY_INCOME] ?: 500000,
                savingGoal = preferences[SAVING_GOAL] ?: 50000,
                foodBudget = preferences[FOOD_BUDGET] ?: 150000,
                transportBudget = preferences[TRANSPORT_BUDGET] ?: 80000,
                livingBudget = preferences[LIVING_BUDGET] ?: 120000,
                hobbyBudget = preferences[HOBBY_BUDGET] ?: 100000
            )
        }

    // AnalysisViewModel / BudgetRepository에서 쓰기 쉽게
    // BudgetState 형태로도 한 번 더 제공
    val budgetState: Flow<BudgetState> =
        context.budgetDataStore.data.map { preferences ->
            BudgetState(
                // 저장된 값이 없으면 기본값 사용
                monthlyIncome = preferences[MONTHLY_INCOME] ?: 500000,
                savingGoal = preferences[SAVING_GOAL] ?: 50000,
                foodBudget = preferences[FOOD_BUDGET] ?: 150000,
                transportBudget = preferences[TRANSPORT_BUDGET] ?: 80000,
                livingBudget = preferences[LIVING_BUDGET] ?: 120000,
                hobbyBudget = preferences[HOBBY_BUDGET] ?: 100000
            )
        }

    // 현재 예산 데이터를 DataStore에 저장
    suspend fun saveBudgetSettings(settings: BudgetSettingsData) {
        context.budgetDataStore.edit { preferences ->
            preferences[MONTHLY_INCOME] = settings.monthlyIncome
            preferences[SAVING_GOAL] = settings.savingGoal
            preferences[FOOD_BUDGET] = settings.foodBudget
            preferences[TRANSPORT_BUDGET] = settings.transportBudget
            preferences[LIVING_BUDGET] = settings.livingBudget
            preferences[HOBBY_BUDGET] = settings.hobbyBudget
        }
    }
}