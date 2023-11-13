package buildsrc.plugins

/** conventions for a Kotlin/JS subproject */

plugins {
    id("buildsrc.plugins.kmp-base")
}

kotlin {
    js(IR) {
        browser()
        nodejs()
    }
}
