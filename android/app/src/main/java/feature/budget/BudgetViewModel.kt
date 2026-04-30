package com.ict.spentopia.feature.budget

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// 예산 설정 상태 관리 VM임
// 수정값/불러오기/저장완료 상태 맡음
class BudgetViewModel(application: Application) : AndroidViewModel(application) {

    // DataStore 인스턴스 생성
    // 예산 설정 저장용임
    private val budgetDataStore = BudgetDataStore(application)

    // 현재 화면 예산 상태
    private val _budgetState = MutableStateFlow(BudgetSettingsData())
    val budgetState: StateFlow<BudgetSettingsData> = _budgetState.asStateFlow()

    // 저장 완료 여부 상태
    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    init {
        loadBudgetSettings()
    }

    // 저장값 불러옴
    // DataStore flow 계속 구독함
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
    // 슬라이더/추천 플랜 저장용
    fun saveBudgetSettings() { // 함수 선언
        viewModelScope.launch { //비동기 작업 시작 저장 작업 시간이 걸릴수 있으니  앱화면 멈추지 않게 따로 실행
            budgetDataStore.saveBudgetSettings(_budgetState.value) // 실제  현재 예상 상태 저장
            //_ budgetState.value 지금 화면이나 ViewModeldl  들고 있는 예산 설정 값이고 그값을 budgetDataStore 에 저장함
            _saveSuccess.value = true// 저장이 끝나면 저장 성공 상태를 true 로 바꾸는 부분
        }
    }

    // 저장 완료 상태 초기화
    fun resetSaveSuccess() {
        _saveSuccess.value = false
    }
}
