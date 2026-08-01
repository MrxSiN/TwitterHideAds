plugins {
    id("com.android.application")
}

val appVersion = "1.2.6-test-app-icon"

android {
    namespace = "my.MrxSiN.twitterhideads"
    compileSdk = 36

    defaultConfig {
        applicationId = "my.MrxSiN.twitterhideads"
        minSdk = 24
        targetSdk = 36
        versionCode = 29
        versionName = appVersion
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes += setOf(
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/LICENSE*",
                "META-INF/NOTICE*"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            output.outputFileName.set("TwitterHideAds-v$appVersion.apk")
        }
    }
}

dependencies {
    compileOnly("de.robv.android.xposed:api:82")
    implementation("org.luckypray:dexkit:2.2.0")
}
