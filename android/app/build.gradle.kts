plugins {
    alias(libs.plugins.android.application) // Android 앱 플러그인
    alias(libs.plugins.kotlin.android)      // Kotlin Android 지원
    alias(libs.plugins.kotlin.compose)      // Jetpack Compose 지원
    alias(libs.plugins.ksp)                 // Kotlin Symbol Processing (Room 컴파일러용)
}

android {
    namespace = "com.ict.spentopia"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ict.spentopia"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false // 난독화/최적화 비활성화
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17 // Java 17 사용
        targetCompatibility = JavaVersion.VERSION_17 // Java 17 타겟
    }

    kotlinOptions {
        jvmTarget = "17" // Kotlin JVM 타겟 17
    }

    buildFeatures {
        compose = true // Jetpack Compose 활성화
    }
}

kotlin {
    jvmToolchain(17) // Kotlin 툴체인 17 사용
}

dependencies {
    // =========================
    // Android 기본 라이브러리
    // =========================
    implementation(libs.androidx.core.ktx)              // Android KTX 확장 함수
    implementation(libs.androidx.lifecycle.runtime.ktx) // Lifecycle 런타임 KTX
    implementation(libs.androidx.activity.compose)      // Activity + Compose 연동

    // =========================
    // Jetpack Compose
    // =========================
    implementation(platform(libs.androidx.compose.bom)) // Compose BOM 버전 통일
    implementation(libs.androidx.compose.ui)            // Compose UI 핵심
    implementation(libs.androidx.compose.ui.graphics)   // Compose 그래픽 처리
    implementation(libs.androidx.compose.ui.tooling.preview) // 미리보기 지원
    implementation(libs.androidx.compose.material3)     // Material 3 UI 컴포넌트
    implementation("androidx.compose.material:material-icons-extended") // 확장 머티리얼 아이콘
    implementation("androidx.navigation:navigation-compose:2.7.7")      // Compose 네비게이션

    // =========================
    // Lifecycle / 상태 관리
    // =========================
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3") // Compose에서 Lifecycle 연동

    // =========================
    // 로컬 데이터 저장
    // =========================
    implementation("androidx.datastore:datastore-preferences:1.1.1") // 키-값 기반 설정 저장(DataStore)

    // =========================
    // Room 데이터베이스
    // =========================
    implementation("androidx.room:room-runtime:2.6.1") // Room 런타임
    implementation("androidx.room:room-ktx:2.6.1")     // Room 코루틴/KTX 지원
    ksp("androidx.room:room-compiler:2.6.1")           // Room 어노테이션 처리기

    // =========================
    // Supabase
    // =========================
    implementation(platform("io.github.jan-tennert.supabase:bom:3.0.0")) // Supabase BOM 버전 통일
    implementation("io.github.jan-tennert.supabase:auth-kt")              // 인증(Auth)
    implementation("io.github.jan-tennert.supabase:postgrest-kt")         // DB/PostgREST 통신

    // =========================
    // 이미지 로딩
    // =========================
    implementation("io.coil-kt:coil-compose:2.6.0") // Compose용 이미지 로딩 라이브러리

    // =========================
    // Solana / Wallet 연동
    // =========================
    implementation("com.solanamobile:mobile-wallet-adapter-clientlib-ktx:2.0.3")
    // Solana 모바일 월렛 어댑터 클라이언트 라이브러리

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    // 비동기 처리용 Kotlin Coroutines

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    // REST API 통신용 Retrofit

    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    // Retrofit JSON 변환기 (Gson)

    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    // 네트워크 요청/응답 로그 확인

    implementation("org.bitcoinj:bitcoinj-core:0.16.3")
    // 비트코인 관련 기능 처리 라이브러리

    // =========================
    // 테스트
    // =========================
    testImplementation(libs.junit)                        // 단위 테스트
    androidTestImplementation(libs.androidx.junit)       // AndroidX JUnit 테스트
    androidTestImplementation(libs.androidx.espresso.core) // UI 테스트(Espresso)

    androidTestImplementation(platform(libs.androidx.compose.bom)) // Compose 테스트 BOM
    androidTestImplementation(libs.androidx.compose.ui.test.junit4) // Compose UI 테스트

    debugImplementation(libs.androidx.compose.ui.tooling)      // 디버그용 Compose 툴링
    debugImplementation(libs.androidx.compose.ui.test.manifest) // 테스트 매니페스트 지원
}