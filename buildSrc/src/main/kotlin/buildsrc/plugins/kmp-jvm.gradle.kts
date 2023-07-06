package buildsrc.plugins

/** conventions for a Kotlin/JVM subproject */

plugins {
    id("buildsrc.plugins.kmp-base")
}

kotlin {
    jvm {
        withJava()
    }
}
