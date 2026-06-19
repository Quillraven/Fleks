plugins {
    kotlin("jvm")
    buildsrc.plugins.base
    `java-gradle-plugin`
}

dependencies {
    compileOnly(libs.kotlin.compiler)
    implementation(libs.kotlin.gradlePluginApi)

    testImplementation(libs.kctfork.core)
    testImplementation(kotlin("test"))
    testImplementation(project(":fleks-runtime"))
}

gradlePlugin {
    plugins {
        create("fleksCompilerPlugin") {
            id = "${ProjectInfo.GROUP}.compiler-plugin"
            displayName = "Fleks Compiler Plugin"
            description = "Kotlin compiler plugin for Fleks ECS"
            implementationClass = "com.github.quillraven.fleks.plugin.FleksCompilerPlugin"
        }
    }
}
