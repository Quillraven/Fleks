package buildsrc.plugins

import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl

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
