package buildsrc.conventions.lang

plugins {
    id("buildsrc.conventions.lang.kotlin-multiplatform-base")
}

kotlin {
    jvmToolchain(11)
    jvm {
        withJava()
    }
}
