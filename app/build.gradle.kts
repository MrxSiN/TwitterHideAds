plugins {
    id("com.android.application")
}

val appVersion = "1.1.0"

android {
    namespace = "my.MrxSiN.twitterhideads"
    compileSdk = 36

    defaultConfig {
        applicationId = "my.MrxSiN.twitterhideads"
        minSdk = 24
        targetSdk = 36
        versionCode = 22
        versionName = appVersion
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    packaging {
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
}
