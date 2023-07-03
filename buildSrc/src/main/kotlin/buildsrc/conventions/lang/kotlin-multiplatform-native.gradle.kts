package buildsrc.conventions.lang

/** conventions for a Kotlin/Native subproject */

plugins {
    id("buildsrc.conventions.lang.kotlin-multiplatform-base")
}

kotlin {

    // Native targets all extend commonMain and commonTest
    //
    // common/
    // └── native/
    //     ├── linuxX64
    //     ├── mingwX64
    //     ├── macosX64
    //     ├── macosArm64
    //     ├── ios/ (shortcut)
    //     │   ├── iosArm64
    //     │   ├── iosX64
    //     │   └── iosSimulatorArm64
    //     ├── tvos/ (shortcut)
    //     │   ├── tvosArm64
    //     │   ├── tvosX64
    //     │   └── tvosSimulatorArm64Main
    //     └── watchos/ (shortcut)
    //         ├── watchosArm32
    //         ├── watchosArm64
    //         ├── watchosX64
    //         └── watchosSimulatorArm64Main
    //
    // More specialised targets are disabled. They can be enabled, if there is demand for them - just make sure
    // to add `dependsOn(nativeMain)` / `dependsOn(nativeTest)` below for any new targets.

    linuxX64()
    linuxArm64()

    mingwX64()

    macosArm64()
    macosX64()


    // https://kotlinlang.org/docs/multiplatform-hierarchy.html#target-shortcuts
    //ios()     // iosArm64, iosX64
    //watchos() // watchosArm32, watchosArm64, watchosX64
    //tvos()    // tvosArm64, tvosX64
    //iosSimulatorArm64()
    //watchosSimulatorArm64()
    //tvosSimulatorArm64()
    //androidNativeArm32()
    //androidNativeArm64()
    //androidNativeX86()
    //androidNativeX64()
    //watchosDeviceArm64()

    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val commonMain by getting {}
        val commonTest by getting {}

        val nativeMain by creating { dependsOn(commonMain) }
        val nativeTest by creating { dependsOn(commonTest) }

        // Linux
        val linuxX64Main by getting { dependsOn(nativeMain) }
        val linuxX64Test by getting { dependsOn(nativeTest) }
        val linuxArm64Main by getting { dependsOn(nativeMain) }
        val linuxArm64Test by getting { dependsOn(nativeTest) }

        // Windows - MinGW
        val mingwX64Main by getting { dependsOn(nativeMain) }
        val mingwX64Test by getting { dependsOn(nativeTest) }

        // Apple - macOS
        val macosArm64Main by getting { dependsOn(nativeMain) }
        val macosArm64Test by getting { dependsOn(nativeTest) }

        val macosX64Main by getting { dependsOn(nativeMain) }
        val macosX64Test by getting { dependsOn(nativeTest) }
    }
}
