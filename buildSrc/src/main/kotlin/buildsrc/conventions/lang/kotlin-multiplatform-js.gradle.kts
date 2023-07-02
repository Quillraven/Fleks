package buildsrc.conventions.lang

/** conventions for a Kotlin/JS subproject */

plugins {
    id("buildsrc.conventions.lang.kotlin-multiplatform-base")
}

kotlin {
    targets {
        js(IR) {
            browser()
            nodejs()
        }
    }
}
