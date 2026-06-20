package buildsrc.plugins

plugins {
    id("buildsrc.plugins.kmp-js")
    id("buildsrc.plugins.kmp-jvm")
    id("buildsrc.plugins.kmp-native")
    id("org.jetbrains.dokka")
}

kotlin {
    sourceSets {
        all {
            // WASM: for bitArray.kt Long::countLeadingZeroBits
            languageSettings.optIn("kotlin.ExperimentalStdlibApi")
        }
    }
}
