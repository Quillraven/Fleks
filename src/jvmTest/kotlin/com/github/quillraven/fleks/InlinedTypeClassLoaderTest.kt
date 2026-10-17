package com.github.quillraven.fleks

import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertSame

private const val PLUGIN_PACKAGE = "com.github.quillraven.fleks.plugin."

/**
 * A class loader like the one of a plugin: it defines the classes of the plugin package itself
 * and gets everything else, e.g. Fleks, from its parent.
 */
private class PluginClassLoader(parent: ClassLoader) : ClassLoader(parent) {
    override fun loadClass(name: String, resolve: Boolean): Class<*> = synchronized(getClassLoadingLock(name)) {
        if (!name.startsWith(PLUGIN_PACKAGE)) {
            return super.loadClass(name, resolve)
        }
        val pluginClass = findLoadedClass(name) ?: run {
            val bytes = parent.getResourceAsStream("${name.replace('.', '/')}.class")!!.use { it.readBytes() }
            defineClass(name, bytes, 0, bytes.size)
        }
        if (resolve) {
            resolveClass(pluginClass)
        }
        pluginClass
    }
}

internal class InlinedTypeClassLoaderTest {
    private val pluginLoader = PluginClassLoader(javaClass.classLoader)
    private val pluginTypes = pluginLoader.loadClass("${PLUGIN_PACKAGE}PluginTypes").getConstructor().newInstance()

    private fun createInPlugin(function: String): Any = pluginTypes.javaClass.getMethod(function).invoke(pluginTypes)

    @Test
    fun pluginCodeIsDefinedByThePluginClassLoader() {
        assertSame(pluginLoader, pluginTypes.javaClass.classLoader)
        assertSame(javaClass.classLoader, EntityTag::class.java.classLoader, "Fleks comes from the parent")
    }

    @Test
    fun entityTagOfCreatesTheTagClassInTheCallersClassLoader() {
        val tag = createInPlugin("tag")

        assertIs<EntityTag>(tag)
        assertSame(pluginLoader, tag.javaClass.classLoader)
    }

    @Test
    fun componentTypeOfCreatesTheTypeClassInTheCallersClassLoader() {
        val type = createInPlugin("componentType")

        assertIs<ComponentType<*>>(type)
        assertSame(pluginLoader, type.javaClass.classLoader)
    }
}
