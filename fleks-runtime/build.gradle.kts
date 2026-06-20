plugins {
    id("buildsrc.plugins.kmp-library")
    id("buildsrc.plugins.publishing")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinxSerialization.json)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
