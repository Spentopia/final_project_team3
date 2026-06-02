package com.ict.spentopia.data.remote // 이 파일이 속한 패키지 위치를 적음

import retrofit2.http.Body // 서버로 보낼 값을 표시하는 도구를 가져옴
import retrofit2.http.GET // GET API 표시를 가져옴
import retrofit2.http.PATCH // PATCH 기능을 가져옴
import retrofit2.http.POST // POST API 표시를 가져옴
import retrofit2.http.Path // 주소 중간에 들어갈 값 표시를 가져옴
import retrofit2.http.Query // 주소 뒤에 붙는 요청값 표시를 가져옴

data class CreateBudgetRequest( // CreateBudgetRequest 데이터를 묶어둘 클래스 시작
    val year: Int, // year 값을 저장함
    val month: Int, // month 값을 저장함
    val total_budget: Long, // 예산 관련 값을 저장함
    val savings_goal: Long? // savings_goal 값을 저장함
)

data class UpdateBudgetRequest( // UpdateBudgetRequest 데이터를 묶어둘 클래스 시작
    val total_budget: Long?, // 예산 관련 값을 저장함
    val savings_goal: Long?, // savings_goal 값을 저장함
    val lock_budget: Boolean = false // 확정 저장이면 서버에서 예산을 잠급니다.
)

data class BudgetCategoryItem( // BudgetCategoryItem 데이터를 묶어둘 클래스 시작
    val category: String, // 카테고리을 저장함
    val allocated_amount: Long // allocated_amount 값을 저장함
)

data class UpdateBudgetCategoriesRequest( // UpdateBudgetCategoriesRequest 데이터를 묶어둘 클래스 시작
    val categories: List<BudgetCategoryItem> // categories 값을 저장함
)

data class BudgetResponse( // BudgetResponse 데이터를 묶어둘 클래스 시작
    val id: String, // 아이디를 저장함
    val year: Int, // year 값을 저장함
    val month: Int, // month 값을 저장함
    val total_budget: Long, // 예산 관련 값을 저장함
    val savings_goal: Long?, // savings_goal 값을 저장함
    val ai_plan: String?, // ai_plan 값을 저장함
    val categories: List<BudgetCategoryItem>, // categories 값을 저장함
    val locked_at: String?, // 서버에서 확정된 예산이면 잠금 시간이 들어옵니다.
    val created_at: String? // created_at 값을 저장함
)

data class AiBudgetPlan( // AiBudgetPlan 데이터를 묶어둘 클래스 시작
    val name: String, // name 값을 저장함
    val budget: Long, // 예산 관련 값을 저장함
    val savings: Long, // savings 값을 저장함
    val food: Long, // food 값을 저장함
    val transport: Long, // transport 값을 저장함
    val living: Long, // living 값을 저장함
    val leisure: Long, // leisure 값을 저장함
    val description: String // description 값을 저장함
)

data class AiPlanResponse( // AiPlanResponse 데이터를 묶어둘 클래스 시작
    val plans: List<AiBudgetPlan> // plans 값을 저장함
)

interface BudgetApi { // BudgetApi에서 꼭 만들어야 할 함수 규칙을 정함
    @GET("/api/budget") // 서버에서 데이터를 가져오는 API 주소를 적음
    suspend fun getBudget( // 데이터를 불러오는 함수 시작
        @Query("year") year: Int, // 이 값을 주소 뒤 요청값으로 보낸다는 표시
        @Query("month") month: Int // 이 값을 주소 뒤 요청값으로 보낸다는 표시
    ): BudgetResponse

    @POST("/api/budget") // 서버에 데이터를 보내는 API 주소를 적음
    suspend fun createBudget( // 데이터를 저장하는 함수 시작
        @Body request: CreateBudgetRequest // 이 값을 서버 요청 본문에 넣는다는 표시
    ): BudgetResponse

    @PATCH("/api/budget/{id}") // 서버 데이터 일부를 수정하는 API 주소를 적음
    suspend fun updateBudget( // 데이터를 수정하는 함수 시작
        @Path("id") budgetId: String, // 이 값을 API 주소 중간에 넣는다는 표시
        @Body request: UpdateBudgetRequest // 이 값을 서버 요청 본문에 넣는다는 표시
    ): BudgetResponse

    @PATCH("/api/budget/{id}/categories") // 서버 데이터 일부를 수정하는 API 주소를 적음
    suspend fun updateCategories( // 데이터를 수정하는 함수 시작
        @Path("id") budgetId: String, // 이 값을 API 주소 중간에 넣는다는 표시
        @Body request: UpdateBudgetCategoriesRequest // 이 값을 서버 요청 본문에 넣는다는 표시
    ): List<BudgetCategoryItem>

    @POST("/api/budget/{id}/ai-plan") // 서버에 데이터를 보내는 API 주소를 적음
    suspend fun generateAiPlan( // generateAiPlan 함수를 선언함
        @Path("id") budgetId: String // 이 값을 API 주소 중간에 넣는다는 표시
    ): AiPlanResponse
}
