package kobx

import kobx.remote.BaseEntityManager
import kobx.remote.BaseEntity
import kobx.remote.ListDidChangeSerializer
import kobx.remote.ValueDidChangeSerializer
import kobx.types.*
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class TestSerialization {
    var f = BaseEntityManager()
    val entity = object : BaseEntity(10, f) {
        override val type: String = "Foo"
    }
    val format = f.createJson()


    @InternalSerializationApi
    @Test
    fun testValueInt() {
        val x = ObservableValue(3)
        f.addAttribute(entity, "x", x)
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

    @InternalSerializationApi
    @Test
    fun testValueString() {
        val x = ObservableValue("33")
        f.addAttribute(entity, "x", x)
        var e2: DidChange? = null

        x.observe({
            val json = format.encodeToString(it as DidChange)
            println(json)
            e2 = format.decodeFromString<DidChange>(json)

            assertEquals(it, e2)
        })

        x.set("44")

        assertNotNull(e2)
    }

    @InternalSerializationApi
    @Test
    fun testValueFloat() {
        val x = ObservableValue(3.3f)
        f.addAttribute(entity, "x", x)
        var e2: DidChange? = null

        x.observe({
            val json = format.encodeToString(it as DidChange)
            println(json)
            e2 = format.decodeFromString<DidChange>(json)

            assertEquals(it, e2)
        })

        x.set(4.4f)

        assertNotNull(e2)
    }

    @InternalSerializationApi
    @Test
    fun testListString() {
        val x = ObservableList(listOf("Foo"))
        f.addAttribute(entity, "x", x)
        val sent = mutableListOf<DidChange>()
        val received = mutableListOf<DidChange>()

        x.observe({
            sent += it
            val json = format.encodeToString(it as DidChange)
            println(json)
            val e = format.decodeFromString<DidChange>(json)
            received += e
        })
        x.add("Bar")
        x.add("Bar2")
        x.remove("Bar")
        x[0]="Foo2"

        assertEquals(sent, received)
    }
    @InternalSerializationApi
    @Test
    fun testListInt() {
        val x = ObservableList(listOf(1))
        f.addAttribute(entity, "x", x)
        val sent = mutableListOf<DidChange>()
        val received = mutableListOf<DidChange>()

        x.observe({
            sent += it
            val json = format.encodeToString(it as DidChange)
            println(json)
            val e = format.decodeFromString<DidChange>(json)
            received += e
        })
        x.add(2)
        x.add(3)
        x.remove(2)
        x[0]=7
        assertEquals(sent, received)
    }
    @InternalSerializationApi
    @Test
    fun testListFloat() {
        val x = ObservableList(listOf(1.1f))
        f.addAttribute(entity, "x", x)
        val sent = mutableListOf<DidChange>()
        val received = mutableListOf<DidChange>()

        x.observe({
            sent += it
            val json = format.encodeToString(it as DidChange)
            println(json)
            val e = format.decodeFromString<DidChange>(json)
            received += e
        })
        x.add(1.2f)
        x.add(1.3f)
        x.remove(1.2f)
        x[0] = 2.0f
        assertEquals(sent, received)
    }
}