1plugins { // 이 블록 안의 내용이 시작됨
    alias(libs.plugins.android.application) apply false // alias 함수를 실행함
    alias(libs.plugins.kotlin.android) apply false // alias 함수를 실행함
    alias(libs.plugins.kotlin.compose) apply false // alias 함수를 실행함
    alias(libs.plugins.ksp) apply false // alias 함수를 실행함
}