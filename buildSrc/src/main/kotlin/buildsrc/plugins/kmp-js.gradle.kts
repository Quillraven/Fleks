package buildsrc.plugins

/** conventions for a Kotlin/JS subproject */

plugins {
    id("buildsrc.plugins.kmp-base")
}

kotlin {
    targets {
        js(IR) {
            browser()
            nodejs()
        }
    }
}
