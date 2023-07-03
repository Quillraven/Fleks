package buildsrc.conventions.lang

plugins {
    id("buildsrc.conventions.lang.kotlin-multiplatform-base")
}

kotlin {
    jvm {
        withJava()
    }
}
