package com.ict.spentopia.data.remote // 이 파일이 속한 패키지 위치를 적음

import retrofit2.http.GET // GET API 표시를 가져옴

data class WeeklyScoreResponse( // WeeklyScoreResponse 데이터를 묶어둘 클래스 시작
    val id: String?, // 아이디를 저장함
    val week_start: String?, // week_start 값을 저장함
    val record_days_score: Int?, // record_days_score 값을 저장함
    val receipt_score: Int?, // receipt_score 값을 저장함
    val diary_score: Int?, // diary_score 값을 저장함
    val budget_score: Int?, // 예산 관련 값을 저장함
    val streak_score: Int?, // streak_score 값을 저장함
    val total_score: Int?, // total_score 값을 저장함
    val reward_granted: Boolean? // reward_granted 값을 저장함
)

interface RewardApi { // RewardApi에서 꼭 만들어야 할 함수 규칙을 정함
    @GET("/api/rewards/weekly-score/current") // 서버에서 데이터를 가져오는 API 주소를 적음
    suspend fun getCurrentWeeklyScore(): WeeklyScoreResponse // 데이터를 불러오는 함수 시작
}
