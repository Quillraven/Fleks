@file:Suppress("UNUSED_VARIABLE")

import kotlinx.benchmark.gradle.KotlinJvmBenchmarkTarget

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("org.jetbrains.kotlinx.benchmark")
    id("org.jetbrains.dokka")
    `maven-publish`
    signing
}

group = "io.github.quillraven.fleks"
version = "2.3"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
    mavenCentral()
}

kotlin {
    targets {
        jvm {
            compilations {
                all { kotlinOptions { jvmTarget = "11" } }
                val main by getting

                // custom benchmark compilation
                val benchmarks by creating { associateWith(main) }
                benchmark.targets.add(
                    KotlinJvmBenchmarkTarget(benchmark, benchmarks.defaultSourceSet.name, benchmarks)
                )
            }
            withJava()
            testRuns["test"].executionTask.configure {
                useJUnitPlatform()
            }
        }
    }
    js(IR) {
        browser { }
    }
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64("native")
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${project.properties["serializationVersion"]}")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting
        val jvmTest by getting
        val jvmBenchmarks by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:${project.properties["benchmarkVersion"]}")
                implementation("com.badlogicgames.ashley:ashley:1.7.4")
                implementation("net.onedaybeard.artemis:artemis-odb:2.3.0")
            }
        }
        val jsMain by getting
        val jsTest by getting
        val nativeMain by getting
        val nativeTest by getting
    }
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    from(tasks.dokkaHtml)
}

publishing {
    repositories {
        maven {
            url = if (project.version.toString().endsWith("SNAPSHOT")) {
                uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            } else {
                uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            }

            credentials {
                username = System.getenv("OSSRH_USERNAME")
                password = System.getenv("OSSRH_TOKEN")
            }
        }
    }

    publications {
        val kotlinMultiplatform by getting(MavenPublication::class) {
            // we need to keep this block up here because
            // otherwise the different target folders like js/jvm/native are not created
            version = project.version.toString()
            groupId = project.group.toString()
            artifactId = "Fleks"
        }
    }

    publications.forEach {
        if (it !is MavenPublication) {
            return@forEach
        }

        // We need to add the javadocJar to every publication
        // because otherwise maven is complaining.
        // It is not sufficient to only have it in the "root" folder.
        it.artifact(javadocJar)

        // pom information needs to be specified per publication
        // because otherwise maven will complain again that
        // information like license, developer or url are missing.
        it.pom {
            name.set("Fleks")
            description.set("A lightweight entity component system written in Kotlin.")
            url.set("https://github.com/Quillraven/Fleks")

            scm {
                connection.set("scm:git:git@github.com:quillraven/fleks.git")
                developerConnection.set("scm:git:git@github.com:quillraven/fleks.git")
                url.set("https://github.com/quillraven/fleks/")
            }

            licenses {
                license {
                    name.set("MIT License")
                    url.set("https://opensource.org/licenses/MIT")
                }
            }

            developers {
                developer {
                    id.set("Quillraven")
                    name.set("Simon Klausner")
                    email.set("quillraven@gmail.com")
                }
            }
        }

        signing {
            useInMemoryPgpKeys(System.getenv("SIGNING_KEY"), System.getenv("SIGNING_PASSWORD"))
            sign(it)
        }
    }
}

// only sign if version is not a SNAPSHOT release.
// this makes it easier to publish to mavenLocal and test the packed version.
tasks.withType<Sign>().configureEach {
    onlyIf { !project.version.toString().endsWith("SNAPSHOT") }
}
