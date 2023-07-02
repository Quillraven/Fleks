package buildsrc.conventions.lang

/** conventions for a Kotlin/Native subproject */

plugins {
    id("buildsrc.conventions.lang.kotlin-multiplatform-base")
}

kotlin {

    // Native targets all extend commonMain and commonTest.
    //
    // Some targets (ios, tvos, watchos) are shortcuts provided by the Kotlin DSL, that
    // provide additional targets, except for 'simulators' which must be defined manually.
    // https://kotlinlang.org/docs/multiplatform-share-on-platforms.html#use-target-shortcuts
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

    linuxX64()

    mingwX64()

    macosX64()
    macosArm64()

    // More specialised targets are disabled for now, since I anticipate low demand.
    // They can be re-enabled, if there is demand for them.
    // https://kotlinlang.org/docs/multiplatform-share-on-platforms.html#use-target-shortcuts
    //ios()     // iosArm64, iosX64
    //watchos() // watchosArm32, watchosArm64, watchosX64
    //tvos()    // tvosArm64, tvosX64

    //iosSimulatorArm64()
    //tvosSimulatorArm64()
    //watchosSimulatorArm64()

    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val commonMain by getting {}
        val commonTest by getting {}

        val nativeMain by creating { dependsOn(commonMain) }
        val nativeTest by creating { dependsOn(commonTest) }

        // Linux
        val linuxX64Main by getting { dependsOn(nativeMain) }
        val linuxX64Test by getting { dependsOn(nativeTest) }

        // Windows - MinGW
        val mingwX64Main by getting { dependsOn(nativeMain) }
        val mingwX64Test by getting { dependsOn(nativeTest) }

        // Apple - macOS
        val macosArm64Main by getting { dependsOn(nativeMain) }
        val macosArm64Test by getting { dependsOn(nativeTest) }

        val macosX64Main by getting { dependsOn(nativeMain) }
        val macosX64Test by getting { dependsOn(nativeTest) }

        //// Apple - iOS
        //val iosMain by getting { dependsOn(nativeMain) }
        //val iosTest by getting { dependsOn(nativeTest) }

        //val iosSimulatorArm64Main by getting { dependsOn(iosMain) }
        //val iosSimulatorArm64Test by getting { dependsOn(iosTest) }

        //// Apple - tvOS
        //val tvosMain by getting { dependsOn(nativeMain) }
        //val tvosTest by getting { dependsOn(nativeTest) }

        //val tvosSimulatorArm64Main by getting { dependsOn(tvosMain) }
        //val tvosSimulatorArm64Test by getting { dependsOn(tvosTest) }

        //// Apple - watchOS
        //val watchosMain by getting { dependsOn(nativeMain) }
        //val watchosTest by getting { dependsOn(nativeTest) }

        //val watchosSimulatorArm64Main by getting { dependsOn(watchosMain) }
        //val watchosSimulatorArm64Test by getting { dependsOn(watchosTest) }

        // val iosArm32Main by getting { dependsOn(desktopMain) }
        // val iosArm32Test by getting { dependsOn(nativeTest) }
    }
}
