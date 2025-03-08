plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.healthmonitoringapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.healthmonitoringapp"
        minSdk = 31  // Minimum SDK set for Android 12 compatibility
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ✅ Enable multidex support if needed
        multiDexEnabled = true
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

    // ✅ Enable View Binding
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // ✅ AndroidX Core & UI
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")  // Latest stable version
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.2.1")

    // ✅ MultiDex Support (for large apps)
    implementation("androidx.multidex:multidex:2.0.1")

    // ✅ Unit Testing
    testImplementation("junit:junit:4.13.2")

    // ✅ Android Testing
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
