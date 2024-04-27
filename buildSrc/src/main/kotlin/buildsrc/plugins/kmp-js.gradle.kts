package buildsrc.plugins

import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
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

    wasmJs {
        browser()
        nodejs()
    }
}

// https://youtrack.jetbrains.com/issue/KT-63014/
// CompileError: WebAssembly.Module(): invalid value type 0x71
rootProject.the<NodeJsRootExtension>().apply {
    nodeVersion = "21.0.0-v8-canary202309143a48826a08"
    nodeDownloadBaseUrl = "https://nodejs.org/download/v8-canary"
    download = false
}
tasks.withType<KotlinNpmInstallTask>().configureEach {
    args.add("--ignore-engines")
}
