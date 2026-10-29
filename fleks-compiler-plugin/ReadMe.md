# Fleks Compiler Plugin

[![Plugin](https://img.shields.io/gradle-plugin-portal/v/io.github.quillraven.fleks.compiler-plugin?label=Gradle%20Plugin&color=orange)](https://plugins.gradle.org/plugin/io.github.quillraven.fleks.compiler-plugin)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.21-red.svg)](http://kotlinlang.org/)

A Kotlin compiler plugin for [Fleks](https://github.com/Quillraven/Fleks) that removes the boilerplate of
components. Annotate a class with `@FleksComponent` and the plugin generates everything that Fleks needs.

## What it does

Without the plugin, every component needs the same three pieces of code: the `Component<T>` interface, the `type()`
function and a companion object that is the `ComponentType<T>`:

```kotlin
data class Position(var x: Float = 0f, var y: Float = 0f) : Component<Position> {
    override fun type() = Position

    companion object : ComponentType<Position>()
}
```

With the plugin, you only write what is specific to your component:

```kotlin
@FleksComponent
data class Position(var x: Float = 0f, var y: Float = 0f)
```

The usage is identical to before. `Position` is still its own `ComponentType`:

```kotlin
val world = configureWorld { }
val entity = world.entity { it += Position(1f, 2f) }

with(world) {
    val (x, y) = entity[Position]
}
```

For a class annotated with `@FleksComponent` the plugin

1. makes the class implement `Component<Self>`,
2. adds a companion object that is a `ComponentType<Self>` if the class does not have one yet, and
3. generates `override fun type()`, which returns the companion object.

Everything is generated at compile time. There is no reflection and no runtime dependency on the plugin.
That makes it suitable for all targets of Fleks, including those that cannot use reflection like JS/Wasm
(and projects using TeaVM).

### Existing companion objects and `type()`

You can still add your own members to a component:

```kotlin
@FleksComponent
class Health(var current: Float, val max: Float) {
    override fun World.onAdd(entity: Entity) {
        // lifecycle hooks work as usual
    }

    companion object {
        const val DEFAULT_MAX = 100f
        fun full() = Health(DEFAULT_MAX, DEFAULT_MAX)
    }
}
```

- A companion object that you wrote yourself becomes a `ComponentType<Self>` too. It keeps all its members and can
  implement interfaces.
- If the companion already extends `ComponentType<Self>`, it is used as it is.
- If you declare `type()` yourself, the plugin does not generate it. This is useful if a component has more than one
  `ComponentType`.

Classes without the annotation are not touched, so you can migrate component by component.

### Limitations

- Only non-generic classes are supported.
- It only works with the K2 compiler.
- The annotation is part of the Fleks runtime, so Fleks must be a dependency of the module.

## Usage

Add the plugin to the `build.gradle.kts` of the module that contains your components. The Fleks dependency
itself is still required because it contains the `@FleksComponent` annotation:

```kotlin
plugins {
    kotlin("jvm") version "2.4.21" // or kotlin("multiplatform")
    id("io.github.quillraven.fleks.compiler-plugin") version "0.1.3"
}

dependencies {
    implementation("io.github.quillraven.fleks:Fleks:2.16")
}
```

The Kotlin Gradle plugin resolves the compiler plugin like any other dependency from the repositories of your project.
Therefore, make sure that your project has Maven Central available:

```kotlin
repositories {
    mavenCentral()
}
```

The plugin applies itself to all Kotlin compilations of the module, including the ones of a Kotlin Multiplatform
project.

## Compatibility

Compiler plugins use the internal API of the Kotlin compiler that can change with every Kotlin version. Therefore, a
plugin version is built against exactly one Kotlin version. Use that Kotlin version in your project.
Using a different version can fail with errors like a `NoSuchMethodError` during compilation.

| Plugin | Kotlin | Fleks   |
|--------|--------|---------|
| 0.1.3  | 2.4.21 | 2.16+   |

The plugin has its own version which is independent of the Fleks version. A new plugin version is released when the
plugin changes or when it needs to support a new Kotlin version.

The plugin is tested with the JVM target. As it only generates plain Kotlin declarations, other targets are expected
to work as well, but this is not covered by tests yet.

## IntelliJ IDEA

Gradle builds work out of the box, but IntelliJ IDEA in K2 mode only loads the compiler plugins that are bundled with
its Kotlin plugin, e.g. kotlinx.serialization or Compose. Third-party compiler plugins like this one are ignored, so the
editor does not know the generated companion objects and marks every usage of a component as an error.

The reason is that the IDE does not use the Kotlin compiler of your project. It analyzes the code with the Kotlin
version that is bundled with it, and a compiler plugin that is built against a different Kotlin version can break the
code analysis.

To enable the plugin in the IDE:

1. Open *Help | Find Action...*, search for *Registry...* and open it.
2. Disable `kotlin.k2.only.bundled.compiler.plugins.enabled`.
3. Restart the IDE and reload the Gradle project.

| Plugin | Tested with          |
|--------|----------------------|
| 0.1.3  | IntelliJ IDEA 2026.2 |

If the code is still marked as an error, the plugin is most likely not compatible with the Kotlin version of your IDE.
The exceptions of the plugin are logged in the `idea.log` of the IDE (*Help | Show Log in Files*).

## Using a local build

To try out unreleased changes, publish the plugin and Fleks to your local Maven repository. Release versions are
signed, therefore publish the plugin as a snapshot:

```shell
./gradlew publishToMavenLocal -Pfleks.plugin.version=0.1.3-SNAPSHOT
```

Then add `mavenLocal()` to the `pluginManagement` and `dependencyResolutionManagement` repositories of your project
and use the snapshot versions of the plugin (`0.1.3-SNAPSHOT`) and Fleks (the current `fleks.version`).
