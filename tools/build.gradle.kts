// Offline generators: the design takes, and later the launcher icon and store
// art. Plain Kotlin on the JVM, no Android, no third-party libraries: PNGs are
// written with ImageIO. Generated assets are committed; these tasks only run
// when a design deliberately changes.
plugins {
    kotlin("jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // The rules, so generated art draws the same clock the app draws.
    implementation(project(":core"))
    testImplementation("junit:junit:4.13.2")
}

tasks.register<JavaExec>("makeTakes") {
    group = "tools"
    description = "Render the dial design takes into build/takes, for review."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "io.github.muntasimulhaque.readoclock.tools.MakeTakesKt"
    args = listOf(rootDir.absolutePath)
}

tasks.register<JavaExec>("makeIcons") {
    group = "tools"
    description = "Regenerate the launcher icon set in app/src/main/res."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "io.github.muntasimulhaque.readoclock.tools.MakeIconsKt"
    args = listOf(rootDir.absolutePath)
}

tasks.register<JavaExec>("checkIcons") {
    group = "tools"
    description = "Verify the committed launcher icons match a fresh regeneration."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "io.github.muntasimulhaque.readoclock.tools.MakeIconsKt"
    args = listOf(rootDir.absolutePath, "--check")
}

tasks.register<JavaExec>("makeTick") {
    group = "tools"
    description = "Regenerate the quartz tick WAV in app/src/main/res/raw."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "io.github.muntasimulhaque.readoclock.tools.MakeTickKt"
    args = listOf(rootDir.absolutePath)
}

tasks.register<JavaExec>("checkTick") {
    group = "tools"
    description = "Verify the committed tick WAV matches a fresh regeneration."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "io.github.muntasimulhaque.readoclock.tools.MakeTickKt"
    args = listOf(rootDir.absolutePath, "--check")
}

tasks.register<JavaExec>("makeArt") {
    group = "tools"
    description = "Regenerate the store art (feature graphic, store icon)."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "io.github.muntasimulhaque.readoclock.tools.MakeArtKt"
    args = listOf(rootDir.absolutePath)
}
