package com.ict.spentopia.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

data class CreateBudgetRequest(
    val year: Int,
    val month: Int,
    val total_budget: Int,
    val savings_goal: Int?
)

data class UpdateBudgetRequest(
    val total_budget: Int?,
    val savings_goal: Int?
)

data class BudgetCategoryItem(
    val category: String,
    val allocated_amount: Int
)

data class UpdateBudgetCategoriesRequest(
    val categories: List<BudgetCategoryItem>
)

data class BudgetResponse(
    val id: String,
    val year: Int,
    val month: Int,
    val total_budget: Int,
    val savings_goal: Int?,
    val ai_plan: String?,
    val categories: List<BudgetCategoryItem>,
    val created_at: String?
)

data class AiBudgetPlan(
    val name: String,
    val budget: Int,
    val savings: Int,
    val food: Int,
    val transport: Int,
    val living: Int,
    val leisure: Int,
    val description: String
)

data class AiPlanResponse(
    val plans: List<AiBudgetPlan>
)

interface BudgetApi {
    @GET("/api/budget")
    suspend fun getBudget(
        @Query("year") year: Int,
        @Query("month") month: Int
    ): BudgetResponse

    @POST("/api/budget")
    suspend fun createBudget(
        @Body request: CreateBudgetRequest
    ): BudgetResponse

    @PATCH("/api/budget/{id}")
    suspend fun updateBudget(
        @Path("id") budgetId: String,
        @Body request: UpdateBudgetRequest
    ): BudgetResponse

    @PATCH("/api/budget/{id}/categories")
    suspend fun updateCategories(
        @Path("id") budgetId: String,
        @Body request: UpdateBudgetCategoriesRequest
    ): List<BudgetCategoryItem>

    @POST("/api/budget/{id}/ai-plan")
    suspend fun generateAiPlan(
        @Path("id") budgetId: String
    ): AiPlanResponse
}
