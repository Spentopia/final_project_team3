package com.ict.spentopia.data.remote

import retrofit2.http.GET

data class WeeklyScoreResponse(
    val id: String?,
    val week_start: String?,
    val record_days_score: Int?,
    val receipt_score: Int?,
    val diary_score: Int?,
    val budget_score: Int?,
    val streak_score: Int?,
    val total_score: Int?,
    val reward_granted: Boolean?
)

interface RewardApi {
    @GET("/api/rewards/weekly-score/current")
    suspend fun getCurrentWeeklyScore(): WeeklyScoreResponse
}
