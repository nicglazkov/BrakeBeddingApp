plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-parcelize") // Add this line to enable @Parcelize
}

android {
    namespace = "com.example.brakebeddingapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.brakebeddingapp"
        minSdk = 21
        targetSdk = 34
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
        kotlinCompilerExtensionVersion = "1.5.12" // Ensure this matches your Compose version
    }
}

dependencies {
    // Core Android dependencies
    implementation("androidx.appcompat:appcompat:1.4.0")
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.4.0")
    implementation("androidx.activity:activity-ktx:1.4.0")
    implementation("androidx.recyclerview:recyclerview:1.2.1")

    implementation("com.google.code.gson:gson:2.8.8")

    // Compose dependencies
    implementation("androidx.compose.ui:ui:1.5.0")
    implementation("androidx.compose.material:material:1.5.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.5.0")
    implementation("androidx.compose.runtime:runtime-livedata:1.5.0")

    // Test dependencies
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
}
