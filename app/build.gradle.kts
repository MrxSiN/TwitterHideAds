plugins {
    id("com.android.application")
}

val appVersion = "3.0.0"

val envKeystorePath = System.getenv("ANDROID_KEYSTORE_PATH")
val envKeystoreAlias = System.getenv("ANDROID_KEYSTORE_ALIAS")
val envKeystorePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
val envKeyPassword = System.getenv("ANDROID_KEY_PASSWORD")

android {
    namespace = "my.MrxSiN.twitterhideads"
    compileSdk = 36
    ndkVersion = "28.2.13676358"

    /*
     * Release signing is supplied by the environment so that no credential ever
     * reaches version control. Local builds without those variables stay
     * unsigned instead of failing.
     */
    val releaseSigningConfig = if (
        !envKeystorePath.isNullOrBlank() &&
        !envKeystoreAlias.isNullOrBlank() &&
        !envKeystorePassword.isNullOrBlank() &&
        !envKeyPassword.isNullOrBlank() &&
        file(envKeystorePath).isFile
    ) {
        signingConfigs.create("release") {
            storeFile = file(envKeystorePath)
            storePassword = envKeystorePassword
            keyAlias = envKeystoreAlias
            keyPassword = envKeyPassword
        }
    } else {
        null
    }

    defaultConfig {
        applicationId = "io.github.mrxsin.twitterhideads"
        minSdk = 26
        targetSdk = 36
        versionCode = 34
        versionName = appVersion
        // Brainfuck request timing in PolicyStats: debug builds, or -PpolicyTiming.
        buildConfigField("boolean", "POLICY_TIMING", project.hasProperty("policyTiming").toString())
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    sourceSets {
        // The JVM suite (frozen legacy oracle, fixtures, parity, robustness and
        // benchmarks) also runs on devices: connectedDebugAndroidTest.
        getByName("androidTest").java.srcDir("src/test/java")
    }

    // -PbenchmarkRelease runs the instrumented tests against the release build
    // (not debuggable: JIT on, CheckJNI off, optimized native code), signed with
    // the debug key. Local benchmarking only.
    if (project.hasProperty("benchmarkRelease")) {
        testBuildType = "release"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            releaseSigningConfig?.let { signingConfig = it }
            // -PlocalRelease: sign the optimized release build with the debug key
            // so it can replace a debug install for on-device benchmarking.
            if (project.hasProperty("localRelease") || project.hasProperty("benchmarkRelease")) {
                signingConfig = signingConfigs.getByName("debug")
            }
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

    buildFeatures {
        buildConfig = true
    }

    // The Brainfuck policy core: brainfuck/src/*.bf compiled ahead of time by
    // tools/bftool/gen.py into cpp/generated/, then by the NDK into
    // libtwitterbf.so. The generated C is committed, so a plain build needs no
    // Python; checkBrainfuck (part of `check` and CI) rejects stale output.
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
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

    testImplementation("junit:junit:4.13.2")
    testImplementation("io.github.libxposed:api:102.0.0")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestCompileOnly("io.github.libxposed:api:102.0.0")
}

// ---- Brainfuck core: generation, checks and the host build used by JVM tests.

val python = if (System.getProperty("os.name").startsWith("Windows")) "python" else "python3"
val hostCoreLibrary = rootProject.layout.buildDirectory.file(
    "bfhost/" + System.mapLibraryName("twitterbf")
).get().asFile

val generateBrainfuck by tasks.registering(Exec::class) {
    group = "brainfuck"
    description = "Lints brainfuck/src and regenerates the AOT C source, ABI constants and memory map."
    workingDir = rootDir
    commandLine(python, "tools/bftool/gen.py")
}

val checkBrainfuck by tasks.registering(Exec::class) {
    group = "brainfuck"
    description = "Fails when the generated C, ABI or memory map files are stale."
    workingDir = rootDir
    inputs.dir(rootProject.file("brainfuck"))
    inputs.dir(rootProject.file("tools/bftool"))
    inputs.dir("src/main/cpp/generated")
    outputs.upToDateWhen { false }
    commandLine(python, "tools/bftool/gen.py", "--check")
}

val buildHostCore by tasks.registering(Exec::class) {
    group = "brainfuck"
    description = "Builds libtwitterbf for the host JVM (parity tests run the shipped AOT C)."
    dependsOn(checkBrainfuck)
    workingDir = rootDir
    inputs.dir("src/main/cpp")
    outputs.file(hostCoreLibrary)
    commandLine(python, "tools/bftool/hostlib.py", System.getProperty("java.home"), hostCoreLibrary.absolutePath)
}

val testBrainfuck by tasks.registering(Exec::class) {
    group = "brainfuck"
    description = "Toolchain, reference-vs-IR-vs-AOT and randomized program tests (Python)."
    dependsOn(checkBrainfuck)
    workingDir = rootDir
    commandLine(python, "-m", "unittest", "discover", "-s", "tests/compiler", "-v")
}

tasks.named("check") {
    dependsOn(checkBrainfuck, testBrainfuck)
}

tasks.withType<Test>().configureEach {
    dependsOn(buildHostCore)
    inputs.file(hostCoreLibrary)
    systemProperty("twitterbf.hostlib", hostCoreLibrary.absolutePath)
    systemProperty("twitterbf.parityCases", project.findProperty("parityCases") ?: "20000")
}
