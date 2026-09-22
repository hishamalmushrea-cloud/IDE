plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.multimodule.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.multimodule.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":feature"))
    implementation("androidx.appcompat:appcompat:1.7.0")
}
