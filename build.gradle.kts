plugins {
    id("fleks.kmp-js")
    id("fleks.kmp-jvm")
    id("fleks.kmp-native")
    id("fleks.publishing")
    id("fleks.benchmark")
}

group = providers.gradleProperty("fleks.group").get()
version = providers.gradleProperty("fleks.version").get()

kotlin {
    sourceSets {
        all {
            // WASM: for bitArray.kt Long::countLeadingZeroBits
            languageSettings.optIn("kotlin.ExperimentalStdlibApi")
        }
        commonMain {
            dependencies {
                implementation(libs.kotlinxSerialization.json)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
