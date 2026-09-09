plugins {
    id("com.android.application")
}

val appVersion = "2.1.0"

android {
    namespace = "my.MrxSiN.twitterhideads"
    compileSdk = 36

    defaultConfig {
        applicationId = "my.MrxSiN.twitterhideads"
        minSdk = 26
        targetSdk = 36
        versionCode = 32
        versionName = appVersion
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles("proguard-rules.pro")
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
        resources {
            merges += "META-INF/xposed/*"
            excludes += setOf(
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/LICENSE*",
                "META-INF/NOTICE*"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
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
    compileOnly("io.github.libxposed:api:102.0.0")
    implementation("org.luckypray:dexkit:2.2.0")
}
