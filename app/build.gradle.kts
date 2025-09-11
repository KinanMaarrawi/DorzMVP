plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    //google maps API
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

// Configure the secrets plugin
secrets {
    propertiesFileName = "app/secrets.properties"
    defaultPropertiesFileName = "app/local.defaults.properties"
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
    // Import the Kotlin BOM to manage Kotlin standard library versions
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:2.0.0")) // Updated to 2.0.0

    // Explicitly add kotlin-compiler-embeddable; version will be managed by the BOM
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.runtime:runtime-livedata:+") // Added for observeAsState
    implementation(libs.play.services.maps) // Base Google Maps SDK
    implementation("com.google.maps.android:maps-compose:4.3.3") // Google Maps Compose Utilities
    implementation("com.google.android.libraries.places:places:3.5.0") // Places SDK
    implementation("androidx.concurrent:concurrent-futures-ktx:1.3.0") // Added for ListenableFuture
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3") // For Task.await()
    implementation("com.google.guava:guava:32.1.3-android") // Added full Guava library
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    //navhost stuff
    val nav_version = "2.9.3"
    implementation("androidx.navigation:navigation-compose:$nav_version")

    constraints {
        implementation("com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava") {
            because("Guava already includes ListenableFuture, and this special version is empty and resolves conflicts.")
        }
    }
}