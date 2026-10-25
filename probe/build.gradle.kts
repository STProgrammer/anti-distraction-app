plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "app.intentional.probe"
    compileSdk = 36
    defaultConfig { applicationId = "app.intentional.probe"; minSdk = 31; targetSdk = 36; versionCode = 1; versionName = "1"; testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }
    flavorDimensions += "identity"
    productFlavors {
        register("one") { dimension = "identity"; applicationIdSuffix = ".one"; resValue("string", "app_name", "Intent test A") }
        register("two") { dimension = "identity"; applicationIdSuffix = ".two"; resValue("string", "app_name", "Intent test B") }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
}
