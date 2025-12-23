import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(shared.plugins.android.application)
    alias(shared.plugins.kotlin.android)
    alias(shared.plugins.kotlin.compose)
}

android {
    namespace = "app.drawmark.android.prototype"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "app.drawmark.android.prototype"
        minSdk = 33
        targetSdk = 36
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
    buildFeatures {
        compose = true
    }
}


kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget("11")
    }
}

dependencies {
    implementation(shared.androidx.core.ktx)
    implementation(shared.androidx.lifecycle.runtime.ktx)
    implementation(appLocal.androidx.activity.compose)
    implementation(platform(shared.androidx.compose.bom))
    implementation(shared.androidx.compose.ui)
    implementation(shared.androidx.compose.ui.graphics)
    implementation(shared.androidx.compose.ui.tooling.preview)
    implementation(shared.androidx.compose.material3)

    // Internal library from libs folder (includes Ink API, Gson, etc.)
    implementation("app.drawmark.android:lib")

    testImplementation(shared.junit)
    androidTestImplementation(shared.androidx.junit)
    androidTestImplementation(shared.androidx.espresso.core)
    androidTestImplementation(platform(shared.androidx.compose.bom))
    androidTestImplementation(shared.androidx.compose.ui.test.junit4)
    debugImplementation(shared.androidx.compose.ui.tooling)
    debugImplementation(shared.androidx.compose.ui.test.manifest)
}