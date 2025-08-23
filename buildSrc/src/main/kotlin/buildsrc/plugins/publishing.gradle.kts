package buildsrc.plugins

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
    id("com.vanniktech.maven.publish")
    id("org.jetbrains.dokka")
}


//region Publication Properties
// can be set in `$GRADLE_USER_HOME/gradle.properties`, e.g. `mavenCentralPassword=123`
// or environment variables, e.g. `ORG_GRADLE_PROJECT_mavenCentralUsername=abc`
val ossrhUsername: Provider<String> = providers.gradleProperty("mavenCentralUsername")
val ossrhPassword: Provider<String> = providers.gradleProperty("mavenCentralPassword")

val signingKey: Provider<String> = providers.gradleProperty("signingInMemoryKey")
val signingPassword: Provider<String> = providers.gradleProperty("signingInMemoryKeyPassword")
//endregion


//region POM convention
mavenPublishing {
    publishToMavenCentral(automaticRelease = true)

    signAllPublications()

    coordinates(group.toString(), "Fleks", version.toString())

    // pom information needs to be specified per publication
    // because otherwise maven will complain again that
    // information like license, developer or url are missing.
    pom {
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
}
//endregion

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
