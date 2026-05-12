package com.ict.spentopia.data.local // 이 파일이 속한 패키지 위치를 적음
// 지갑 정보 저장용 DataStore 확장임
// SharedPreferences보다 타입 안정성 높음

import android.content.Context // 현재 화면 정보 타입을 가져옴
import androidx.datastore.preferences.preferencesDataStore // preferencesDataStore 기능을 가져옴

// 앱 전역 wallet DataStore 이름
val Context.walletDataStore by preferencesDataStore(name = "wallet_prefs") // Context 값을 저장함
