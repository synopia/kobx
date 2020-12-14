package kobx

import kobx.types.*
import kotlinx.serialization.*
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class TestSerialization {
    class OBS: KSerializer<ObservableValue<*>> {
        val map = mutableMapOf<Int, ObservableValue<*>>()
        var nextId = 1
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("OBV", PrimitiveKind.INT)
        override fun deserialize(decoder: Decoder): ObservableValue<*> {
            return map[decoder.decodeInt()]!!
        }

        override fun serialize(encoder: Encoder, value: ObservableValue<*>) {
            val id = nextId++
            map[id] = value
            encoder.encodeInt(id)
        }
    }
    class OBSL: KSerializer<ObservableList<*>> {
        val map = mutableMapOf<Int, ObservableList<*>>()
        var nextId = 1
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("OBV", PrimitiveKind.INT)
        override fun deserialize(decoder: Decoder): ObservableList<*> {
            return map[decoder.decodeInt()]!!
        }

        override fun serialize(encoder: Encoder, value: ObservableList<*>) {
            val id = nextId++
            map[id] = value
            encoder.encodeInt(id)
        }
    }

    val format = Json {
        serializersModule= SerializersModule {
            contextual(ObservableValue::class, OBS())
            contextual(ObservableList::class, OBSL())
            polymorphic(Any::class) {
                subclass(Int.serializer())
                subclass(String.serializer())
            }
            polymorphic(DidChange::class) {
                subclass(ValueDidChange.serializer(PolymorphicSerializer(Any::class)))
                subclass(ListDidChange.serializer(PolymorphicSerializer(Any::class)))
            }
        }
    }

    @Test
    fun testValue() {
        val x = ObservableValue(3)
        var e2: DidChange? = null

        x.observe({
            val json = format.encodeToString(it as DidChange)
            println(json)
            e2 = format.decodeFromString<DidChange>(json)

            assertEquals(it, e2)
        })

        x.set(4)

        assertNotNull(e2)
    }

    @Test
    fun testList() {
        val x = ObservableList(listOf("Foo"))
        var e2: ListDidChange<String>? = null
        x.observe({
            val json = format.encodeToString(it as DidChange)
            println(json)
            e2 = format.decodeFromString<ListDidChange<String>>(json)

            assertEquals(it, e2)
        })
        x.add("Bar")

        assertNotNull(e2)
    }
}