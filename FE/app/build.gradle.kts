import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use(::load)
    }
}

fun getLocalProperty(name: String, defaultValue: String = ""): String {
    val fromLocal = localProperties.getProperty(name)
    val fromGradle = project.findProperty(name) as? String
    return (fromLocal ?: fromGradle ?: defaultValue).trim()
}

fun getFirstLocalProperty(vararg names: String): String {
    return names
        .asSequence()
        .map { getLocalProperty(it) }
        .firstOrNull { it.isNotBlank() }
        .orEmpty()
}

fun normalizedBaseUrl(value: String, fallback: String): String {
    val candidate = value.ifBlank { fallback }
    val withScheme = if (candidate.startsWith("http://") || candidate.startsWith("https://")) {
        candidate
    } else {
        fallback
    }
    return if (withScheme.endsWith("/")) withScheme else "$withScheme/"
}

val baseUrlEmulator = normalizedBaseUrl(
    getFirstLocalProperty("BASE_URL_EMULATOR", "BASE_URL_DEV"),
    "http://10.0.2.2:8080/",
)
val baseUrlDevice = normalizedBaseUrl(
    getFirstLocalProperty("BASE_URL_DEVICE"),
    baseUrlEmulator,
)
val baseUrlProd = normalizedBaseUrl(getLocalProperty("BASE_URL_PROD"), "http://10.0.2.2:8082/")
val googleWebClientId = getLocalProperty("GOOGLE_WEB_CLIENT_ID")
val kakaoNativeAppKey = getLocalProperty("KAKAO_NATIVE_APP_KEY")
val sharedDebugKeystore = rootProject.file("signing/debug.keystore")

android {
    namespace = "com.ssafy.e106"
    compileSdk = 35

    signingConfigs {
        create("sharedDebug") {
            // Shared debug keystore keeps Firebase SHA stable across teammates.
            storeFile = sharedDebugKeystore
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    defaultConfig {
        applicationId = "com.ssafy.e106"
        minSdk = 26                              // UsageStats 최소 SDK
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        // local.properties → BuildConfig
        buildConfigField("String", "BASE_URL_DEV",  "\"$baseUrlEmulator\"")
        buildConfigField("String", "BASE_URL_EMULATOR", "\"$baseUrlEmulator\"")
        buildConfigField("String", "BASE_URL_DEVICE", "\"$baseUrlDevice\"")
        buildConfigField("String", "BASE_URL_PROD", "\"$baseUrlProd\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
        buildConfigField("String", "KAKAO_NATIVE_APP_KEY", "\"$kakaoNativeAppKey\"")
        manifestPlaceholders["kakaoAuthScheme"] = "kakao$kakaoNativeAppKey"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    androidResources {
        noCompress += listOf("npy", "bin")
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("sharedDebug")
            buildConfigField("String", "BASE_URL", "\"$baseUrlEmulator\"")
        }
        create("device") {
            initWith(getByName("debug"))
            matchingFallbacks += listOf("debug")
            signingConfig = signingConfigs.getByName("sharedDebug")
            buildConfigField("String", "BASE_URL", "\"$baseUrlDevice\"")
        }
        release {
            buildConfigField("String", "BASE_URL", "\"$baseUrlProd\"")
            isMinifyEnabled = true
            isShrinkResources = true
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
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // ── Compose BOM ──────────────────────────────────────────────────
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)

    // ── Activity / Lifecycle ─────────────────────────────────────────────
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.runtime.compose)           // collectAsStateWithLifecycle

    // ── Navigation ───────────────────────────────────────────────────
    implementation(libs.navigation.compose)

    // ── Hilt (DI) ───────────────────────────────────────────────────────
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)             // hiltViewModel()

    // ── Network ────────────────────────────────────────────────────────────
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)

    // ── Coroutines ────────────────────────────────────────────────────────
    implementation(libs.kotlinx.coroutines.android)

    // ── Firebase BOM + FCM ────────────────────────────────────────────────────────────────
    val firebaseBom = platform(libs.firebase.bom)
    implementation(firebaseBom)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.analytics)

    // ── DataStore (JWT 토큰 로친 저장) ────────────────────────────────────────────────────────
    implementation(libs.datastore.preferences)

    // ── Security (JWT 토큰 암호화 저장) ─────────────────────────────────────────────────────
    implementation(libs.security.crypto)

    // ── Coil (이미지 로딩) ─────────────────────────────────────────────────────────────────────────
    implementation(libs.coil.compose)
    implementation(libs.coil.svg)

    // ── Google Sign-In (Credential Manager) ────────────────────────────────
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)

    // ── Kakao Login ────────────────────────────────────────────────────────
    implementation(libs.kakao.user)

    // ── On-Device AI (LiteRT-LM) ──────────────────────────────────────────
    implementation(libs.litertlm.android)

    // [AI 모델 교체] XGBoost 추론은 xgb_model_android.json 직접 파싱(순수 Kotlin)으로 구현.
    // xgboost4j-android 아티팩트는 Maven Central 미존재, xgboost4j JVM은 Android ARM 미지원.
    // 외부 의존성 추가 없음.

    // ── Testing ──────────────────────────────────────────────────────────────
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.junit.ext)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
}

tasks.register("testClasses") {
    group = "verification"
    description = "Compatibility alias for tools that expect a JVM-style testClasses task on :app."
    dependsOn("testDebugUnitTest")
}
