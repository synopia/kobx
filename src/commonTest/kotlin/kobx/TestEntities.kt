package kobx

import kobx.remote.*
import kobx.types.DidChange
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.serializer
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class TestEntities {
    class Foo(id: Int, em: EntityManager) : BaseEntity(em, id) {
        override val type: String = "Foo"
        var bar : String by box("x")
    }

    @BeforeTest
    fun setup() {
        EntityFactory.register("Foo") { em, id->Foo(id, em) }
    }
    @Test
    fun testIt() {
        val em = BaseEntityManager()

        val foo1 = Foo(10, em)
        val foo2 = Foo(20, em)

        assertEquals(10, foo1.id)
        assertEquals(20, foo2.id)
        assertEquals(0, foo1.attributes[0].id)
        assertEquals(1, foo2.attributes[0].id)
    }

    @InternalSerializationApi
    @Test
    fun testTracking() {
        val em = TrackChangesManager()

        val f1 = Foo(10, em)
        val f2 = Foo(20, em)

        val remote = BaseEntityManager()
        val outJson = em.createJson()
        val inJson = remote.createJson()
        var json = outJson.encodeToString(em.outgoing)
        println(json)
        var read = inJson.decodeFromString(ListSerializer(DidChange::class.serializer()), json)
        read.forEach {
            it.apply(remote)
        }
        val foo1 = remote.entities[10] as Foo
        val foo2 = remote.entities[20] as Foo

        assertEquals(10, foo1.id)
        assertEquals(20, foo2.id)
        assertEquals(0, foo1.attributes[0].id)
        assertEquals(1, foo2.attributes[0].id)

        em.outgoing.clear()
        f1.bar = "New"
        assertEquals("x", foo1.bar)
        json = outJson.encodeToString(em.outgoing)
        println(json)
        read = inJson.decodeFromString(ListSerializer(DidChange::class.serializer()), json)
        read.forEach { it.apply(remote) }
        assertEquals("New", foo1.bar)
    }
}