package com.github.quillraven.fleks.plugin

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCompilerApi::class)
class ComponentAnnotationTest {
    private fun compile(vararg sources: SourceFile) = KotlinCompilation().apply {
        this.sources = sources.toList()
        compilerPluginRegistrars = listOf(FleksCompilerPlugin())
        inheritClassPath = true
        jvmTarget = "17"
    }.compile()

    private fun assertCompiles(vararg sources: SourceFile): JvmCompilationResult {
        val result = compile(*sources)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
        return result
    }

    @Test
    fun `annotated class compiles`() {
        assertCompiles(
            SourceFile.kotlin(
                "Position.kt", """
                import com.github.quillraven.fleks.FleksComponent
                @FleksComponent data class Position(var x: Float = 0f, var y: Float = 0f)
                """
            )
        )
    }

    @Test
    fun `annotated class is a Component of its own type`() {
        assertCompiles(
            SourceFile.kotlin(
                "Velocity.kt", """
                import com.github.quillraven.fleks.Component
                import com.github.quillraven.fleks.FleksComponent
                @FleksComponent data class Velocity(var dx: Float = 0f)
                val check: Component<Velocity> = Velocity()
                """
            )
        )
    }

    @Test
    fun `annotated class gets a companion that is a ComponentType`() {
        assertCompiles(
            SourceFile.kotlin(
                "Health.kt", """
                import com.github.quillraven.fleks.ComponentType
                import com.github.quillraven.fleks.FleksComponent
                @FleksComponent data class Health(var hp: Int = 100)
                val check: ComponentType<Health> = Health
                """
            )
        )
    }

    @Test
    fun `type function returns the companion`() {
        val result = assertCompiles(
            SourceFile.kotlin(
                "Mana.kt", """
                import com.github.quillraven.fleks.FleksComponent
                @FleksComponent data class Mana(var mp: Int = 10)
                fun check() = Mana().type() === Mana
                """
            )
        )
        val check = result.classLoader.loadClass("ManaKt").getMethod("check")
        assertTrue(check.invoke(null) as Boolean)
    }

    @Test
    fun `component types have unique ids`() {
        val result = assertCompiles(
            SourceFile.kotlin(
                "Types.kt", """
                import com.github.quillraven.fleks.FleksComponent
                @FleksComponent data class A(var a: Int = 0)
                @FleksComponent data class B(var b: Int = 0)
                fun ids() = A.id to B.id
                """
            )
        )
        val ids = result.classLoader.loadClass("TypesKt").getMethod("ids").invoke(null) as Pair<*, *>
        assertTrue(ids.first != ids.second)
    }

    @Test
    fun `existing companion that is a ComponentType is reused`() {
        val result = assertCompiles(
            SourceFile.kotlin(
                "Own.kt", """
                import com.github.quillraven.fleks.ComponentType
                import com.github.quillraven.fleks.FleksComponent
                @FleksComponent data class Own(var v: Int = 0) {
                    companion object : ComponentType<Own>()
                }
                fun check() = Own().type() === Own
                """
            )
        )
        val check = result.classLoader.loadClass("OwnKt").getMethod("check")
        assertTrue(check.invoke(null) as Boolean)
    }

    @Test
    fun `user defined type function is respected`() {
        assertCompiles(
            SourceFile.kotlin(
                "Manual.kt", """
                import com.github.quillraven.fleks.Component
                import com.github.quillraven.fleks.ComponentType
                import com.github.quillraven.fleks.FleksComponent
                @FleksComponent data class Manual(var v: Int = 0) {
                    override fun type(): ComponentType<Manual> = Manual
                    companion object : ComponentType<Manual>()
                }
                """
            )
        )
    }

    @Test
    fun `lifecycle hooks can be overridden`() {
        assertCompiles(
            SourceFile.kotlin(
                "Hook.kt", """
                import com.github.quillraven.fleks.*
                @FleksComponent data class Hook(var v: Int = 0) {
                    override fun World.onAdd(entity: Entity) { v = 1 }
                }
                """
            )
        )
    }

    @Test
    fun `works in a world`() {
        val result = assertCompiles(
            SourceFile.kotlin(
                "WorldUse.kt", """
                import com.github.quillraven.fleks.*
                @FleksComponent data class Pos(var x: Int = 0)
                fun check(): Int {
                    val world = configureWorld { }
                    val e = world.entity { it += Pos(5) }
                    return with(world) { e[Pos].x }
                }
                """
            )
        )
        val value = result.classLoader.loadClass("WorldUseKt").getMethod("check").invoke(null)
        assertEquals(5, value)
    }

    @Test
    fun `class without annotation is untouched`() {
        val result = compile(
            SourceFile.kotlin(
                "Plain.kt", """
                import com.github.quillraven.fleks.Component
                data class Plain(var v: Int = 0)
                val check: Component<Plain> = Plain()
                """
            )
        )
        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
    }

    @Test
    fun `existing companion with members becomes a ComponentType`() {
        val result = assertCompiles(
            SourceFile.kotlin(
                "Members.kt", """
                import com.github.quillraven.fleks.ComponentType
                import com.github.quillraven.fleks.FleksComponent
                @FleksComponent data class Members(var v: Int = 0) {
                    companion object {
                        const val MAX = 5
                        fun create() = Members(MAX)
                    }
                }
                val check: ComponentType<Members> = Members
                fun check() = Members.create().type() === Members && Members.MAX == 5
                """
            )
        )
        val check = result.classLoader.loadClass("MembersKt").getMethod("check")
        assertTrue(check.invoke(null) as Boolean)
    }

    @Test
    fun `named companion becomes a ComponentType`() {
        val result = assertCompiles(
            SourceFile.kotlin(
                "Named.kt", """
                import com.github.quillraven.fleks.FleksComponent
                @FleksComponent class Named {
                    companion object Type { val label = "named" }
                }
                fun check() = Named().type() === Named.Type && Named.label == "named"
                """
            )
        )
        val check = result.classLoader.loadClass("NamedKt").getMethod("check")
        assertTrue(check.invoke(null) as Boolean)
    }

    @Test
    fun `existing companion with interface and initializer becomes a ComponentType`() {
        val result = assertCompiles(
            SourceFile.kotlin(
                "Iface.kt", """
                import com.github.quillraven.fleks.FleksComponent
                interface Named { val label: String }
                @FleksComponent class Iface {
                    companion object : Named {
                        override val label = "iface"
                        val created = System.nanoTime()
                    }
                }
                fun check() = Iface().type() === Iface && Iface.label == "iface" && (Iface as Named).label == "iface"
                """
            )
        )
        val check = result.classLoader.loadClass("IfaceKt").getMethod("check")
        assertTrue(check.invoke(null) as Boolean)
    }
}
