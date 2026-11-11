plugins {
    id("com.android.application")
}

android {
    namespace = "com.monomaxia.gnoseon"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.monomaxia.gnoseon.preview"
        minSdk = 23
        targetSdk = 36
        versionCode = 34
        versionName = "0.11.10"
    }

    // Reuse the original Preview signing certificate so a new installation
    // updates the existing Quizara Preview without deleting saved games.
    // The keystore is kept out of git: without it, builds (e.g. CI) fall back to
    // the standard debug key. Never use it for a Play Store release.
    val previewKeystore = file("../development/preview.keystore")
    signingConfigs {
        if (previewKeystore.exists()) {
            create("preview") {
                storeFile = previewKeystore
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfigs.findByName("preview")?.let { signingConfig = it }
        }
        getByName("release") {
            isMinifyEnabled = false
            signingConfigs.findByName("preview")?.let { signingConfig = it }
        }
    }
}
