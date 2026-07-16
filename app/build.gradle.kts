plugins {
    alias(libs.plugins.android.application)
}

val appVersion = "1.0.0"
val appVersionCode = 18

val releaseKeystorePath = providers.environmentVariable("ANDROID_KEYSTORE_PATH").orNull
val releaseKeystoreAlias = providers.environmentVariable("ANDROID_KEYSTORE_ALIAS").orNull
val releaseKeystorePassword = providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD").orNull
val releaseKeyPassword = providers.environmentVariable("ANDROID_KEY_PASSWORD").orNull
val releaseSigningConfigured = listOf(
    releaseKeystorePath,
    releaseKeystoreAlias,
    releaseKeystorePassword,
    releaseKeyPassword
).all { !it.isNullOrBlank() }

android {
    namespace = "my.MrxSiN.twitterhideads"
    compileSdk = 37

    defaultConfig {
        applicationId = "my.MrxSiN.twitterhideads"
        minSdk = 24
        targetSdk = 37
        versionCode = appVersionCode
        versionName = appVersion

        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
    }

    val ciReleaseSigning = if (releaseSigningConfigured) {
        signingConfigs.create("ciRelease") {
            storeFile = rootProject.file(releaseKeystorePath!!)
            storePassword = releaseKeystorePassword!!
            keyAlias = releaseKeystoreAlias!!
            keyPassword = releaseKeyPassword!!
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = true
        }
    } else {
        null
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            ciReleaseSigning?.let { signingConfig = it }
        }
    }

    buildFeatures {
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    compileOnly(libs.xposed.api)
}
