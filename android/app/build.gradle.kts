plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
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
        compose = true
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