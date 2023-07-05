package buildsrc.plugins

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

/**
 * Base configuration for all Kotlin/Multiplatform conventions.
 *
 * This plugin does not enable any Kotlin target.
 * To enable targets apply a specific specific Kotlin target convention plugins, e.g.
 *
 * ```
 * plugins {
 *   id("buildsrc.plugins.kmp-js")
 * }
 * ```
 */

plugins {
    id("buildsrc.plugins.base")
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}


kotlin {
    jvmToolchain(11)

    // configure all Kotlin/JVM Tests to use JUnit Jupiter
    targets.withType<KotlinJvmTarget>().configureEach {
        testRuns.configureEach {
            executionTask.configure {
                useJUnitPlatform()
            }
        }
    }
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}
