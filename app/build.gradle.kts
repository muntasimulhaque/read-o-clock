import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// The upload keystore's path and credentials live in keystore.properties in
// the owner's vault, outside every repo, so no credential ever enters the
// repository. Read-o-Clock uses the shared upload keystore already used by
// the other apps (docs/decisions.md D-007); a per-app keystore would win if
// one is ever created. CI instead provides the keystore through
// KEYSTORE_FILE and the three secrets. When neither is present (a fresh
// clone) the release build degrades to unsigned rather than failing.
//
// The vault is one Google Drive folder, and Drive mounts it under a
// different letter on each machine (D: on one, E: on another), so the
// drive letter is searched, never assumed. Hard-coding it once made a
// release build here come out unsigned while looking signed, because the
// build still succeeds, just without credentials.
val vaultPaths = listOf(
    "BSCPLC/DM (Development)/Personal Docs/Pers/My Apps/Google Play Signing Key/Read-o-Clock/keystore.properties",
    "BSCPLC/DM (Development)/Personal Docs/Pers/Google Play Signing Key/Read-o-Clock/keystore.properties",
    "BSCPLC/DM (Development)/Personal Docs/Pers/My Apps/Google Play Signing Key/keystore.properties",
)
val vaultDrives = listOf("C", "D", "E", "F", "G", "H")
val keystoreFile = vaultDrives
    .asSequence()
    .flatMap { drive -> vaultPaths.asSequence().map { "$drive:/GDrive/$it" } }
    .map(::file)
    .firstOrNull { it.exists() }

if (keystoreFile != null) {
    logger.lifecycle("Signing with the vault keystore at ${keystoreFile.absolutePath}")
} else {
    logger.lifecycle("No vault keystore found; the release build will be unsigned.")
}

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

val uploadStoreFile: java.io.File? = System.getenv("KEYSTORE_FILE")
    ?.takeIf { it.isNotBlank() }
    ?.let { file(it) } ?: keystoreStoreFile
val uploadStorePassword: String? = System.getenv("KEYSTORE_PASSWORD") ?: releaseKeystore.getProperty("storePassword")
val uploadKeyAlias: String? = System.getenv("KEY_ALIAS") ?: releaseKeystore.getProperty("keyAlias")
val uploadKeyPassword: String? = System.getenv("KEY_PASSWORD") ?: releaseKeystore.getProperty("keyPassword")
val canSignRelease = uploadStoreFile?.exists() == true &&
    !uploadStorePassword.isNullOrEmpty() &&
    !uploadKeyAlias.isNullOrEmpty() &&
    !uploadKeyPassword.isNullOrEmpty()

android {
    namespace = "io.github.muntasimulhaque.readoclock"
    compileSdk = 37

    defaultConfig {
        // Play ties an app to its first package ID forever.
        applicationId = "io.github.muntasimulhaque.readoclock"
        minSdk = 24
        targetSdk = 37
        versionCode = 5
        versionName = "0.5"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (canSignRelease) {
            create("release") {
                storeFile = uploadStoreFile
                storePassword = uploadStorePassword
                keyAlias = uploadKeyAlias
                keyPassword = uploadKeyPassword
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

    // Instrumented (emulator) screenshot capture: a bare ComponentActivity
    // hosts each scene and PixelCopy grabs the window.
    androidTestImplementation("androidx.test:core:1.7.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
}
