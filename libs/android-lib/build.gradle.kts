plugins {
    id("dev.nx.gradle.project-graph") version("0.1.10")
    alias(shared.plugins.android.library)
    alias(shared.plugins.kotlin.android)
    alias(shared.plugins.kotlin.compose)
}

group = "app.drawmark.android"
version = "0.0.1"

android {
    namespace = "app.drawmark.android.lib"
    compileSdk = 36

    defaultConfig {
        minSdk = 33

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget("11")
    }
}

dependencies {
    implementation(shared.androidx.core.ktx)
    implementation(shared.androidx.lifecycle.runtime.ktx)
    implementation(platform(shared.androidx.compose.bom))
    implementation(shared.androidx.compose.ui)
    implementation(shared.androidx.compose.ui.graphics)
    implementation(shared.androidx.compose.ui.tooling.preview)
    implementation(shared.androidx.compose.material3)

    // Motion Event Predictor for low-latency stylus input
    implementation(libLocal.androidx.input.motionprediction)

    // Android Ink API dependencies
    implementation(libLocal.androidx.ink.authoring)
    implementation(libLocal.androidx.ink.brush)
    implementation(libLocal.androidx.ink.geometry)
    implementation(libLocal.androidx.ink.nativeloader)
    implementation(libLocal.androidx.ink.rendering)
    implementation(libLocal.androidx.ink.strokes)

    // Gson for JSON serialization
    implementation(libLocal.gson)

    testImplementation(shared.junit)
    androidTestImplementation(shared.androidx.junit)
    androidTestImplementation(shared.androidx.espresso.core)
    androidTestImplementation(platform(shared.androidx.compose.bom))
    androidTestImplementation(shared.androidx.compose.ui.test.junit4)
    debugImplementation(shared.androidx.compose.ui.tooling)
    debugImplementation(shared.androidx.compose.ui.test.manifest)
}
