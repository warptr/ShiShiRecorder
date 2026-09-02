plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

val signingStoreFile = providers.gradleProperty("SHISHIRECORDER_KEYSTORE_FILE").orNull
    ?: System.getenv("SHISHIRECORDER_KEYSTORE_FILE")
val signingStorePassword = providers.gradleProperty("SHISHIRECORDER_KEYSTORE_PASSWORD").orNull
    ?: System.getenv("SHISHIRECORDER_KEYSTORE_PASSWORD")
val signingKeyAlias = providers.gradleProperty("SHISHIRECORDER_KEY_ALIAS").orNull
    ?: System.getenv("SHISHIRECORDER_KEY_ALIAS")
val signingKeyPassword = providers.gradleProperty("SHISHIRECORDER_KEY_PASSWORD").orNull
    ?: System.getenv("SHISHIRECORDER_KEY_PASSWORD")

android {
    namespace = "com.warptr.ShiShiRecorder"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.warptr.ShiShiRecorder"
        minSdk = 29
        targetSdk = 36
        versionCode = 2
        versionName = "1.0.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    signingConfigs {
        create("personalRelease") {
            if (!signingStoreFile.isNullOrBlank() && !signingStorePassword.isNullOrBlank()
                && !signingKeyAlias.isNullOrBlank() && !signingKeyPassword.isNullOrBlank()) {
                storeFile = file(signingStoreFile)
                storePassword = signingStorePassword
                keyAlias = signingKeyAlias
                keyPassword = signingKeyPassword
                storeType = "PKCS12"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (!signingStoreFile.isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("personalRelease")
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    sourceSets {
        getByName("main").java.setSrcDirs(listOf("src/main/newjava"))
        getByName("main").res.setSrcDirs(listOf("src/main/newres"))
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        buildConfig = true
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    ndkVersion = "27.0.12077973"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.activity:activity-ktx:1.9.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
