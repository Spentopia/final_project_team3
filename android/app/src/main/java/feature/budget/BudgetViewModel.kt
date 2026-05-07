package com.ict.spentopia.feature.budget

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ict.spentopia.data.remote.BudgetCategoryItem
import com.ict.spentopia.data.remote.BudgetResponse
import com.ict.spentopia.data.remote.CreateBudgetRequest
import com.ict.spentopia.data.remote.RetrofitClient
import com.ict.spentopia.data.remote.UpdateBudgetCategoriesRequest
import com.ict.spentopia.data.remote.UpdateBudgetRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.util.Calendar

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

    private val _saveError = MutableStateFlow("")
    val saveError: StateFlow<String> = _saveError.asStateFlow()

    private val _aiPlanList = MutableStateFlow<List<BudgetPlanUiData>>(emptyList())
    val aiPlanList: StateFlow<List<BudgetPlanUiData>> = _aiPlanList.asStateFlow()

    private val _isAiPlanLoading = MutableStateFlow(false)
    val isAiPlanLoading: StateFlow<Boolean> = _isAiPlanLoading.asStateFlow()

    private val _aiPlanError = MutableStateFlow("")
    val aiPlanError: StateFlow<String> = _aiPlanError.asStateFlow()

    private var currentBudgetId: String? = null
    private var lastAiPlanRequestSettings: BudgetSettingsData? = null

    init {
        loadBudgetSettings()
        syncCurrentMonthBudgetFromBackend()
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
    fun updateMonthlyIncome(value: Long) {
        _budgetState.value = _budgetState.value.copy(monthlyIncome = value.coerceAtLeast(0L))
    }

    // 저축 목표 변경
    fun updateSavingGoal(value: Long) {
        _budgetState.value = _budgetState.value.copy(savingGoal = value.coerceAtLeast(0L))
    }

    // 식비 변경
    fun updateFoodBudget(value: Long) {
        _budgetState.value = _budgetState.value.copy(foodBudget = value.coerceAtLeast(0L))
    }

    // 교통비 변경
    fun updateTransportBudget(value: Long) {
        _budgetState.value = _budgetState.value.copy(transportBudget = value.coerceAtLeast(0L))
    }

    // 생활비 변경
    fun updateLivingBudget(value: Long) {
        _budgetState.value = _budgetState.value.copy(livingBudget = value.coerceAtLeast(0L))
    }

    // 여가/취미 변경
    fun updateHobbyBudget(value: Long) {
        _budgetState.value = _budgetState.value.copy(hobbyBudget = value.coerceAtLeast(0L))
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
            val currentSettings = _budgetState.value
            budgetDataStore.saveBudgetSettings(currentSettings) // 실제  현재 예상 상태 저장
            //_ budgetState.value 지금 화면이나 ViewModeldl  들고 있는 예산 설정 값이고 그값을 budgetDataStore 에 저장함

            try {
                upsertBackendBudget(currentSettings)
                _saveError.value = ""
                _saveSuccess.value = true// 저장이 끝나면 저장 성공 상태를 true 로 바꾸는 부분
            } catch (e: HttpException) {
                _saveError.value = when (e.code()) {
                    401 -> "로그인이 만료되었습니다. 다시 로그인해주세요."
                    else -> "예산 저장에 실패했습니다. 잠시 후 다시 시도해주세요. (${e.code()})"
                }
            } catch (e: Exception) {
                _saveError.value = "예산 저장에 실패했습니다. 잠시 후 다시 시도해주세요."
            }
        }
    }

    // 저장 완료 상태 초기화
    fun resetSaveSuccess() {
        _saveSuccess.value = false
    }

    fun resetSaveError() {
        _saveError.value = ""
    }

    fun requestAiRecommendedPlans() {
        if (_isAiPlanLoading.value) return

        val requestSettings = _budgetState.value
        if (_aiPlanList.value.isNotEmpty() && lastAiPlanRequestSettings == requestSettings) {
            return
        }

        viewModelScope.launch {
            _isAiPlanLoading.value = true
            _aiPlanError.value = ""

            try {
                val budgetId = upsertBackendBudget(requestSettings)
                val response = RetrofitClient.budgetApi.generateAiPlan(budgetId)
                _aiPlanList.value = response.plans.map { plan ->
                    BudgetPlanUiData(
                        title = plan.name,
                        description = plan.description,
                        monthlyBudget = plan.budget,
                        savingGoal = plan.savings,
                        food = plan.food,
                        transport = plan.transport,
                        living = plan.living,
                        hobby = plan.leisure,
                        saving = plan.savings
                    )
                }
                lastAiPlanRequestSettings = requestSettings
                _aiPlanError.value = if (response.plans.isEmpty()) {
                    "AI 추천 플랜이 비어 있습니다."
                } else {
                    ""
                }
            } catch (e: HttpException) {
                _aiPlanError.value = when (e.code()) {
                    401 -> "로그인이 만료되었습니다. 다시 로그인해주세요."
                    404 -> "예산 정보를 찾지 못했습니다. 설정 저장 후 다시 시도해주세요."
                    500, 502 -> "AI 추천 플랜을 불러오지 못했습니다. 잠시 후 다시 시도해주세요."
                    else -> "AI 추천 플랜 요청에 실패했습니다. (${e.code()})"
                }
            } catch (e: Exception) {
                _aiPlanError.value = "AI 추천 플랜 요청에 실패했습니다. 잠시 후 다시 시도해주세요."
            } finally {
                _isAiPlanLoading.value = false
            }
        }
    }

    private fun syncCurrentMonthBudgetFromBackend() {
        viewModelScope.launch {
            try {
                val (year, month) = currentYearMonth()
                val response = RetrofitClient.budgetApi.getBudget(year = year, month = month)
                currentBudgetId = response.id

                val syncedSettings = response.toBudgetSettingsData(_budgetState.value)
                _budgetState.value = syncedSettings
                budgetDataStore.saveBudgetSettings(syncedSettings)
            } catch (e: HttpException) {
                if (e.code() != 404 && e.code() != 401) {
                    _saveError.value = "예산 정보를 불러오지 못했습니다. (${e.code()})"
                }
            } catch (_: Exception) {
                // 예산 화면은 로컬 설정으로도 동작해야 하므로 초기 조회 실패는 조용히 넘깁니다.
            }
        }
    }

    private suspend fun upsertBackendBudget(settings: BudgetSettingsData): String {
        val (year, month) = currentYearMonth()
        val budgetId = currentBudgetId ?: findOrCreateBudget(year, month, settings).id.also {
            currentBudgetId = it
        }

        RetrofitClient.budgetApi.updateBudget(
            budgetId = budgetId,
            request = UpdateBudgetRequest(
                total_budget = settings.monthlyIncome,
                savings_goal = settings.savingGoal
            )
        )

        RetrofitClient.budgetApi.updateCategories(
            budgetId = budgetId,
            request = UpdateBudgetCategoriesRequest(
                categories = settings.toBudgetCategoryItems()
            )
        )

        return budgetId
    }

    private suspend fun findOrCreateBudget(
        year: Int,
        month: Int,
        settings: BudgetSettingsData
    ): BudgetResponse {
        return try {
            RetrofitClient.budgetApi.getBudget(year = year, month = month)
        } catch (e: HttpException) {
            if (e.code() != 404) throw e

            RetrofitClient.budgetApi.createBudget(
                CreateBudgetRequest(
                    year = year,
                    month = month,
                    total_budget = settings.monthlyIncome,
                    savings_goal = settings.savingGoal
                )
            )
        }
    }

    private fun currentYearMonth(): Pair<Int, Int> {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.YEAR) to calendar.get(Calendar.MONTH) + 1
    }

    private fun BudgetSettingsData.toBudgetCategoryItems(): List<BudgetCategoryItem> {
        return listOf(
            BudgetCategoryItem(category = "food", allocated_amount = foodBudget),
            BudgetCategoryItem(category = "transport", allocated_amount = transportBudget),
            BudgetCategoryItem(category = "living", allocated_amount = livingBudget),
            BudgetCategoryItem(category = "leisure", allocated_amount = hobbyBudget)
        )
    }

    private fun BudgetResponse.toBudgetSettingsData(fallback: BudgetSettingsData): BudgetSettingsData {
        fun amountOf(vararg names: String, fallbackValue: Long): Long {
            return categories
                .firstOrNull { item ->
                    names.any { name -> item.category.equals(name, ignoreCase = true) }
                }
                ?.allocated_amount
                ?: fallbackValue
        }

        return BudgetSettingsData(
            monthlyIncome = total_budget,
            savingGoal = savings_goal ?: fallback.savingGoal,
            foodBudget = amountOf("food", "식비", fallbackValue = fallback.foodBudget),
            transportBudget = amountOf("transport", "교통", "교통비", fallbackValue = fallback.transportBudget),
            livingBudget = amountOf("living", "생활", "생활비", fallbackValue = fallback.livingBudget),
            hobbyBudget = amountOf("leisure", "hobby", "여가", "취미", "여가/취미", fallbackValue = fallback.hobbyBudget)
        )
    }
}
