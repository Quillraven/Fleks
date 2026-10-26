import org.gradle.plugin.compatibility.compatibility

plugins {
    kotlin("jvm")
    id("fleks.base")
    id("fleks.publishing")
    `java-gradle-plugin`
    alias(libs.plugins.gradlePluginPublish)
}

val pluginId = "${providers.gradleProperty("fleks.group").get()}.compiler-plugin"

// the plugin has its own version which is independent of the runtime
version = property("fleks.plugin.version").toString()

dependencies {
    compileOnly(libs.kotlin.compiler)
    compileOnly(libs.kotlin.gradlePluginApi)

    testImplementation(libs.kctfork.core)
    testImplementation(kotlin("test"))
    testImplementation(rootProject)
}

gradlePlugin {
    website = "https://github.com/Quillraven/Fleks"
    vcsUrl = "https://github.com/Quillraven/Fleks.git"
    plugins {
        create("fleksCompilerPlugin") {
            id = pluginId
            displayName = "Fleks Compiler Plugin"
            description = "Kotlin compiler plugin for Fleks that removes the boilerplate of components"
            tags = listOf("kotlin", "ecs", "compiler-plugin", "gamedev")
            compatibility {
                features {
                    // the Gradle plugin only registers the compiler plugin and creates no tasks
                    configurationCache = true
                }
            }
            implementationClass = "com.github.quillraven.fleks.plugin.FleksGradlePlugin"
        }
    }
}

mavenPublishing {
    coordinates(
        providers.gradleProperty("fleks.group").get(),
        providers.gradleProperty("fleks.plugin.artifact").get(),
        version.toString(),
    )
    pom {
        name.set("Fleks Compiler Plugin")
        description.set("Kotlin compiler plugin for Fleks that removes the boilerplate of components")
    }
}

tasks.processResources {
    val props = mapOf(
        "group" to providers.gradleProperty("fleks.group").get(),
        "artifact" to providers.gradleProperty("fleks.plugin.artifact").get(),
        "version" to version.toString(),
    )
    inputs.properties(props)
    filesMatching("fleks-compiler-plugin.properties") { expand(props) }
}
