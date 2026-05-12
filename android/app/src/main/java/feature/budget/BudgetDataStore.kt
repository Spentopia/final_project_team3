package com.ict.spentopia.feature.budget // 이 파일이 속한 패키지 위치를 적음

import android.content.Context // 현재 화면 정보 타입을 가져옴
import androidx.datastore.preferences.core.edit // edit 기능을 가져옴
import androidx.datastore.preferences.core.intPreferencesKey // intPreferencesKey 기능을 가져옴
import androidx.datastore.preferences.core.longPreferencesKey // longPreferencesKey 기능을 가져옴
import androidx.datastore.preferences.preferencesDataStore // preferencesDataStore 기능을 가져옴
import kotlinx.coroutines.flow.Flow // Flow 기능을 가져옴
import kotlinx.coroutines.flow.map // map 기능을 가져옴

// Context에 DataStore를 연결하는 확장 프로퍼티
// "budget_settings"라는 이름으로 로컬 저장소를 만듦
private val Context.budgetDataStore by preferencesDataStore(name = "budget_settings") // Context 값을 저장함

// 예산 데이터를 저장하고 불러오는 클래스
class BudgetDataStore(private val context: Context) { // BudgetDataStore 기능을 묶어둔 클래스 시작

    companion object { // 이 블록 안의 내용이 시작됨
        // 각 값들을 저장할 때 사용할 key들
        private val MONTHLY_INCOME = longPreferencesKey("monthly_income_long") // MONTHLY_INCOME 값을 저장함
        private val SAVING_GOAL = longPreferencesKey("saving_goal_long") // SAVING_GOAL 값을 저장함
        private val FOOD_BUDGET = longPreferencesKey("food_budget_long") // 예산 관련 값을 저장함
        private val TRANSPORT_BUDGET = longPreferencesKey("transport_budget_long") // 예산 관련 값을 저장함
        private val LIVING_BUDGET = longPreferencesKey("living_budget_long") // 예산 관련 값을 저장함
        private val HOBBY_BUDGET = longPreferencesKey("hobby_budget_long") // 예산 관련 값을 저장함

        private val LEGACY_MONTHLY_INCOME = intPreferencesKey("monthly_income") // LEGACY_MONTHLY_INCOME 값을 저장함
        private val LEGACY_SAVING_GOAL = intPreferencesKey("saving_goal") // LEGACY_SAVING_GOAL 값을 저장함
        private val LEGACY_FOOD_BUDGET = intPreferencesKey("food_budget") // 예산 관련 값을 저장함
        private val LEGACY_TRANSPORT_BUDGET = intPreferencesKey("transport_budget") // 예산 관련 값을 저장함
        private val LEGACY_LIVING_BUDGET = intPreferencesKey("living_budget") // 예산 관련 값을 저장함
        private val LEGACY_HOBBY_BUDGET = intPreferencesKey("hobby_budget") // 예산 관련 값을 저장함
    }

    // 저장된 예산 데이터를 읽어오는 Flow
    // 값이 바뀌면 자동으로 새 데이터를 내보냄
    val budgetSettingsFlow: Flow<BudgetSettingsData> = // 예산 관련 값을 저장함
        context.budgetDataStore.data.map { preferences ->
            BudgetSettingsData( // Budget Settings Data 함수를 실행함
                // 저장된 값이 없으면 기본값 사용
                monthlyIncome = preferences[MONTHLY_INCOME] ?: preferences[LEGACY_MONTHLY_INCOME]?.toLong() ?: 500000L, // 월 수입을 정해줌
                savingGoal = preferences[SAVING_GOAL] ?: preferences[LEGACY_SAVING_GOAL]?.toLong() ?: 50000L, // 저축 목표를 정해줌
                foodBudget = preferences[FOOD_BUDGET] ?: preferences[LEGACY_FOOD_BUDGET]?.toLong() ?: 150000L, // 식비 예산을 정해줌
                transportBudget = preferences[TRANSPORT_BUDGET] ?: preferences[LEGACY_TRANSPORT_BUDGET]?.toLong() ?: 80000L, // 교통비 예산을 정해줌
                livingBudget = preferences[LIVING_BUDGET] ?: preferences[LEGACY_LIVING_BUDGET]?.toLong() ?: 120000L, // 생활비 예산을 정해줌
                hobbyBudget = preferences[HOBBY_BUDGET] ?: preferences[LEGACY_HOBBY_BUDGET]?.toLong() ?: 100000L // 취미 예산을 정해줌
            )
        }

    // 현재 예산 데이터를 DataStore에 저장
    suspend fun saveBudgetSettings(settings: BudgetSettingsData) { // 데이터를 저장하는 함수 시작
        context.budgetDataStore.edit { preferences ->
            preferences[MONTHLY_INCOME] = settings.monthlyIncome // preferences[MONTHLY_INCOME] 값을 정해줌
            preferences[SAVING_GOAL] = settings.savingGoal // preferences[SAVING_GOAL] 값을 정해줌
            preferences[FOOD_BUDGET] = settings.foodBudget // 예산 관련 값을 정해줌
            preferences[TRANSPORT_BUDGET] = settings.transportBudget // 예산 관련 값을 정해줌
            preferences[LIVING_BUDGET] = settings.livingBudget // 예산 관련 값을 정해줌
            preferences[HOBBY_BUDGET] = settings.hobbyBudget // 예산 관련 값을 정해줌
        }
    }
}
