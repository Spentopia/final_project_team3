package com.example.spentopia.feature.budget

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// 예산 설정 상태 관리 ViewModel
// 화면에서 수정되는 값과 저장/불러오기를 담당
class BudgetViewModel(application: Application) : AndroidViewModel(application) {

    private val budgetDataStore = BudgetDataStore(application)

    // 현재 화면에서 사용하는 예산 설정 상태
    private val _budgetState = MutableStateFlow(BudgetSettingsData())
    val budgetState: StateFlow<BudgetSettingsData> = _budgetState.asStateFlow()

    // 저장 완료 여부를 화면에 알려주기 위한 상태
    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    init {
        loadBudgetSettings()
    }

    // 저장된 값 불러오기
    private fun loadBudgetSettings() {
        viewModelScope.launch {
            budgetDataStore.budgetSettingsFlow.collect { savedSettings ->
                _budgetState.value = savedSettings
            }
        }
    }

    // 월 수입 변경
    fun updateMonthlyIncome(value: Int) {
        _budgetState.value = _budgetState.value.copy(monthlyIncome = value)
    }

    // 저축 목표 변경
    fun updateSavingGoal(value: Int) {
        _budgetState.value = _budgetState.value.copy(savingGoal = value)
    }

    // 식비 변경
    fun updateFoodBudget(value: Int) {
        _budgetState.value = _budgetState.value.copy(foodBudget = value)
    }

    // 교통비 변경
    fun updateTransportBudget(value: Int) {
        _budgetState.value = _budgetState.value.copy(transportBudget = value)
    }

    // 생활비 변경
    fun updateLivingBudget(value: Int) {
        _budgetState.value = _budgetState.value.copy(livingBudget = value)
    }

    // 여가/취미 변경
    fun updateHobbyBudget(value: Int) {
        _budgetState.value = _budgetState.value.copy(hobbyBudget = value)
    }

    // 추천 플랜 적용
    fun applyPlan(plan: BudgetPlanUiData) {
        _budgetState.value = _budgetState.value.copy(
            monthlyIncome = plan.monthlyBudget,
            savingGoal = plan.savingGoal,
            foodBudget = plan.food,
            transportBudget = plan.transport,
            livingBudget = plan.living,
            hobbyBudget = plan.hobby
        )
    }

    // 현재 설정 저장
    fun saveBudgetSettings() {
        viewModelScope.launch {
            budgetDataStore.saveBudgetSettings(_budgetState.value)
            _saveSuccess.value = true
        }
    }

    // 저장 완료 상태 초기화
    fun resetSaveSuccess() {
        _saveSuccess.value = false
    }
}