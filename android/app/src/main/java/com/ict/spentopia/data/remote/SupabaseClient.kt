

package com.ict.spentopia.data.remote

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth

object SupabaseClient {

    val client = createSupabaseClient(
        supabaseUrl = "https://gapdntsijwgoucxhnojq.supabase.co",
        supabaseKey = "sb_publishable_gO8kXf1S7fQcgNhgouMkRw_osZfwy0e"
    ) {
        install(Auth) {
            autoLoadFromStorage = false
            autoSaveToStorage = false
            alwaysAutoRefresh = false
        }
    }
}
