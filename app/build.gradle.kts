plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.floppy.app"
    compileSdk = 36

    val useMockApiOverride = providers.gradleProperty("floppy.useMockApi").orNull
    val debugUseMockApi = useMockApiOverride ?: "true"
    val releaseUseMockApi = useMockApiOverride ?: "false"
    val apiBaseUrl = providers.gradleProperty("floppy.apiBaseUrl").orNull ?: "http://8000-fph4ha6m.agent-sandbox.baidu-int.com/"

    defaultConfig {
        applicationId = "com.floppy.app"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("boolean", "USE_MOCK_API", debugUseMockApi)
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "USE_MOCK_API", debugUseMockApi)
            buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        }
        release {
            isMinifyEnabled = false
            buildConfigField("boolean", "USE_MOCK_API", releaseUseMockApi)
            buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
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

    kotlin {
        jvmToolchain(17)
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose-android:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose-android:2.9.4")
    implementation("androidx.compose.ui:ui-android:1.8.2")
    implementation("androidx.compose.ui:ui-tooling-preview-android:1.8.2")
    implementation("androidx.compose.foundation:foundation-android:1.8.2")
    implementation("androidx.compose.material3:material3-android:1.3.2")
    implementation("androidx.navigation:navigation-compose-android:2.9.0")
    implementation("androidx.datastore:datastore-preferences-android:1.1.3")
    implementation("androidx.media3:media3-exoplayer:1.7.1")
    implementation("androidx.media3:media3-ui:1.7.1")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    debugImplementation("androidx.compose.ui:ui-tooling-android:1.8.2")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}
