package fleks

import kotlinx.benchmark.gradle.KotlinJvmBenchmarkTarget
import kotlinx.benchmark.gradle.benchmark
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi

plugins {
    id("fleks.kmp-jvm")
    id("org.jetbrains.kotlinx.benchmark")
}

kotlin {
    jvm {
        compilations {
            val main = getByName("main")

            // custom benchmark compilation
            val benchmarks = create("benchmarks") { associateWith(main) }
            @OptIn(KotlinxBenchmarkPluginInternalApi::class)
            benchmark.targets.add(
                KotlinJvmBenchmarkTarget(benchmark, benchmarks.defaultSourceSet.name, benchmarks)
            )
        }
    }

    sourceSets {
        val jvmBenchmarks = getByName("jvmBenchmarks") {
            dependencies {
                implementation(versionCatalogs.named("libs").findLibrary("kotlinxBenchmark.runtime").orElseThrow(::AssertionError))
                implementation(versionCatalogs.named("libs").findLibrary("ashley").orElseThrow(::AssertionError))
                implementation(versionCatalogs.named("libs").findLibrary("artemisOdb").orElseThrow(::AssertionError))
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

        create("FleksArtemisOnly") {
            include("simple|complex")
            exclude("Ashley")
        }
    }
}
