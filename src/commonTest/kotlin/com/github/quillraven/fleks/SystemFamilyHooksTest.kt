package com.github.quillraven.fleks

import com.github.quillraven.fleks.World.Companion.family
import kotlin.test.Test
import kotlin.test.assertEquals

private class NameComponent(val name: String) : Component<NameComponent> {
    companion object : ComponentType<NameComponent>()
    override fun type() = NameComponent
}

private class SystemFamilyHooksTest {
    @Test
    fun iterationSystemFamilyHookFlow() {

        var check: Int = 0

        val world = configureWorld {
            families {
                val family = family { all(NameComponent) }
                onAdd(family) { entity ->
                    assertEquals(0, check++) // First, we expect the global add hook to be called.
                    println("$check. Global family onAdd() called with: entity = ${entity[NameComponent].name}")
                }
                onRemove(family) { entity ->
                    assertEquals(7, check++) // Lastly, the global hook gets called after all the systems.
                    println("$check. Global family onRemove() called with: entity = ${entity[NameComponent].name}")
                }
            }

            systems {
                add(object : IteratingSystem(family { all(NameComponent) }, familyHooks = true) {
                    val sysName = "System0"

                    override fun onAddEntity(entity: Entity) {
                        assertEquals(1, check++) // onAdd hooks called in forward system order. Thus, "System0" gets called first.
                        println("$check. $sysName onAddEntity() called with: entity = ${entity[NameComponent].name}")
                    }

                    override fun onTickEntity(entity: Entity) {
                        assertEquals(3, check++)
                        println("$check. $sysName onTickEntity() called with: entity = ${entity[NameComponent].name}")
                    }

                    override fun onRemoveEntity(entity: Entity) {
                        assertEquals(6, check++)
                        println("$check. $sysName onRemoveEntity() called with: entity = ${entity[NameComponent].name}")
                    }
                })
                add(object : IteratingSystem(family { all(NameComponent) }, familyHooks = true) {
                    val sysName = "System1"

                    override fun onAddEntity(entity: Entity) {
                        assertEquals(2, check++)
                        println("$check. $sysName onAddEntity() called with: entity = ${entity[NameComponent].name}")
                    }

                    override fun onTickEntity(entity: Entity) {
                        assertEquals(4, check++)
                        println("$check. $sysName onTickEntity() called with: entity = ${entity[NameComponent].name}")
                    }

                    override fun onRemoveEntity(entity: Entity) {
                        assertEquals(5, check++) // onRemove hooks called in reverse system order. Thus, "System1" gets called first.
                        println("$check. $sysName onRemoveEntity() called with: entity = ${entity[NameComponent].name}")
                    }
                })
            }
        }
        val entity = world.entity { it += NameComponent("Entity0") }
        world.update(0f)
        world -= entity
    }
}
