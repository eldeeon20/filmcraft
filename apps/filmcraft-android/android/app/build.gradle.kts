plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val keystorePath = (project.findProperty("filmcraftStoreFile") as String?)
    ?: System.getenv("FILMCRAFT_KEYSTORE")

android {
    namespace = "ai.storyteller.filmcraft"
    compileSdk = 35

    defaultConfig {
        applicationId = "ai.storyteller.filmcraft"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "0.10.0"
        ndk { abiFilters += "arm64-v8a" }
    }

    signingConfigs {
        if (keystorePath != null) {
            create("ci") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("FILMCRAFT_KEYSTORE_PASSWORD") ?: "filmcraft-ci"
                keyAlias = System.getenv("FILMCRAFT_KEY_ALIAS") ?: "filmcraft-ci"
                keyPassword = System.getenv("FILMCRAFT_KEY_PASSWORD") ?: "filmcraft-ci"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (keystorePath != null) {
                signingConfigs.getByName("ci")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    packaging {
        jniLibs.useLegacyPackaging = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}
