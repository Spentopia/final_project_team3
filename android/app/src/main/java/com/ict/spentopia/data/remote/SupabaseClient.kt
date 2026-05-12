

package com.ict.spentopia.data.remote // 이 파일이 속한 패키지 위치를 적음

import io.github.jan.supabase.createSupabaseClient // createSupabaseClient 기능을 가져옴
import io.github.jan.supabase.auth.Auth // Auth 기능을 가져옴

object SupabaseClient { // SupabaseClient를 앱에서 하나만 쓰게 만듦

    val client = createSupabaseClient( // 통신 클라이언트를 저장함
        supabaseUrl = "https://gapdntsijwgoucxhnojq.supabase.co", // supabaseUrl 값을 정해줌
        supabaseKey = "sb_publishable_gO8kXf1S7fQcgNhgouMkRw_osZfwy0e" // supabaseKey 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        install(Auth) { // 이 블록 안의 내용이 시작됨
            autoLoadFromStorage = false // false 값을 autoLoadFromStorage 값에 넣음
            autoSaveToStorage = false // false 값을 autoSaveToStorage 값에 넣음
            alwaysAutoRefresh = false // false 값을 alwaysAutoRefresh 값에 넣음
        }
    }
}
