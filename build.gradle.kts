plugins {
    kotlin("jvm") version "1.5.31"
    id("org.jetbrains.kotlinx.benchmark") version "0.3.1"
    id("org.jetbrains.dokka") version "1.5.30"
    `maven-publish`
}

group = "com.github.quillraven.fleks"
version = "preRelease-20211118"
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

    configurations["${bmSourceSetName}Implementation"]("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:0.3.1")
    configurations["${bmSourceSetName}Implementation"]("com.badlogicgames.ashley:ashley:1.7.3")
    configurations["${bmSourceSetName}Implementation"]("net.onedaybeard.artemis:artemis-odb:2.3.0")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

val dokkaJavadocJar = tasks.create<Jar>("jarDokkaJavadoc") {
    group = "build"

    dependsOn(tasks.dokkaJavadoc)
    from(tasks.dokkaJavadoc.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
}

val sourcesJar = tasks.create<Jar>("jarSources") {
    group = "build"

    from(sourceSets.main.get().allSource)
    archiveClassifier.set("sources")
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/Quillraven/Fleks/")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }

    publications {
        register<MavenPublication>("mavenFleks") {
            from(components["kotlin"])
            version = project.version.toString()
            groupId = project.group.toString()
            artifactId = "Fleks"
            artifact(dokkaJavadocJar)
            artifact(sourcesJar)
        }
    }
}

benchmark {
    targets {
        register(bmSourceSetName)
    }
}
