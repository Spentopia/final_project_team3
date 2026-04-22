
package com.ict.spentopia.data.local
//Context import 입니다.

//DataStore import입니다.
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
//앱 전역에서 사용할 wallet DataStore 이름입니다.
val Context.walletDataStore by preferencesDataStore(name = "wallet_prefs")