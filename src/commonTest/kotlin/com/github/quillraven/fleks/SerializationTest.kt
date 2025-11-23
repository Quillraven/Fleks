package com.github.quillraven.fleks

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Serializable
data object SerializeTag : EntityTag()

@Serializable
class TestSerializableComponent(val iVal: Int = 1, val sVal: String = "s") : Component<TestSerializableComponent> {
    override fun type() = TestSerializableComponent

    companion object : ComponentType<TestSerializableComponent>()
}

class SerializationTest {

    val json = Json {
        serializersModule = SerializersModule {
            // register components
            polymorphic(Component::class) {
                subclass(TestSerializableComponent::class, TestSerializableComponent.serializer())
            }

            // register tags
            polymorphic(UniqueId::class) {
                subclass(SerializeTag::class, SerializeTag.serializer())
            }

            polymorphic(Entity::class) {
                subclass(EntityImpl::class, EntityImpl.serializer())
            }
        }
        allowStructuredMapKeys = true // to support entity id + version as a key
    }


    @Test
    fun testSerializeEntity() {
        val entity0 = entity(id = 0, version = 0u)
        val entity1 = entity(id = 1, version = 1u)

        val entity0Json = json.encodeToString(entity0)
        val entity1Json = json.encodeToString(entity1)

        assertEquals(entity0, json.decodeFromString(entity0Json))
        assertEquals(entity1, json.decodeFromString(entity1Json))
    }

    @Test
    fun testSerializeSnapshot() {
        val world = configureWorld { }
        val entity = world.entity {
            it += TestSerializableComponent()
            it += SerializeTag
        }
        val snapshot = world.snapshot()
        val snapshotJson = json.encodeToString(snapshot)

        val decodedSnapshot = json.decodeFromString<Map<Entity, Snapshot>>(snapshotJson)
        assertEquals(1, decodedSnapshot.size)
        assertEquals(entity, decodedSnapshot.keys.first())
        val decodedCmps = decodedSnapshot.values.first().components
        assertEquals(1, decodedCmps.size)
        assertEquals(1, (decodedCmps.first() as TestSerializableComponent).iVal)
        assertEquals("s", (decodedCmps.first() as TestSerializableComponent).sVal)
        val decodedTags = decodedSnapshot.values.first().tags
        assertEquals(1, decodedTags.size)
        assertEquals(SerializeTag, decodedTags.first())
    }

    @Test
    fun testLoadSerializedSnapshot() {
        val world = configureWorld { }
        world.entity {
            it += TestSerializableComponent(2, "test")
            it += SerializeTag
        }

        val snapshot = world.snapshot()
        val snapshotJson = json.encodeToString(snapshot)
        world.removeAll(clearRecycled = true)

        world.loadSnapshot(json.decodeFromString(snapshotJson))
        assertEquals(1, world.numEntities)
        world.forEach { e ->
            assertTrue(e has SerializeTag)
            assertTrue(e has TestSerializableComponent)
            assertEquals(2, e[TestSerializableComponent].iVal)
            assertEquals("test", e[TestSerializableComponent].sVal)
        }
    }

}
