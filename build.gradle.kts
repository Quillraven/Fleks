plugins {
    buildsrc.plugins.`kmp-js`
    buildsrc.plugins.`kmp-jvm`
    buildsrc.plugins.`kmp-native`
    buildsrc.plugins.publishing
    buildsrc.plugins.benchmark
    id("org.jetbrains.dokka")
}

group = ProjectInfo.GROUP
version = ProjectInfo.VERSION

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
