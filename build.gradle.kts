@file:Suppress("UNUSED_VARIABLE")

import kotlinx.benchmark.gradle.KotlinJvmBenchmarkTarget

plugins {
    buildsrc.conventions.lang.`kotlin-multiplatform-js`
    buildsrc.conventions.lang.`kotlin-multiplatform-jvm`
    buildsrc.conventions.lang.`kotlin-multiplatform-native`
    buildsrc.conventions.publishing
    buildsrc.conventions.`git-branch-publish`
    kotlin("plugin.serialization")
    id("org.jetbrains.kotlinx.benchmark")
    id("org.jetbrains.dokka")
}

group = "io.github.quillraven.fleks"
version = "2.3"

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
    // disable Dokka, it's slow less useful than the sources.jar, which is also published
    //from(tasks.dokkaHtml)
}
