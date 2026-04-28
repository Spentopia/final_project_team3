import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// =====================================================
// local.properties 읽기
// -----------------------------------------------------
// android/local.properties 파일에 아래처럼 저장해둔 값을 읽어옴
//
// GOOGLE_WEB_CLIENT_ID=your-web-client-id.apps.googleusercontent.com
//
// 이렇게 하면 LoginScreen.kt에 Web Client ID를 직접 하드코딩 안해도됨
// =====================================================
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        load(file.inputStream())
    }
}

// local.properties에서 GOOGLE_WEB_CLIENT_ID 값을 가져옴
// 값이 없으면 빈 문자열("")이 들어갑니다.
val googleWebClientId = localProperties.getProperty("GOOGLE_WEB_CLIENT_ID") ?: ""

android {
    namespace = "com.ict.spentopia"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ict.spentopia"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testApplicationId = "com.ict.spentopia.test"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // =====================================================
        // BuildConfig에 Google Web Client ID 등록
        // -----------------------------------------------------
        // Kotlin 코드에서는 아래처럼 사용할 수 있음
        //
        // BuildConfig.GOOGLE_WEB_CLIENT_ID
        //
        // 실제 값은 local.properties의 GOOGLE_WEB_CLIENT_ID에서 가져옴
        // =====================================================
        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            "\"$googleWebClientId\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        // Jetpack Compose 사용
        compose = true

        // BuildConfig.GOOGLE_WEB_CLIENT_ID를 사용하려면 true 필요
        buildConfig = true
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // =========================
    // Android 기본 라이브러리
    // =========================
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // =========================
    // Jetpack Compose
    // =========================
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // =========================
    // Lifecycle / 상태 관리
    // =========================
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")

    // =========================
    // 로컬 데이터 저장
    // =========================
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // =========================
    // Room 데이터베이스
    // =========================
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // =========================
    // Supabase
    // =========================
    implementation(platform("io.github.jan-tennert.supabase:bom:3.0.0"))
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.github.jan-tennert.supabase:postgrest-kt")

    // =========================
    // 이미지 로딩
    // =========================
    implementation("io.coil-kt:coil-compose:2.6.0")

    // =========================
    // Solana / Wallet 연동
    // =========================
    implementation("com.solanamobile:mobile-wallet-adapter-clientlib-ktx:2.0.3")

    // =========================
    // Coroutine
    // =========================
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // =========================
    // Retrofit / OkHttp
    // =========================
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // =========================
    // Ktor
    // =========================
    // 기존에 2.3.10과 3.0.3이 같이 있어서 충돌 가능성이 있음.
    // 삭제하지 않고 주석 처리만 해둠.
    // implementation("io.ktor:ktor-client-okhttp:2.3.10")

    implementation("io.ktor:ktor-client-core:3.0.3")
    implementation("io.ktor:ktor-client-okhttp:3.0.3")
    implementation("io.ktor:ktor-client-logging:3.0.3")

    // =========================
    // Bitcoin
    // =========================
    implementation("org.bitcoinj:bitcoinj-core:0.16.3")

    // =========================
    // Kakao Login SDK
    // =========================
    implementation("com.kakao.sdk:v2-user:2.20.1")

    // =========================
    // Google Login
    // =========================
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // =========================
    // 테스트
    // =========================
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
