plugins {
    kotlin("jvm") version "1.6.20"
    id("org.jetbrains.kotlinx.benchmark") version "0.4.2"
    id("org.jetbrains.dokka") version "1.6.10"
    `maven-publish`
    signing
}

group = "io.github.quillraven.fleks"
version = "1.0-JVM"
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

    testImplementation("org.jetbrains.kotlin:kotlin-test:1.6.0")

    configurations["${bmSourceSetName}Implementation"]("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:0.4.1")
    configurations["${bmSourceSetName}Implementation"]("com.badlogicgames.ashley:ashley:1.7.3")
    configurations["${bmSourceSetName}Implementation"]("net.onedaybeard.artemis:artemis-odb:2.3.0")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

tasks.test {
    useJUnitPlatform()
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

artifacts {
    archives(dokkaJavadocJar)
    archives(sourcesJar)
}

val publicationName = "mavenFleks"
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
        register<MavenPublication>(publicationName) {
            version = project.version.toString()
            groupId = project.group.toString()
            artifactId = "Fleks"

            from(components["kotlin"])
            artifact(dokkaJavadocJar)
            artifact(sourcesJar)

            pom {
                name.set("Fleks")
                packaging = "jar"
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
        }
    }
}

signing {
    useInMemoryPgpKeys(System.getenv("SIGNING_KEY"), System.getenv("SIGNING_PASSWORD"))
    sign(publishing.publications[publicationName])
}

benchmark {
    targets {
        register(bmSourceSetName)
    }
}
