plugins {
    buildsrc.plugins.`kmp-js`
    buildsrc.plugins.`kmp-jvm`
    buildsrc.plugins.`kmp-native`
    buildsrc.plugins.publishing
    buildsrc.plugins.benchmark
}

group = "io.github.quillraven.fleks"
version = "2.12-SNAPSHOT"

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

tasks.javadocJar {
    from(tasks.named("dokkaGeneratePublicationHtml"))
}
