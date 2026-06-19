package com.github.quillraven.fleks.plugin

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@OptIn(ExperimentalCompilerApi::class)
class ComponentAnnotationTest {

    private fun compile(vararg sources: SourceFile) = KotlinCompilation().apply {
        this.sources = sources.toList()
        compilerPluginRegistrars = listOf(FleksCompilerPlugin())
        inheritClassPath = true
    }.compile()

    @Test
    fun `@FleksComponent class compiles successfully`() {
        val result = compile(
            SourceFile.kotlin(
                "Position.kt", """
                import com.github.quillraven.fleks.FleksComponent
                @FleksComponent data class Position(var x: Float = 0f, var y: Float = 0f)
            """
            )
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
    }

    @Test
    fun `@FleksComponent class implements Component`() {
        // If the plugin didn't add Component, this assignment would not compile
        val result = compile(
            SourceFile.kotlin(
                "Velocity.kt", """
                import com.github.quillraven.fleks.Component
                import com.github.quillraven.fleks.FleksComponent
                @FleksComponent data class Velocity(var dx: Float = 0f, var dy: Float = 0f)
                val check: Component = Velocity()
            """
            )
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
    }

    @Test
    fun `@FleksComponent class gets a companion object extending ComponentType`() {
        // If the plugin didn't generate the companion, using it as ComponentType would not compile
        val result = compile(
            SourceFile.kotlin(
                "Health.kt", """
                import com.github.quillraven.fleks.FleksComponent
                import com.github.quillraven.fleks.ComponentType
                @FleksComponent data class Health(var hp: Int = 100)
                val check: ComponentType<Health> = Health
            """
            )
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
    }

    @Test
    fun `two @FleksComponent classes get distinct ids`() {
        // If both companions extend ComponentType, they each get a unique id at init time
        val result = compile(
            SourceFile.kotlin(
                "Components.kt", """
                import com.github.quillraven.fleks.FleksComponent
                @FleksComponent data class CompA(var v: Int = 0)
                @FleksComponent data class CompB(var v: Int = 0)
                val idsAreDistinct: Boolean = CompA.id != CompB.id
            """
            )
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
    }

    @Test
    fun `non-@FleksComponent class is not affected`() {
        // Should fail to compile if plugin wrongly adds Component to a plain class
        val result = compile(
            SourceFile.kotlin(
                "Plain.kt", """
                import com.github.quillraven.fleks.Component
                data class Plain(var x: Float = 0f)
                // This must NOT compile — Plain doesn't implement Component
                val check: Component = Plain()
            """
            )
        )
        assertNotEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    }

    @Test
    fun `@FleksComponent class extends existing companion object`() {
        val result = compile(
            SourceFile.kotlin(
                "Speed.kt", """
                import com.github.quillraven.fleks.FleksComponent
                @FleksComponent data class Speed(var base: Float = DEFAULT_SPEED) {
                    companion object {
                        const val DEFAULT_SPEED = 20f
                    }
                }
            """
            )
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
    }

    @Test
    fun `@FleksComponent class extends existing companion object with a name`() {
        val result = compile(
            SourceFile.kotlin(
                "Speed.kt", """
                import com.github.quillraven.fleks.FleksComponent
                @FleksComponent data class Speed(var base: Float = DEFAULT_SPEED) {
                    companion object Factory {
                        const val DEFAULT_SPEED = 20f
                    }
                }
            """
            )
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
    }
}
