package com.ict.spentopia.feature.budget // 이 파일이 속한 패키지 위치를 적음

import android.app.Application // 앱 전체 정보 타입을 가져옴
import androidx.lifecycle.AndroidViewModel // AndroidViewModel 기능을 가져옴
import androidx.lifecycle.viewModelScope // viewModelScope 기능을 가져옴
import com.google.gson.Gson
import com.ict.spentopia.data.remote.AiBudgetPlan
import com.ict.spentopia.data.remote.BudgetCategoryItem // BudgetCategoryItem 기능을 가져옴
import com.ict.spentopia.data.remote.BudgetResponse // BudgetResponse 기능을 가져옴
import com.ict.spentopia.data.remote.CreateBudgetRequest // CreateBudgetRequest 기능을 가져옴
import com.ict.spentopia.data.remote.RetrofitClient // RetrofitClient 기능을 가져옴
import com.ict.spentopia.data.remote.UpdateBudgetCategoriesRequest // UpdateBudgetCategoriesRequest 기능을 가져옴
import com.ict.spentopia.data.remote.UpdateBudgetRequest // UpdateBudgetRequest 기능을 가져옴
import kotlinx.coroutines.flow.MutableStateFlow // 바뀌는 상태값 도구를 가져옴
import kotlinx.coroutines.flow.StateFlow // 읽기 전용 상태값 도구를 가져옴
import kotlinx.coroutines.flow.asStateFlow // asStateFlow 기능을 가져옴
import kotlinx.coroutines.launch // 코루틴 실행 도구를 가져옴
import retrofit2.HttpException // 서버 오류 타입을 가져옴
import java.util.Calendar // Calendar 기능을 가져옴

// 예산 설정 상태 관리 VM임
// 수정값/불러오기/저장완료 상태 맡음
class BudgetViewModel(application: Application) : AndroidViewModel(application) { // BudgetViewModel 기능을 묶어둔 클래스 시작

    // DataStore 인스턴스 생성
    // 예산 설정 저장용임
    private val budgetDataStore = BudgetDataStore(application) // 예산 관련 값을 저장함

    // 현재 화면 예산 상태
    private val _budgetState = MutableStateFlow(BudgetSettingsData()) // 화면에서 바뀔 예산 관련 값을 저장함
    val budgetState: StateFlow<BudgetSettingsData> = _budgetState.asStateFlow() // 화면에서 예산 관련 값을 읽을 수 있게 열어둠

    // 저장 완료 여부 상태
    private val _saveSuccess = MutableStateFlow(false) // 화면에서 바뀔 저장 성공 여부를 저장함
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow() // 화면에서 저장 성공 여부를 읽을 수 있게 열어둠

    private val _saveError = MutableStateFlow("") // 화면에서 바뀔 오류 내용을 저장함
    val saveError: StateFlow<String> = _saveError.asStateFlow() // 화면에서 오류 내용을 읽을 수 있게 열어둠

    private val _aiPlanList = MutableStateFlow<List<BudgetPlanUiData>>(emptyList()) // 화면에서 바뀔 AI 추천 플랜 목록을 저장함
    val aiPlanList: StateFlow<List<BudgetPlanUiData>> = _aiPlanList.asStateFlow() // 화면에서 AI 추천 플랜 목록을 읽을 수 있게 열어둠

    private val _selectedPlanId = MutableStateFlow<String?>(null)
    val selectedPlanId: StateFlow<String?> = _selectedPlanId.asStateFlow()

    private val _isAiPlanLoading = MutableStateFlow(false) // 화면에서 바뀔 로딩 상태를 저장함
    val isAiPlanLoading: StateFlow<Boolean> = _isAiPlanLoading.asStateFlow() // 화면에서 로딩 상태를 읽을 수 있게 열어둠

    private val _aiPlanError = MutableStateFlow("") // 화면에서 바뀔 오류 내용을 저장함
    val aiPlanError: StateFlow<String> = _aiPlanError.asStateFlow() // 화면에서 오류 내용을 읽을 수 있게 열어둠

    private val _isPaymentRequired = MutableStateFlow(false) // AI 추천 결제가 필요한지 저장함
    val isPaymentRequired: StateFlow<Boolean> = _isPaymentRequired.asStateFlow() // 화면에서 결제 팝업 표시 여부를 읽게 함

    private var currentBudgetId: String? = null // 나중에 바뀔 수 있는 예산 관련 값을 저장함
    private var lastAiPlanRequestSettings: BudgetSettingsData? = null // 나중에 바뀔 수 있는 마지막 AI 추천 요청값을 저장함
    private val gson = Gson()

    init { // 이 블록 안의 내용이 시작됨
        loadBudgetSettings() // 예산 설정을 불러옴
        syncCurrentMonthBudgetFromBackend() // 이번 달 예산을 서버에서 맞춰 가져옴
    }

    // 저장값 불러옴
    // DataStore flow 계속 구독함
    private fun loadBudgetSettings() { // 데이터를 불러오는 함수 시작
        viewModelScope.launch { // 화면이 멈추지 않게 코루틴으로 실행함
            // DataStore는 값이 바뀌면 Flow로 새 값을 다시 흘려줍니다.
            // 그래서 여기서는 계속 구독하면서 화면 상태를 최신으로 유지합니다.
            budgetDataStore.budgetSettingsFlow.collect { savedSettings ->
                _budgetState.value = savedSettings // 예산 관련 값을 정해줌
            }
        }
    }

    // 월 수입 변경
    fun updateMonthlyIncome(value: Long) { // 데이터를 수정하는 함수 시작
        _budgetState.value = _budgetState.value.toMonthlyOnlySettings().copy(monthlyIncome = value.coerceAtLeast(0L)) // 예산 관련 값을 정해줌
    }

    // 저축 목표 변경
    fun updateSavingGoal(value: Long) { // 데이터를 수정하는 함수 시작
        _budgetState.value = _budgetState.value.copy(savingGoal = value.coerceAtLeast(0L)) // 예산 관련 값을 정해줌
    }

    // 식비 변경
    fun updateFoodBudget(value: Long) { // 데이터를 수정하는 함수 시작
        _budgetState.value = _budgetState.value.copy(foodBudget = value.coerceAtLeast(0L)) // 예산 관련 값을 정해줌
    }

    // 교통비 변경
    fun updateTransportBudget(value: Long) { // 데이터를 수정하는 함수 시작
        _budgetState.value = _budgetState.value.copy(transportBudget = value.coerceAtLeast(0L)) // 예산 관련 값을 정해줌
    }

    // 생활비 변경
    fun updateLivingBudget(value: Long) { // 데이터를 수정하는 함수 시작
        _budgetState.value = _budgetState.value.copy(livingBudget = value.coerceAtLeast(0L)) // 예산 관련 값을 정해줌
    }

    // 여가/취미 변경
    fun updateHobbyBudget(value: Long) { // 데이터를 수정하는 함수 시작
        _budgetState.value = _budgetState.value.copy(hobbyBudget = value.coerceAtLeast(0L)) // 예산 관련 값을 정해줌
    }

    // 추천 플랜 적용
    fun applyPlan(plan: BudgetPlanUiData) { // applyPlan 함수를 선언함
        if (!canEditBudgetThisMonth()) { // 조건이 맞는지 확인함
            _saveError.value = "이번 달 예산 설정이 완료되었습니다. 예산 설정은 월 1회만 가능합니다."
            return
        }

        val nextSettings = _budgetState.value.copy( // 예산 관련 값을 정해줌
            monthlyIncome = plan.monthlyBudget, // 월 수입을 정해줌
            savingGoal = plan.savingGoal, // 저축 목표를 정해줌
            foodBudget = plan.food, // 식비 예산을 정해줌
            transportBudget = plan.transport, // 교통비 예산을 정해줌
            livingBudget = plan.living, // 생활비 예산을 정해줌
            hobbyBudget = plan.hobby // 취미 예산을 정해줌
        )
        _budgetState.value = nextSettings // 예산 관련 값을 정해줌

        viewModelScope.launch { // 화면이 멈추지 않게 코루틴으로 실행함
            try { // 오류가 날 수 있는 코드를 먼저 시도함
                upsertBackendBudget(nextSettings, lockBudget = true, selectedPlanId = plan.id) // 서버 예산도 같은 값으로 맞춘 뒤 확정함
                val lockedSettings = nextSettings.copy(lockedMonthKey = currentMonthKey())
                _budgetState.value = lockedSettings
                _selectedPlanId.value = plan.id
                budgetDataStore.saveBudgetSettings(lockedSettings) // 서버 확정 성공 후에만 로컬도 잠급니다.
                _saveError.value = "" // 오류 내용을 정해줌
                _saveSuccess.value = true // saveSuccess.value 값을 정해줌
            } catch (e: HttpException) { // 이 블록 안의 내용이 시작됨
                _saveError.value = when (e.code()) { // 오류 내용을 정해줌
                    401 -> "로그인이 만료되었습니다. 다시 로그인해주세요."
                    else -> "플랜 적용 저장에 실패했습니다. 잠시 후 다시 시도해주세요. (${e.code()})" // 위 조건이 아니면 이쪽을 실행함
                }
            } catch (e: Exception) { // 이 블록 안의 내용이 시작됨
                _saveError.value = "플랜 적용 저장에 실패했습니다. 잠시 후 다시 시도해주세요." // 오류 내용을 정해줌
            }
        }
    }

    // 현재 설정 저장
    // 슬라이더/추천 플랜 저장용
    fun saveBudgetSettings() { // 데이터를 저장하는 함수 시작
        if (!canEditBudgetThisMonth()) { // 조건이 맞는지 확인함
            _saveError.value = "이번 달 예산 설정이 완료되었습니다. 예산 설정은 월 1회만 가능합니다."
            return
        }

        viewModelScope.launch { // 화면이 멈추지 않게 코루틴으로 실행함
            val currentSettings = _budgetState.value.toMonthlyOnlySettings().copy(lockedMonthKey = "") // 임시 저장은 수정 가능 상태로 유지함
            _budgetState.value = currentSettings // 예산 관련 값을 정해줌
            // 먼저 로컬 DataStore에 저장해서 앱 재실행 후에도 값이 남게 합니다.
            budgetDataStore.saveBudgetSettings(currentSettings)
            //_ budgetState.value 지금 화면이나 ViewModeldl  들고 있는 예산 설정 값이고 그값을 budgetDataStore 에 저장함

            try { // 오류가 날 수 있는 코드를 먼저 시도함
                // 그 다음 서버 예산 API에도 같은 값을 동기화합니다.
                upsertBackendBudget(currentSettings, lockBudget = false) // upsert Backend Budget 함수를 실행함
                _saveError.value = "" // 오류 내용을 정해줌
                _saveSuccess.value = true // saveSuccess.value 값을 정해줌
            } catch (e: HttpException) { // 이 블록 안의 내용이 시작됨
                _saveError.value = when (e.code()) { // 오류 내용을 정해줌
                    401 -> "로그인이 만료되었습니다. 다시 로그인해주세요."
                    else -> "예산 저장에 실패했습니다. 잠시 후 다시 시도해주세요. (${e.code()})" // 위 조건이 아니면 이쪽을 실행함
                }
            } catch (e: Exception) { // 이 블록 안의 내용이 시작됨
                _saveError.value = "예산 저장에 실패했습니다. 잠시 후 다시 시도해주세요." // 오류 내용을 정해줌
            }
        }
    }

    fun confirmBudgetSettings() { // 현재 임시 저장된 예산을 확정하고 서버 잠금을 겁니다.
        if (!canEditBudgetThisMonth()) { // 조건이 맞는지 확인함
            _saveError.value = "이번 달 예산 설정이 완료되었습니다. 예산 설정은 월 1회만 가능합니다."
            return
        }

        viewModelScope.launch { // 화면이 멈추지 않게 코루틴으로 실행함
            val currentSettings = _budgetState.value.toMonthlyOnlySettings().copy(lockedMonthKey = "")
            try { // 오류가 날 수 있는 코드를 먼저 시도함
                upsertBackendBudget(currentSettings, lockBudget = true)
                val lockedSettings = currentSettings.copy(lockedMonthKey = currentMonthKey())
                _budgetState.value = lockedSettings
                budgetDataStore.saveBudgetSettings(lockedSettings)
                _saveError.value = ""
                _saveSuccess.value = true
            } catch (e: HttpException) { // 이 블록 안의 내용이 시작됨
                _saveError.value = when (e.code()) {
                    401 -> "로그인이 만료되었습니다. 다시 로그인해주세요."
                    else -> "예산 확정에 실패했습니다. 잠시 후 다시 시도해주세요. (${e.code()})"
                }
            } catch (e: Exception) { // 이 블록 안의 내용이 시작됨
                _saveError.value = "예산 확정에 실패했습니다. 잠시 후 다시 시도해주세요."
            }
        }
    }

    // 저장 완료 상태 초기화
    fun resetSaveSuccess() { // 데이터를 저장하는 함수 시작
        _saveSuccess.value = false // saveSuccess.value 값을 정해줌
    }

    fun resetSaveError() { // 데이터를 저장하는 함수 시작
        _saveError.value = "" // 오류 내용을 정해줌
    }

    fun requestAiRecommendedPlans() { // requestAiRecommendedPlans 함수를 선언함
        if (_isAiPlanLoading.value) return // 조건이 맞는지 확인함
        if (!canEditBudgetThisMonth()) { // 조건이 맞는지 확인함
            _aiPlanError.value = "이번 달 예산 설정이 완료되었습니다. 예산 설정은 월 1회만 가능합니다."
            return
        }

        viewModelScope.launch { // 화면이 멈추지 않게 코루틴으로 실행함
            // AI 추천은 현재 화면 입력값을 임시 저장한 뒤 서버에 저장된 예산을 다시 조회해서 사용합니다.
            _isAiPlanLoading.value = true // 로딩 상태를 정해줌
            _aiPlanError.value = "" // 오류 내용을 정해줌
            _isPaymentRequired.value = false // 이전 결제 팝업 상태를 지움

            try { // 오류가 날 수 있는 코드를 먼저 시도함
                val currentSettings = _budgetState.value.toMonthlyOnlySettings().copy(lockedMonthKey = "")
                upsertBackendBudget(currentSettings, lockBudget = false)

                val (year, month) = currentYearMonth()
                val savedBudget = RetrofitClient.budgetApi.getBudget(year = year, month = month)
                currentBudgetId = savedBudget.id

                val requestSettings = savedBudget.toBudgetSettingsData(_budgetState.value)
                if (requestSettings.lockedMonthKey == currentMonthKey()) {
                    _aiPlanError.value = "이번 달 예산 설정이 완료되었습니다. 예산 설정은 월 1회만 가능합니다."
                    return@launch
                }

                if (_aiPlanList.value.isNotEmpty() && lastAiPlanRequestSettings == requestSettings) { // 조건이 맞는지 확인함
                    return@launch
                }

                _budgetState.value = requestSettings
                budgetDataStore.saveBudgetSettings(requestSettings)

                val budgetId = savedBudget.id
                val response = RetrofitClient.budgetApi.generateAiPlan(budgetId) // 서버 응답을 저장함
                _aiPlanList.value = response.plans.toBudgetPlanUiDataList()
                _selectedPlanId.value = null
                lastAiPlanRequestSettings = requestSettings // requestSettings 값을 lastAiPlanRequestSettings 값에 넣음
                _aiPlanError.value = if (response.plans.isEmpty()) { // 오류 내용을 정해줌
                    "AI 추천 플랜이 비어 있습니다."
                } else { // 이 블록 안의 내용이 시작됨
                    ""
                }
            } catch (e: HttpException) { // 이 블록 안의 내용이 시작됨
                if (e.code() == 402) {
                    _isPaymentRequired.value = true
                    _aiPlanError.value = ""
                } else {
                    _aiPlanError.value = when (e.code()) { // 오류 내용을 정해줌
                        401 -> "로그인이 만료되었습니다. 다시 로그인해주세요."
                        404 -> "예산 정보를 찾지 못했습니다. 설정 저장 후 다시 시도해주세요."
                        500, 502 -> "AI 추천 플랜을 불러오지 못했습니다. 잠시 후 다시 시도해주세요."
                        else -> "AI 추천 플랜 요청에 실패했습니다. (${e.code()})" // 위 조건이 아니면 이쪽을 실행함
                    }
                }
            } catch (e: Exception) { // 이 블록 안의 내용이 시작됨
                _aiPlanError.value = "AI 추천 플랜 요청에 실패했습니다. 잠시 후 다시 시도해주세요." // 오류 내용을 정해줌
            } finally { // 이 블록 안의 내용이 시작됨
                _isAiPlanLoading.value = false // 로딩 상태를 정해줌
            }
        }
    }

    fun dismissPaymentDialog() { // 결제 팝업을 닫는 함수임
        _isPaymentRequired.value = false
    }

    fun showMobilePaymentNotReadyMessage() { // 결제 버튼을 눌렀을 때 현재 상태를 알려줌
        _aiPlanError.value = "모바일 결제 트랜잭션 생성 로직을 연결해야 합니다. 지갑은 연결되어 있으나 결제 전송은 아직 실행하지 않았습니다."
    }

    private fun syncCurrentMonthBudgetFromBackend() { // syncCurrentMonthBudgetFromBackend 함수를 선언함
        viewModelScope.launch { // 화면이 멈추지 않게 코루틴으로 실행함
            try { // 오류가 날 수 있는 코드를 먼저 시도함
                // 현재 연/월의 서버 예산을 먼저 읽어와서
                // 앱 첫 화면이 서버 상태와 비슷하게 시작되도록 맞춥니다.
                val (year, month) = currentYearMonth() // month 값을 정해줌
                val response = RetrofitClient.budgetApi.getBudget(year = year, month = month) // 서버 응답을 저장함
                currentBudgetId = response.id // 예산 관련 값을 정해줌

                val syncedSettings = response.toBudgetSettingsData(_budgetState.value) // syncedSettings 값을 저장함
                _budgetState.value = syncedSettings // 예산 관련 값을 정해줌
                _aiPlanList.value = parseStoredAiPlans(response.ai_plan)
                _selectedPlanId.value = parseStoredSelectedPlanId(response.ai_plan)
                budgetDataStore.saveBudgetSettings(syncedSettings)
            } catch (e: HttpException) { // 이 블록 안의 내용이 시작됨
                if (e.code() != 404 && e.code() != 401) { // 조건이 맞는지 확인함
                    _saveError.value = "예산 정보를 불러오지 못했습니다. (${e.code()})" // 오류 내용을 정해줌
                }
            } catch (_: Exception) { // 이 블록 안의 내용이 시작됨
                // 예산 화면은 로컬 설정으로도 동작해야 하므로 초기 조회 실패는 조용히 넘깁니다.
            }
        }
    }

    private suspend fun upsertBackendBudget(
        settings: BudgetSettingsData,
        lockBudget: Boolean,
        selectedPlanId: String? = null
    ): String { // upsertBackendBudget 함수를 선언함
        val (year, month) = currentYearMonth() // month 값을 정해줌
        // 현재 월 예산이 아직 없으면 생성하고, 있으면 그 값을 수정합니다.
        val budgetId = currentBudgetId ?: findOrCreateBudget(year, month, settings).id.also { // 예산 관련 값을 저장함
            currentBudgetId = it // it 값을 예산 관련 값에 넣음
        }

        RetrofitClient.budgetApi.updateCategories( // 서버 통신 도구를 설정함
            budgetId = budgetId, // 예산 관련 값을 예산 관련 값에 넣음
            request = UpdateBudgetCategoriesRequest( // 서버 요청값을 정해줌
                categories = settings.toBudgetCategoryItems() // categories 값을 정해줌
            )
        )

        RetrofitClient.budgetApi.updateBudget( // 서버 통신 도구를 설정함
            budgetId = budgetId, // 예산 관련 값을 예산 관련 값에 넣음
            request = UpdateBudgetRequest( // 서버 요청값을 정해줌
                total_budget = settings.monthlyIncome, // 예산 관련 값을 정해줌
                savings_goal = settings.savingGoal, // savings_goal 값을 정해줌
                selected_plan_id = selectedPlanId,

                lock_budget = lockBudget // 확정 저장이면 서버에서 잠금을 겁니다.
            )
        )

        return budgetId // 이 값을 함수 결과로 돌려줌
    }

    private suspend fun findOrCreateBudget( // 데이터를 저장하는 함수 시작
        year: Int, // year 값을 받음
        month: Int, // month 값을 받음
        settings: BudgetSettingsData // settings 값을 받음
    ): BudgetResponse { // 이 블록 안의 내용이 시작됨
        return try { // 이 값을 함수 결과로 돌려줌
            RetrofitClient.budgetApi.getBudget(year = year, month = month) // 서버 통신 도구를 설정함
        } catch (e: HttpException) { // 이 블록 안의 내용이 시작됨
            if (e.code() != 404) throw e // 조건이 맞는지 확인함

            RetrofitClient.budgetApi.createBudget( // 서버 통신 도구를 설정함
                CreateBudgetRequest( // 데이터를 저장하는 함수를 실행함
                    year = year, // year 값을 year 값에 넣음
                    month = month, // month 값을 month 값에 넣음
                    total_budget = settings.monthlyIncome, // 예산 관련 값을 정해줌
                    savings_goal = settings.savingGoal // savings_goal 값을 정해줌
                )
            )
        }
    }

    private fun currentYearMonth(): Pair<Int, Int> { // currentYearMonth 함수를 선언함
        val calendar = Calendar.getInstance() // calendar 값을 저장함
        return calendar.get(Calendar.YEAR) to calendar.get(Calendar.MONTH) + 1 // 이 값을 함수 결과로 돌려줌
    }

    private fun currentMonthKey(): String { // currentMonthKey 함수를 선언함
        val (year, month) = currentYearMonth() // month 값을 정해줌
        return "%04d-%02d".format(year, month) // 이 값을 함수 결과로 돌려줌
    }

    private fun canEditBudgetThisMonth(): Boolean { // canEditBudgetThisMonth 함수를 선언함
        return _budgetState.value.lockedMonthKey != currentMonthKey() // 이 값을 함수 결과로 돌려줌
    }

    private fun BudgetSettingsData.toMonthlyOnlySettings(): BudgetSettingsData {
        return copy(
            savingGoal = 0L,
            foodBudget = 0L,
            transportBudget = 0L,
            livingBudget = 0L,
            hobbyBudget = 0L
        )
    }

    private fun BudgetSettingsData.toBudgetCategoryItems(): List<BudgetCategoryItem> { // BudgetSettingsData 함수를 선언함
        return listOf( // 이 값을 함수 결과로 돌려줌
            BudgetCategoryItem(category = "food", allocated_amount = foodBudget), // 예산 관련 값을 정해줌
            BudgetCategoryItem(category = "transport", allocated_amount = transportBudget), // 예산 관련 값을 정해줌
            BudgetCategoryItem(category = "living", allocated_amount = livingBudget), // 예산 관련 값을 정해줌
            BudgetCategoryItem(category = "leisure", allocated_amount = hobbyBudget) // 예산 관련 값을 정해줌
        )
    }

    private fun BudgetResponse.toBudgetSettingsData(fallback: BudgetSettingsData): BudgetSettingsData { // BudgetResponse 함수를 선언함
        fun amountOf(vararg names: String, fallbackValue: Long): Long { // amountOf 함수를 선언함
            return categories // 이 값을 함수 결과로 돌려줌
                .firstOrNull { item ->
                    names.any { name -> item.category.equals(name, ignoreCase = true) } // ignoreCase 값을 정해줌
                }
                ?.allocated_amount
                ?: fallbackValue
        }

        return BudgetSettingsData( // 이 값을 함수 결과로 돌려줌
            monthlyIncome = total_budget, // 예산 관련 값을 월 수입에 넣음
            savingGoal = savings_goal ?: fallback.savingGoal, // 저축 목표를 정해줌
            foodBudget = amountOf("food", "식비", fallbackValue = fallback.foodBudget), // 식비 예산을 정해줌
            transportBudget = amountOf("transport", "교통", "교통비", fallbackValue = fallback.transportBudget), // 교통비 예산을 정해줌
            livingBudget = amountOf("living", "생활", "생활비", fallbackValue = fallback.livingBudget), // 생활비 예산을 정해줌
            hobbyBudget = amountOf("leisure", "hobby", "여가", "취미", "여가/취미", fallbackValue = fallback.hobbyBudget), // 취미 예산을 정해줌
            lockedMonthKey = if (locked_at != null) currentMonthKey() else fallback.lockedMonthKey // 서버 잠금 상태를 반영함
        )
    }

    private data class StoredAiPlanPayload(
        val plans: List<AiBudgetPlan>? = null,
        val selected_plan_id: String? = null
    )

    private fun parseStoredAiPlans(storedPlan: String?): List<BudgetPlanUiData> {
        if (storedPlan.isNullOrBlank()) return emptyList()
        return try {
            gson.fromJson(storedPlan, StoredAiPlanPayload::class.java)
                ?.plans
                ?.toBudgetPlanUiDataList()
                .orEmpty()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseStoredSelectedPlanId(storedPlan: String?): String? {
        if (storedPlan.isNullOrBlank()) return null
        return try {
            gson.fromJson(storedPlan, StoredAiPlanPayload::class.java)?.selected_plan_id
        } catch (_: Exception) {
            null
        }
    }

    private fun List<AiBudgetPlan>.toBudgetPlanUiDataList(): List<BudgetPlanUiData> {
        val labels = listOf("기본 플랜", "중간 플랜", "여유 플랜")
        return mapIndexed { index, plan ->
            val label = labels.getOrNull(index) ?: plan.name
            BudgetPlanUiData(
                id = label,
                title = label,
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
    }
}
