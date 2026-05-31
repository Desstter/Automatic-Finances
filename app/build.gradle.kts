import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// Gemini API key resolution order: CI env var -> Gradle property (local.properties) -> empty.
// local.properties is gitignored, so the key never lands in VCS. A release build with an empty
// key still compiles; the voice feature degrades gracefully and surfaces a clear error.
val geminiApiKey: String = System.getenv("GEMINI_API_KEY")
    ?: Properties().apply {
        val f = rootProject.file("local.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }.getProperty("GEMINI_API_KEY")
    ?: ""

// --- CI/CD: values injected by GitHub Actions environment ---
val ciVersionCode  = System.getenv("VERSION_CODE")?.toIntOrNull()
val ciVersionName  = System.getenv("VERSION_NAME")
val ciKeystorePath = System.getenv("KEYSTORE_PATH")
val ciKeystorePass = System.getenv("KEYSTORE_PASSWORD")
val ciKeyAlias     = System.getenv("KEY_ALIAS")
val ciKeyPass      = System.getenv("KEY_PASSWORD")

android {
    namespace = "com.example.automaticfinances"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.automaticfinances"
        minSdk = 29
        targetSdk = 35
        versionCode = ciVersionCode ?: 1
        versionName = ciVersionName ?: "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Exposed to runtime via BuildConfig.GEMINI_API_KEY (see GeminiService).
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
    }

    signingConfigs {
        create("release") {
            // In CI the keystore is restored from a secret; locally fall back to the
            // committed dev keystore so release builds keep the same signing identity.
            if (ciKeystorePath != null) {
                storeFile     = file(ciKeystorePath)
                storePassword = ciKeystorePass
                keyAlias      = ciKeyAlias
                keyPassword   = ciKeyPass
            } else {
                storeFile     = file("release-key.keystore")
                storePassword = "123456"
                keyAlias      = "release"
                keyPassword   = "123456"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            isDebuggable = false
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// Export Room schemas so migrations can be validated with MigrationTestHelper going forward.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui.text.google.fonts)
    
    // Room
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    
    // ViewModel Compose
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    
    // Navigation Compose
    implementation(libs.androidx.navigation.compose)
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    
    // Crypto hash
    implementation(libs.commons.codec)

    // Networking + JSON for Gemini NLP layer
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)
    
    // DataStore for theme preferences
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // Material Icons Extended for brightness icons
    implementation("androidx.compose.material:material-icons-extended:1.7.5")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}