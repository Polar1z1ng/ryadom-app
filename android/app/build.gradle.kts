plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "ru.ryadom.safety"
    compileSdk = 35
    defaultConfig {
        applicationId = "ru.ryadom.safety"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "0.2.1-test"
    }
    buildTypes {
        release { isMinifyEnabled = false }
        debug { applicationIdSuffix = ".test" }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
