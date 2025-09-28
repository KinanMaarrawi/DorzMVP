import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.ksp)
    id("kotlin-parcelize")
}

android {
    namespace = "com.example.dorzmvp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.dorzmvp"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val secrets = Properties()
        val secretsFile = project.file("secrets.properties")
        if (secretsFile.exists()) {
            secrets.load(FileInputStream(secretsFile))
        } else {
            println("Warning: secrets.properties not found. API keys will be empty.")
        }

        // Load keys from properties, defaulting to empty strings if not found.
        val yangoApiKey = secrets.getProperty("YANGO_API_KEY", "")
        val yangoClid = secrets.getProperty("YANGO_CLID", "")
        val mapsApiKey = secrets.getProperty("MAPS_API_KEY", "")

        // Expose Yango secrets as BuildConfig fields
        buildConfigField("String", "YANGO_API_KEY", "\"$yangoApiKey\"")
        buildConfigField("String", "YANGO_CLID", "\"$yangoClid\"")

        // --- THIS IS THE FIX ---
        // Expose the Maps API key as a BuildConfig field so Kotlin code can access it
        buildConfigField("String", "MAPS_API_KEY", "\"$mapsApiKey\"")

        // Inject the Maps API key into the manifest.
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // Debug build type configuration.
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
        buildConfig = true
    }
    packaging {
        resources {
            excludes.add("kotlin/internal/internal.kotlin_builtins")
            excludes.add("kotlin/reflect/reflect.kotlin_builtins")
            excludes.add("kotlin/kotlin.kotlin_builtins")
            excludes.add("kotlin/coroutines/coroutines.kotlin_builtins")
            excludes.add("kotlin/ranges/ranges.kotlin_builtins")
            excludes.add("kotlin/collections/collections.kotlin_builtins")
            excludes.add("kotlin/annotation/annotation.kotlin_builtins")
        }
    }
}

dependencies {
    // Kotlin Standard Library and Compiler
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:2.0.0"))
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable")

    // AndroidX Core Libraries
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Jetpack Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.runtime:runtime-livedata:+")
    implementation(libs.androidx.compose.foundation)

    // Google Play Services and Maps
    implementation(libs.play.services.maps)
    implementation("com.google.maps.android:maps-compose:4.3.3")
    implementation("com.google.android.libraries.places:places:3.5.0")

    // Asynchronous Programming
    implementation("androidx.concurrent:concurrent-futures-ktx:1.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // Utility Libraries
    implementation("com.google.guava:guava:32.1.3-android")

    // Google Engage Core (Purpose might need further clarification based on usage)
    implementation(libs.engage.core)
    implementation(libs.androidx.compose.runtime.saveable)

    // Testing Libraries
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Navigation
    val nav_version = "2.9.3"
    implementation("androidx.navigation:navigation-compose:$nav_version")
    implementation("com.google.maps:google-maps-services:2.2.0")

    // Networking (Retrofit and OkHttp)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.10.0")

    // Constraints
    constraints {
        implementation("com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava") {
            because("Guava already includes ListenableFuture, and this special version is empty and resolves conflicts.")
        }
    }

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
}
