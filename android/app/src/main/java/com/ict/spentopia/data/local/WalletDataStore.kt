package com.ict.spentopia.data.local
// 지갑 정보 저장용 DataStore 확장임
// SharedPreferences보다 타입 안정성 높음

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

// 앱 전역 wallet DataStore 이름
val Context.walletDataStore by preferencesDataStore(name = "wallet_prefs")
