import kotlinx.benchmark.gradle.KotlinJvmBenchmarkTarget
import kotlinx.benchmark.gradle.benchmark

plugins {
    buildsrc.plugins.`kmp-js`
    buildsrc.plugins.`kmp-jvm`
    buildsrc.plugins.`kmp-native`
    buildsrc.plugins.publishing
    id("org.jetbrains.kotlinx.benchmark")
    id("org.jetbrains.dokka")
}

group = "io.github.quillraven.fleks"
version = "2.8-SNAPSHOT"

kotlin {
    jvm {
        compilations {
            val main by getting

            // custom benchmark compilation
            val benchmarks by creating { associateWith(main) }
            benchmark.targets.add(
                KotlinJvmBenchmarkTarget(benchmark, benchmarks.defaultSourceSet.name, benchmarks)
            )
        }
    }

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
        val jvmBenchmarks by getting {
            dependencies {
                implementation(libs.kotlinxBenchmark.runtime)
                implementation(libs.ashley)
                implementation(libs.artemisOdb)
            }
        }
    }
}

benchmark {
    configurations {
        create("FleksOnly") {
            exclude("Artemis|Ashley")
        }

        create("FleksAddRemoveOnly") {
            include("addRemove")
            exclude("Artemis|Ashley")
        }

        create("FleksSimpleOnly") {
            include("simple")
            exclude("Artemis|Ashley")
        }

        create("FleksComplexOnly") {
            include("complex")
            exclude("Artemis|Ashley")
        }
    }
}

tasks.javadocJar {
    from(tasks.dokkaHtml)
}
