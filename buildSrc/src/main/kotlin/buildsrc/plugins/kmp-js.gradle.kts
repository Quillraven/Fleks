package buildsrc.plugins

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask

/** conventions for a Kotlin/JS subproject */

plugins {
    id("buildsrc.plugins.kmp-base")
}

kotlin {
    js(IR) {
        browser()
        nodejs()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        nodejs()
    }
}

// https://youtrack.jetbrains.com/issue/KT-78504
tasks.withType<KotlinNpmInstallTask>().configureEach {
    args.addAll(listOf("--network-concurrency", "1", "--mutex", "network"))
}
