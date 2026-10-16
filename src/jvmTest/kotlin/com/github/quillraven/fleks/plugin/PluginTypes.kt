package com.github.quillraven.fleks.plugin

import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.EntityTag
import com.github.quillraven.fleks.componentTypeOf
import com.github.quillraven.fleks.entityTagOf

// stands in for a plugin's code, InlinedTypeClassLoaderTest loads and calls it via reflection
@Suppress("unused")
class PluginTypes {
    fun tag(): EntityTag = entityTagOf()

    fun componentType(): ComponentType<String> = componentTypeOf()
}
