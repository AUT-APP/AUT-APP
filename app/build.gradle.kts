plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
    // Remove com.google.relay if not used; comment out for now
    // id("com.google.relay") version "0.3.12"
}

android {
    namespace = "com.example.autapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.autapp"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14" // Updated to match Kotlin 1.9.24
    }

}
dependencies {
    // Core Android dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // OpenStreetMap
    implementation("org.osmdroid:osmdroid-android:6.1.18")

// Compose dependencies
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
// Remove material1 to avoid conflicts with material3
// implementation("androidx.compose.material:material:1.6.7")
    implementation ("androidx.work:work-runtime-ktx:2.8.1")
    implementation ("com.google.accompanist:accompanist-pager:0.25.1")
    implementation ("androidx.compose.material3:material3:1.2.1")
    implementation ("androidx.compose.material:material-icons-extended:1.6.8")

// Gson and Coroutines
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

// Room dependencies
    implementation(libs.androidx.room.runtime)
    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation ("androidx.compose.material:material-icons-extended:1.6.8")
    implementation(libs.play.services.maps)
    kapt(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

// Time dependencies
    implementation("com.jakewharton.threetenabp:threetenabp:1.4.6")

// Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

// ViewModel Compose integration
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation ("androidx.lifecycle:lifecycle-viewmodel-savedstate:2.8.6")
// Gemini API (keep for now, remove if unused)
    implementation("com.google.ai.client.generativeai:generativeai:0.1.1")

// Testing dependencies
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

}

