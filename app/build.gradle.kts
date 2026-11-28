import java.util.Properties

plugins {
    id("com.android.application")
}

android {
    namespace = "com.monomaxia.gnoseon"
    compileSdk = 36

    defaultConfig {
        // Play Store app: com.monomaxia.gnoseon. Debug builds add ".preview" (see buildTypes),
        // so the preview APK stays a separate app that keeps updating existing installs.
        applicationId = "com.monomaxia.gnoseon"
        minSdk = 23
        targetSdk = 36
        versionCode = 56
        versionName = "0.13.5"
    }

    // Reuse the original Preview signing certificate so a new installation
    // updates the existing Quizara Preview without deleting saved games.
    // The keystore is kept out of git: without it, builds (e.g. CI) fall back to
    // the standard debug key. Never use it for a Play Store release.
    val previewKeystore = file("../development/preview.keystore")
    // Play Store upload key: keystore.properties in the project root (not in git) names the
    // keystore file and its passwords. Without it, release builds come out unsigned.
    val uploadProps = rootProject.file("keystore.properties")
    signingConfigs {
        if (previewKeystore.exists()) {
            create("preview") {
                storeFile = previewKeystore
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
        if (uploadProps.exists()) {
            val p = Properties().apply { uploadProps.inputStream().use { load(it) } }
            create("upload") {
                storeFile = rootProject.file(p.getProperty("storeFile"))
                storePassword = p.getProperty("storePassword")
                keyAlias = p.getProperty("keyAlias")
                keyPassword = p.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".preview"
            signingConfigs.findByName("preview")?.let { signingConfig = it }
        }
        getByName("release") {
            isMinifyEnabled = false
            signingConfigs.findByName("upload")?.let { signingConfig = it }
        }
    }
}
