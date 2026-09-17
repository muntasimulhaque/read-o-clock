import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// The upload keystore's path and credentials live in keystore.properties in
// the owner's vault, outside every repo, so no credential ever enters the
// repository. When the file is absent (CI, a fresh clone) the release build
// degrades to unsigned rather than failing.
val keystoreLayouts = listOf(
    "BSCPLC/DM (Development)/Personal Docs/Pers/My Apps/Google Play Signing Key/Read-o-Clock/keystore.properties",
    "BSCPLC/DM (Development)/Personal Docs/Pers/Google Play Signing Key/Read-o-Clock/keystore.properties",
)
val keystoreFile = keystoreLayouts
    .map { file("E:/GDrive/$it") }
    .firstOrNull { it.exists() }

val releaseKeystore = Properties()
if (keystoreFile != null) {
    releaseKeystore.load(keystoreFile.inputStream())
}

// The storeFile inside keystore.properties carries a drive letter that may not
// match this machine; if the literal path does not exist, resolve the store
// file against the properties file's own directory instead.
val keystoreStoreFile: java.io.File? = run {
    val kf = keystoreFile ?: return@run null
    val raw = releaseKeystore.getProperty("storeFile") ?: return@run null
    val literal = file(raw)
    if (literal.exists()) literal
    else {
        val name = raw.substringAfterLast('/').substringAfterLast('\\')
        file("${kf.parentFile.absolutePath}/$name")
    }
}
val canSignRelease = keystoreStoreFile != null &&
    releaseKeystore.containsKey("storePassword") &&
    releaseKeystore.containsKey("keyAlias") &&
    releaseKeystore.containsKey("keyPassword")

android {
    namespace = "io.github.muntasimulhaque.readoclock"
    compileSdk = 37

    defaultConfig {
        // Play ties an app to its first package ID forever.
        applicationId = "io.github.muntasimulhaque.readoclock"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "0.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (canSignRelease) {
            create("release") {
                storeFile = keystoreStoreFile
                storePassword = releaseKeystore.getProperty("storePassword")
                keyAlias = releaseKeystore.getProperty("keyAlias")
                keyPassword = releaseKeystore.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (canSignRelease) signingConfigs.getByName("release") else null
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    lint {
        abortOnError = true
        checkDependencies = false
    }

    // Play encodes a dependency manifest into every artifact by default. The
    // app ships nothing it needs to advertise there, so leave the block out
    // of the APK and the AAB entirely.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    // The rules: pure Kotlin, shared with the offline generators in :tools.
    implementation(project(":core"))

    val composeBom = platform("androidx.compose:compose-bom:2026.06.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    // The host is a ViewModel; the frame loop runs only while the app is
    // resumed, and the dial speaks through StateDescription.
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")

    testImplementation("junit:junit:4.13.2")
}
