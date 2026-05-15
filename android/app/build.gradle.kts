import java.util.Properties // Properties 기능을 가져옴

plugins { // 이 블록 안의 내용이 시작됨
    alias(libs.plugins.android.application) // alias 함수를 실행함
    alias(libs.plugins.kotlin.android) // alias 함수를 실행함
    alias(libs.plugins.kotlin.compose) // alias 함수를 실행함
    alias(libs.plugins.ksp) // alias 함수를 실행함
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
val localProperties = Properties().apply { // localProperties 값을 저장함
    val file = rootProject.file("local.properties") // 파일을 저장함
    if (file.exists()) { // 조건이 맞는지 확인함
        load(file.inputStream()) // 데이터를 불러오는 함수를 실행함
    }
}

// local.properties에서 GOOGLE_WEB_CLIENT_ID 값을 가져옴
// 값이 없으면 빈 문자열("")이 들어갑니다.
val googleWebClientId = localProperties.getProperty("GOOGLE_WEB_CLIENT_ID") ?: "" // googleWebClientId 값을 저장함

// local.properties에서 백엔드 API 주소를 가져옵니다.
// 값이 없으면 안드로이드 에뮬레이터에서 로컬 PC 백엔드에 접근하는 기본 주소를 사용
val apiBaseUrl = localProperties.getProperty("API_BASE_URL") ?: "http://10.0.2.2:1113/" // apiBaseUrl 값을 저장함

// Android에서 AI 분석 서버를 직접 호출할 때 사용할 주소입니다.
// 웹 프론트의 http://localhost:8000/api/v1/analyze/report 와 같은 서버
val aiAnalyzeBaseUrl = localProperties.getProperty("AI_ANALYZE_BASE_URL") ?: "http://10.0.2.2:8000/" // aiAnalyzeBaseUrl 값을 저장함

// Android WebView에서 열 NFT 마켓 웹 URL
// 로컬 에뮬레이터에서는 localhost 대신 10.0.2.2를 사용
val nftMarketWebViewUrl = // 마켓 관련 값을 저장함
    localProperties.getProperty("NFT_MARKET_WEBVIEW_URL") ?: "http://10.0.2.2:5173/marketplace"

// =====================================================
// 릴리즈 서명 정보
// -----------------------------------------------------
// 실제 배포용 키 값은 git에 올리지 말고 android/local.properties에만 둡니다.
//
// RELEASE_STORE_FILE=../keystore/spentopia-release.jks
// RELEASE_STORE_PASSWORD=your-store-password
// RELEASE_KEY_ALIAS=spentopia
// RELEASE_KEY_PASSWORD=your-key-password
// =====================================================
val releaseStoreFile = localProperties.getProperty("RELEASE_STORE_FILE") ?: "" // releaseStoreFile 값을 저장함
val releaseStorePassword = localProperties.getProperty("RELEASE_STORE_PASSWORD") ?: "" // releaseStorePassword 값을 저장함
val releaseKeyAlias = localProperties.getProperty("RELEASE_KEY_ALIAS") ?: "" // releaseKeyAlias 값을 저장함
val releaseKeyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD") ?: "" // releaseKeyPassword 값을 저장함
val hasReleaseSigningConfig = // 릴리즈 서명 정보가 모두 있는지 확인함
    releaseStoreFile.isNotBlank() &&
        releaseStorePassword.isNotBlank() &&
        releaseKeyAlias.isNotBlank() &&
        releaseKeyPassword.isNotBlank()

android { // 이 블록 안의 내용이 시작됨
    namespace = "com.ict.spentopia" // namespace 값을 정해줌
    compileSdk = 36 // compileSdk 값을 정해줌

    signingConfigs { // signingConfigs 값을 정해줌
        if (hasReleaseSigningConfig) { // 릴리즈 서명 정보가 있을 때만 설정함
            create("release") { // release 서명 설정을 만듦
                storeFile = rootProject.file(releaseStoreFile) // storeFile 값을 정해줌
                storePassword = releaseStorePassword // storePassword 값을 정해줌
                keyAlias = releaseKeyAlias // keyAlias 값을 정해줌
                keyPassword = releaseKeyPassword // keyPassword 값을 정해줌
            }
        }
    }

    defaultConfig { // 이 블록 안의 내용이 시작됨
        applicationId = "com.ict.spentopia" // applicationId 값을 정해줌
        minSdk = 26 // minSdk 값을 정해줌
        targetSdk = 35 // targetSdk 값을 정해줌
        versionCode = 1 // versionCode 값을 정해줌
        versionName = "1.0" // versionName 값을 정해줌

        testApplicationId = "com.ict.spentopia.test" // testApplicationId 값을 정해줌
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" // testInstrumentationRunner 값을 정해줌

        // =====================================================
        // BuildConfig에 Google Web Client ID 등록
        // -----------------------------------------------------
        // Kotlin 코드에서는 아래처럼 사용할 수 있음
        //
        // BuildConfig.GOOGLE_WEB_CLIENT_ID
        //
        // 실제 값은 local.properties의 GOOGLE_WEB_CLIENT_ID에서 가져옴
        // =====================================================
        buildConfigField( // build Config Field 함수를 실행함
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            "\"$googleWebClientId\""
        )

        // RetrofitClient에서 사용할 백엔드 기본 주소입니다.
        // android/local.properties에 API_BASE_URL=http://10.0.2.2:1113/ 처럼 적으면 여기로 들어옵니다.
        buildConfigField( // build Config Field 함수를 실행함
            "String",
            "API_BASE_URL",
            "\"$apiBaseUrl\""
        )

        buildConfigField( // build Config Field 함수를 실행함
            "String",
            "AI_ANALYZE_BASE_URL",
            "\"$aiAnalyzeBaseUrl\""
        )

        // MarketScreen WebView에서 로드할 NFT 마켓 웹 페이지 주소
        // android/local.properties에 NFT_MARKET_WEBVIEW_URL=http://10.0.2.2:5173/marketplace 처럼 설정할 수 있습니다.
        buildConfigField( // build Config Field 함수를 실행함
            "String",
            "NFT_MARKET_WEBVIEW_URL",
            "\"$nftMarketWebViewUrl\""
        )
    }

    buildTypes { // 이 블록 안의 내용이 시작됨
        debug { // debug 빌드 설정을 정함
            manifestPlaceholders["usesCleartextTraffic"] = "true" // 로컬 개발 서버 접속을 위해 debug에서만 HTTP를 허용함
        }

        release { // 이 블록 안의 내용이 시작됨
            isMinifyEnabled = false // false 값을 isMinifyEnabled인지 여부에 넣음
            isDebuggable = false // false 값을 isDebuggable인지 여부에 넣음
            manifestPlaceholders["usesCleartextTraffic"] = "false" // 배포 빌드에서는 HTTP 평문 통신을 막음
            if (hasReleaseSigningConfig) { // 릴리즈 서명 정보가 있을 때만 서명 설정을 연결함
                signingConfig = signingConfigs.getByName("release") // signingConfig 값을 정해줌
            }
            proguardFiles( // proguard Files 함수를 실행함
                getDefaultProguardFile("proguard-android-optimize.txt"), // get Default Proguard File 함수를 실행함
                "proguard-rules.pro"
            )
        }
    }

    compileOptions { // 이 블록 안의 내용이 시작됨
        sourceCompatibility = JavaVersion.VERSION_17 // sourceCompatibility 값을 정해줌
        targetCompatibility = JavaVersion.VERSION_17 // targetCompatibility 값을 정해줌
    }

    kotlinOptions { // 이 블록 안의 내용이 시작됨
        jvmTarget = "17" // jvmTarget 값을 정해줌
    }

    buildFeatures { // 이 블록 안의 내용이 시작됨
        // Jetpack Compose 사용
        compose = true // true 값을 compose 값에 넣음

        // BuildConfig.GOOGLE_WEB_CLIENT_ID를 사용하려면 true 필요
        buildConfig = true // true 값을 buildConfig 값에 넣음
    }
}

kotlin { // 이 블록 안의 내용이 시작됨
    jvmToolchain(17) // jvm Toolchain 함수를 실행함
}

dependencies { // 이 블록 안의 내용이 시작됨
    // =========================
    // Android 기본 라이브러리
    // =========================
    implementation(libs.androidx.core.ktx) // implementation 함수를 실행함
    implementation(libs.androidx.lifecycle.runtime.ktx) // implementation 함수를 실행함
    implementation(libs.androidx.activity.compose) // implementation 함수를 실행함

    // =========================
    // Jetpack Compose
    // =========================
    implementation(platform(libs.androidx.compose.bom)) // implementation 함수를 실행함
    implementation(libs.androidx.compose.ui) // implementation 함수를 실행함
    implementation(libs.androidx.compose.ui.graphics) // implementation 함수를 실행함
    implementation(libs.androidx.compose.ui.tooling.preview) // implementation 함수를 실행함
    implementation(libs.androidx.compose.material3) // implementation 함수를 실행함
    implementation("androidx.compose.material:material-icons-extended") // implementation 함수를 실행함
    implementation("androidx.navigation:navigation-compose:2.7.7") // implementation 함수를 실행함

    // =========================
    // Lifecycle / 상태 관리
    // =========================
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3") // implementation 함수를 실행함

    // =========================
    // 로컬 데이터 저장
    // =========================
    implementation("androidx.datastore:datastore-preferences:1.1.1") // implementation 함수를 실행함

    // =========================
    // Room 데이터베이스
    // =========================
    implementation("androidx.room:room-runtime:2.6.1") // implementation 함수를 실행함
    implementation("androidx.room:room-ktx:2.6.1") // implementation 함수를 실행함
    ksp("androidx.room:room-compiler:2.6.1") // ksp 함수를 실행함

    // =========================
    // Supabase
    // =========================
    implementation(platform("io.github.jan-tennert.supabase:bom:3.0.0")) // implementation 함수를 실행함
    implementation("io.github.jan-tennert.supabase:auth-kt") // implementation 함수를 실행함
    implementation("io.github.jan-tennert.supabase:postgrest-kt") // implementation 함수를 실행함

    // =========================
    // 이미지 로딩
    // =========================
    implementation("io.coil-kt:coil-compose:2.6.0") // implementation 함수를 실행함

    // =========================
    // Solana / Wallet 연동
    // =========================
    implementation("com.solanamobile:mobile-wallet-adapter-clientlib-ktx:2.0.3") // implementation 함수를 실행함
    implementation("org.purejava:tweetnacl-java:1.1.3") // implementation 함수를 실행함

    // =========================
    // Coroutine
    // =========================
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3") // implementation 함수를 실행함

    // =========================
    // Retrofit / OkHttp
    // =========================
    implementation("com.squareup.retrofit2:retrofit:2.11.0") // implementation 함수를 실행함
    implementation("com.squareup.retrofit2:converter-gson:2.11.0") // implementation 함수를 실행함
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0") // implementation 함수를 실행함

    // =========================
    // Ktor
    // =========================
    // 기존에 2.3.10과 3.0.3이 같이 있어서 충돌 가능성이 있음.
    // 삭제하지 않고 주석 처리만 해둠.
    // implementation("io.ktor:ktor-client-okhttp:2.3.10")

    implementation("io.ktor:ktor-client-core:3.0.3") // implementation 함수를 실행함
    implementation("io.ktor:ktor-client-okhttp:3.0.3") // implementation 함수를 실행함
    implementation("io.ktor:ktor-client-logging:3.0.3") // implementation 함수를 실행함

    // =========================
    // Bitcoin
    // =========================
    implementation("org.bitcoinj:bitcoinj-core:0.16.3") // implementation 함수를 실행함

    // =========================
    // Kakao Login SDK
    // =========================
    implementation("com.kakao.sdk:v2-user:2.20.1") // implementation 함수를 실행함

    // =========================
    // Google Login
    // =========================
    implementation("com.google.android.gms:play-services-auth:21.2.0") // implementation 함수를 실행함

    // =========================
    // 테스트
    // =========================
    testImplementation(libs.junit) // test Implementation 함수를 실행함
    androidTestImplementation(libs.androidx.junit) // android Test Implementation 함수를 실행함
    androidTestImplementation(libs.androidx.espresso.core) // android Test Implementation 함수를 실행함

    androidTestImplementation(platform(libs.androidx.compose.bom)) // android Test Implementation 함수를 실행함
    androidTestImplementation(libs.androidx.compose.ui.test.junit4) // android Test Implementation 함수를 실행함

    debugImplementation(libs.androidx.compose.ui.tooling) // debug Implementation 함수를 실행함
    debugImplementation(libs.androidx.compose.ui.test.manifest) // debug Implementation 함수를 실행함
}
