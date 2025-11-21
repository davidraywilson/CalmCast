import java.io.File

// Load environment variables from .env file
fun loadEnv(): Map<String, String> {
    val envFile = File(rootProject.rootDir, ".env")
    val envVars = mutableMapOf<String, String>()
    if (envFile.exists()) {
        envFile.readLines().forEach { line ->
            if (line.isNotEmpty() && !line.startsWith("#")) {
                val parts = line.split("=", limit = 2)
                if (parts.size == 2) {
                    envVars[parts[0].trim()] = parts[1].trim()
                }
            }
        }
    }
    return envVars
}

val envVars = loadEnv()

plugins {
    id("com.android.application")
    kotlin("android")
    id("com.google.devtools.ksp") version "1.9.20-1.0.13"
    id("androidx.room") version "2.6.1"
}

android {
    namespace = "com.calmcast.podcast"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.calmcast.podcast"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Inject environment variables into BuildConfig
        buildConfigField("String", "TADDY_API_KEY", "\"${envVars["TADDY_API_KEY"] ?: ""}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }



    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-process:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.0")
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    val composeBom = platform("androidx.compose:compose-bom:2023.10.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.1.1")
    implementation("androidx.compose.material:material-icons-extended:1.7.0")
    implementation("androidx.navigation:navigation-compose:2.7.4")
    implementation("com.google.android.material:material:1.10.0")

    // Networking and GraphQL
    implementation("com.squareup.retrofit2:retrofit:2.10.0")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    implementation("com.apollographql.apollo3:apollo-runtime:3.8.2")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Media3 (ExoPlayer) for audio playback
    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-session:1.3.1")

    // Room for database
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    ksp("androidx.room:room-compiler:$room_version")
    implementation("androidx.room:room-ktx:$room_version")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Mudita Mindful Design Framework
    implementation("com.mudita:MMD:1.0.0")

    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.5.0")
}