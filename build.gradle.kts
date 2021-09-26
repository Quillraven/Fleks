plugins {
    kotlin("jvm") version "1.5.31"
    id("org.jetbrains.kotlinx.benchmark") version "0.3.1"
}

group = "com.github.quillraven.fleks"
version = "1.0-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_1_8

val bmSourceSetName = "benchmarks"
sourceSets {
    create(bmSourceSetName) {
        compileClasspath += sourceSets["main"].output
        runtimeClasspath += sourceSets["main"].output
    }
}

configurations {
    getByName("${bmSourceSetName}Implementation") {
        extendsFrom(configurations["implementation"])
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    configurations["${bmSourceSetName}Implementation"]("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:0.3.1")
    configurations["${bmSourceSetName}Implementation"]("com.badlogicgames.ashley:ashley:1.7.3")
    configurations["${bmSourceSetName}Implementation"]("net.onedaybeard.artemis:artemis-odb:2.3.0")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

benchmark {
    targets {
        register(bmSourceSetName)
    }
}
