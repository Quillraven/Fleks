package buildsrc.conventions

import org.gradle.api.plugins.JavaBasePlugin.DOCUMENTATION_GROUP

/**
 * Conventions for publishing.
 *
 * Mostly focused on Maven Central publishing, which requires
 *
 * * a Javadoc JAR (even if the project is not a Java project)
 * * artifacts are signed (and Gradle's [SigningPlugin] is outdated and does not have good support for lazy config/caching)
 */

plugins {
    signing
    `maven-publish`
}


//region Publication Properties
// can be set in `$GRADLE_USER_HOME/gradle.properties`, e.g. `fleks.ossrhPassword=123`
// or environment variables, e.g. `ORG_GRADLE_PROJECT_fleks.ossrhUsername=abc`
val ossrhUsername = providers.gradleProperty("fleks.ossrhUsername")
val ossrhPassword = providers.gradleProperty("fleks.ossrhPassword")

val signingKey = providers.gradleProperty("fleks.signing.key")
val signingPassword = providers.gradleProperty("fleks.signing.password")

val isReleaseVersion = provider { !version.toString().endsWith("-SNAPSHOT") }

val sonatypeReleaseUrl = isReleaseVersion.map { isRelease ->
    if (isRelease) {
        "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
    } else {
        "https://s01.oss.sonatype.org/content/repositories/snapshots/"
    }
}
//endregion


//region POM convention
publishing {
    publications.withType<MavenPublication>().configureEach {
        // pom information needs to be specified per publication
        // because otherwise maven will complain again that
        // information like license, developer or url are missing.
        pom {
            name.convention("Fleks")
            description.convention("A lightweight entity component system written in Kotlin.")
            url.convention("https://github.com/Quillraven/Fleks")

            scm {
                connection.convention("scm:git:git@github.com:quillraven/fleks.git")
                developerConnection.convention("scm:git:git@github.com:quillraven/fleks.git")
                url.convention("https://github.com/quillraven/fleks/")
            }

            licenses {
                license {
                    name.convention("MIT License")
                    url.convention("https://opensource.org/licenses/MIT")
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
//endregion


//region Maven Central publishing/signing
val javadocJar by tasks.registering(Jar::class) {
    group = DOCUMENTATION_GROUP
    description = "Javadoc Jar"
    // We need to add the javadocJar to every publication because otherwise Maven Central complains.
    // It is not sufficient to only have it in the "root" folder.
    archiveClassifier.set("javadoc")
}

publishing {
    repositories {
        maven(sonatypeReleaseUrl) {
            name = "SonatypeRelease"
            credentials {
                username = ossrhUsername.orNull
                password = ossrhPassword.orNull
            }
        }

        // Publish to a project-local Maven directory, for verification.
        // To test, run:
        // ./gradlew publishAllPublicationsToProjectLocalRepository
        // and check $rootDir/build/maven-project-local
        maven(rootProject.layout.buildDirectory.dir("maven-project-local")) {
            name = "ProjectLocal"
        }
    }

    publications.withType<MavenPublication>().configureEach {
        // Maven Central requires Javadoc
        artifact(javadocJar)
    }
}

signing {
    if (ossrhUsername.isPresent && ossrhPassword.isPresent) {
        logger.lifecycle("publishing.gradle.kts enabled signing for ${project.path}")
        if (signingKey.isPresent && signingPassword.isPresent) {
            useInMemoryPgpKeys(signingKey.get(), signingPassword.get())
        } else {
            useGpgCmd()
        }

        afterEvaluate {
            // Register signatures in afterEvaluate, otherwise the signing plugin creates
            // the signing tasks too early, before all the publications are added.
            signing {
                sign(publishing.publications)
            }
        }
    }
}
//endregion


//region Fix Gradle warning about signing tasks using publishing task outputs without explicit dependencies
// https://youtrack.jetbrains.com/issue/KT-46466
val signingTasks = tasks.withType<Sign>()

tasks.withType<AbstractPublishToMaven>().configureEach {
    mustRunAfter(signingTasks)
}
//endregion


//region publishing logging
tasks.withType<AbstractPublishToMaven>().configureEach {
    val publicationGAV = provider { publication?.run { "$group:$artifactId:$version" } }
    doLast("log publication GAV") {
        if (publicationGAV.isPresent) {
            logger.lifecycle("[task: ${path}] ${publicationGAV.get()}")
        }
    }
}
//endregion
