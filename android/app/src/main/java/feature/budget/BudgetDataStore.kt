package com.ict.spentopia.feature.budget

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
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
        private val MONTHLY_INCOME = longPreferencesKey("monthly_income_long")
        private val SAVING_GOAL = longPreferencesKey("saving_goal_long")
        private val FOOD_BUDGET = longPreferencesKey("food_budget_long")
        private val TRANSPORT_BUDGET = longPreferencesKey("transport_budget_long")
        private val LIVING_BUDGET = longPreferencesKey("living_budget_long")
        private val HOBBY_BUDGET = longPreferencesKey("hobby_budget_long")

        private val LEGACY_MONTHLY_INCOME = intPreferencesKey("monthly_income")
        private val LEGACY_SAVING_GOAL = intPreferencesKey("saving_goal")
        private val LEGACY_FOOD_BUDGET = intPreferencesKey("food_budget")
        private val LEGACY_TRANSPORT_BUDGET = intPreferencesKey("transport_budget")
        private val LEGACY_LIVING_BUDGET = intPreferencesKey("living_budget")
        private val LEGACY_HOBBY_BUDGET = intPreferencesKey("hobby_budget")
    }

    // 저장된 예산 데이터를 읽어오는 Flow
    // 값이 바뀌면 자동으로 새 데이터를 내보냄
    val budgetSettingsFlow: Flow<BudgetSettingsData> =
        context.budgetDataStore.data.map { preferences ->
            BudgetSettingsData(
                // 저장된 값이 없으면 기본값 사용
                monthlyIncome = preferences[MONTHLY_INCOME] ?: preferences[LEGACY_MONTHLY_INCOME]?.toLong() ?: 500000L,
                savingGoal = preferences[SAVING_GOAL] ?: preferences[LEGACY_SAVING_GOAL]?.toLong() ?: 50000L,
                foodBudget = preferences[FOOD_BUDGET] ?: preferences[LEGACY_FOOD_BUDGET]?.toLong() ?: 150000L,
                transportBudget = preferences[TRANSPORT_BUDGET] ?: preferences[LEGACY_TRANSPORT_BUDGET]?.toLong() ?: 80000L,
                livingBudget = preferences[LIVING_BUDGET] ?: preferences[LEGACY_LIVING_BUDGET]?.toLong() ?: 120000L,
                hobbyBudget = preferences[HOBBY_BUDGET] ?: preferences[LEGACY_HOBBY_BUDGET]?.toLong() ?: 100000L
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
