package buildsrc.plugins

/** conventions for a Kotlin/Native subproject */

plugins {
    id("buildsrc.plugins.kmp-base")
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
    // More specialised targets are disabled. They can be enabled, if there is demand for them.

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
}
