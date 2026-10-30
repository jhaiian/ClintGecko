import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { stream ->
        localProperties.load(stream)
    }
}

val ccacheExecutable: String? = (System.getenv("PATH") ?: "")
    .split(File.pathSeparator)
    .filter { it.isNotEmpty() }
    .flatMap { dir -> listOf(File(dir, "ccache"), File(dir, "ccache.exe")) }
    .firstOrNull { it.isFile && it.canExecute() }
    ?.absolutePath

android {
    namespace = "com.jhaiian.clint"
    compileSdk {
        version = release(37) {
            minorApiLevel = 2
        }
    }

    ndkVersion = "27.2.12479018"

    defaultConfig {
        applicationId = "com.jhaiian.clint.gecko"
        minSdk = 26
        targetSdk = 37
        versionCode = 6
        versionName = "1.0.5"

        externalNativeBuild {
            cmake {
                arguments.addAll(
                    listOf(
                        "-DANDROID_STL=c++_static",
                        "-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON"
                    )
                )
                if (ccacheExecutable != null) {
                    arguments.add("-DCMAKE_CXX_COMPILER_LAUNCHER=$ccacheExecutable")
                }
                abiFilters("arm64-v8a", "armeabi-v7a", "x86_64")
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = rootProject.file("native/websiteblocker-cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    val hasSigningConfig = localProperties.getProperty("signingConfig.storePassword") != null

    if (hasSigningConfig) {
        signingConfigs {
            create("release") {
                storeFile = rootProject.file(localProperties.getProperty("signingConfig.storeFile", "app/release_keystore.jks"))
                storePassword = localProperties.getProperty("signingConfig.storePassword")
                keyAlias = localProperties.getProperty("signingConfig.keyAlias")
                keyPassword = localProperties.getProperty("signingConfig.keyPassword")
            }
        }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    flavorDimensions += "distribution"

    productFlavors {
        create("github") {
            dimension = "distribution"
            buildConfigField("boolean", "IS_FOSS", "false")
        }
        create("foss") {
            dimension = "distribution"
            buildConfigField("boolean", "IS_FOSS", "true")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasSigningConfig) {
                signingConfig = signingConfigs["release"]
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes += listOf("META-INF/INDEX.LIST", "META-INF/io.netty.versions.properties")
        }
    }

    bundle {
        language {
            enableSplit = false
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.documentfile:documentfile:1.1.0")
    implementation("androidx.appcompat:appcompat:1.8.0")
    implementation("com.google.android.material:material:1.14.0")
    implementation("org.mozilla.geckoview:geckoview:159.0.20261022150525")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.2.0")
    implementation("com.squareup.okhttp3:okhttp:5.5.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("io.noties.markwon:core:4.6.2")
    implementation("io.noties.markwon:image:4.6.2")
    implementation("com.j256.simplemagic:simplemagic:1.17")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("com.caverock:androidsvg-aar:1.4")
    implementation("androidx.activity:activity-ktx:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-service:2.11.0")
    implementation("androidx.work:work-runtime-ktx:2.12.0")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("org.bouncycastle:bcprov-jdk18on:1.86")
    implementation("androidx.media3:media3-common:1.11.1")
    implementation("androidx.media3:media3-muxer:1.11.1")
    implementation("androidx.media3:media3-exoplayer:1.11.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.11.1")
    implementation("androidx.media3:media3-exoplayer-dash:1.11.1")
    implementation("androidx.media3:media3-datasource-okhttp:1.11.1")
    implementation("androidx.media3:media3-ui:1.11.1")

    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.github.skydoves:colorpicker-compose:1.3.0")
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("androidx.compose.material3.adaptive:adaptive")
    implementation("androidx.compose.material3.adaptive:adaptive-layout")
    implementation("androidx.compose.material3.adaptive:adaptive-navigation")
    implementation("androidx.activity:activity-compose:1.13.0")
}
