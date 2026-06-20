package buildsrc.plugins

import buildsrc.library
import kotlinx.benchmark.gradle.KotlinJvmBenchmarkTarget
import kotlinx.benchmark.gradle.benchmark
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi

plugins{
    id("buildsrc.plugins.kmp-jvm")
    id("org.jetbrains.kotlinx.benchmark")
}

kotlin {
    jvm {
        compilations {
            val main by getting

            // custom benchmark compilation
            val benchmarks by creating { associateWith(main) }
            @OptIn(KotlinxBenchmarkPluginInternalApi::class)
            benchmark.targets.add(
                KotlinJvmBenchmarkTarget(benchmark, benchmarks.defaultSourceSet.name, benchmarks)
            )
        }
    }

    sourceSets {
        val jvmBenchmarks by getting {
            dependencies {
                implementation(library("kotlinxBenchmark-runtime"))
                implementation(library("ashley"))
                implementation(library("artemisOdb"))
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
