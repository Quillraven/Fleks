package com.github.quillraven.fleks.plugin

import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import java.util.Properties

/**
 * Gradle plugin that registers the [FleksCompilerPlugin] to all Kotlin compilations of a project.
 * The compiler plugin is part of the same artifact.
 */
class FleksGradlePlugin : KotlinCompilerPluginSupportPlugin {
    // coordinates are filled in by the build via processResources
    private val coordinates = Properties().apply {
        FleksGradlePlugin::class.java.getResourceAsStream("/fleks-compiler-plugin.properties")!!.use(::load)
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun getCompilerPluginId(): String = FLEKS_PLUGIN_ID

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = coordinates.getProperty("group"),
        artifactId = coordinates.getProperty("artifact"),
        version = coordinates.getProperty("version"),
    )

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> =
        kotlinCompilation.target.project.provider { emptyList() }
}
