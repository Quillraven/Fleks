@file:Suppress("UNUSED_VARIABLE")

import kotlinx.benchmark.gradle.KotlinJvmBenchmarkTarget

plugins {
    buildsrc.plugins.`kmp-js`
    buildsrc.plugins.`kmp-jvm`
    buildsrc.plugins.`kmp-native`
    buildsrc.plugins.publishing
    id("org.jetbrains.kotlinx.benchmark")
    id("org.jetbrains.dokka")
}

group = "io.github.quillraven.fleks"
version = "2.5-SNAPSHOT"

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

tasks.javadocJar {
    from(tasks.dokkaHtml)
}
