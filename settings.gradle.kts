rootProject.name = "Fleks"

pluginManagement {
    // versions from gradle.properties
    val kotlinVersion: String by settings
    val dokkaVersion: String by settings
    val benchmarkVersion: String by settings

    plugins {
        kotlin("multiplatform") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion
        id("org.jetbrains.dokka") version dokkaVersion
        id("org.jetbrains.kotlinx.benchmark") version benchmarkVersion
    }
}
