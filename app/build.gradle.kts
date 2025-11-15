plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.emsi.emsipresence"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.emsi.emsipresence"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            isDebuggable = true
            buildConfigField("String", "GEMINI_API_KEY", "\"AIzaSyBrjU1slQIbG3NChnFr_EXiSAUDmCGIDiM\"")
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "GEMINI_API_KEY", "\"AIzaSyBrjU1slQIbG3NChnFr_EXiSAUDmCGIDiM\"")
        }
    }

    buildFeatures {
        dataBinding = true
        buildConfig = true  // Nécessaire pour BuildConfig.GEMINI_API_KEY
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // Core Android
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.gridlayout)
    implementation(libs.fragment)
    implementation(libs.recyclerview)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.database)

    // Google Play Services
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location.v2101) // Utilisez la version la plus récente

    // Networking
    implementation(libs.okhttp)
    implementation(libs.retrofit2.retrofit)
    implementation(libs.converter.gson)

    // Google AI
    implementation(libs.google.generativeai)

    // Maps Services
    implementation(libs.google.maps.services)
    implementation(libs.slf4j.simple)

    // Image Loading
    implementation(libs.picasso)
    implementation (libs.okhttp.v4120)
    // JSON
    implementation(libs.json)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}