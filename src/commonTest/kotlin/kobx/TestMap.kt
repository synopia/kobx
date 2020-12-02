package kobx

import kobx.api.Kobx
import kobx.api.autorun
import kobx.types.MapChangeType
import kobx.types.MapDidChange
import kobx.types.ObservableMap
import kobx.types.SimpleEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TestMap {
    @Test
    fun testMap() {
        val events = mutableListOf<MapDidChange<String, String>>()
        val m = ObservableMap(mapOf("1" to "a"))
        m.observe({ events += it })

        assertEquals(true, m.containsKey("1"))
        assertEquals(false, m.containsKey("2"))
        assertEquals("a", m["1"])
        assertEquals(null, m["2"])
        assertEquals(1, m.size)

        m["1"] = "aa"
        m["2"] = "b"
        assertEquals(true, m.containsKey("1"))
        assertEquals(true, m.containsKey("2"))
        assertEquals("aa", m["1"])
        assertEquals("b", m["2"])
        assertEquals(2, m.size)

        assertEquals(listOf("1", "2"), m.keys.toList())
        assertEquals(listOf("aa", "b"), m.values.toList())

        m.clear()
        assertEquals(false, m.containsKey("1"))
        assertEquals(false, m.containsKey("2"))
        assertEquals(null, m["1"])
        assertEquals(null, m["2"])
        assertEquals(0, m.size)

        val expected = listOf(
            MapDidChange.update(m, "1", "aa", "a"),
            MapDidChange.add(m, "2", "b"),
            MapDidChange.delete(m, "1", "aa"),
            MapDidChange.delete(m, "2", "b"),
        )
        assertEquals(expected, events)
    }

    @Test
    fun testObserve() {
        val m = ObservableMap(mapOf<String,String>())
        var hasX = false
        var valueX: String? = null
        var valueY: String? = null

        Kobx.autorun {
            hasX = m.containsKey("x")
        }
        Kobx.autorun {
            valueX = m["x"]
        }
        Kobx.autorun {
            valueY = m["y"]
        }

        assertEquals(false, hasX)
        assertEquals(null, valueX)

        m["x"] = "3"
        assertEquals(true, hasX)
        assertEquals("3", valueX)

        m["x"] = "4"
        assertEquals(true, hasX)
        assertEquals("4", valueX)

        m.remove("x")
        assertEquals(false, hasX)
        assertEquals(null, valueX)

        m["x"] = "5"
        assertEquals(true, hasX)
        assertEquals("5", valueX)

        assertEquals(null, valueY)
        m.putAll(mapOf("y" to "hi"))
        assertEquals("hi", valueY)
        m.putAll(mapOf("y" to "hallo"))
        assertEquals("hallo", valueY)

        assertEquals(listOf("x", "y"), m.keys.toList())
    }

    @Test
    fun testObserveCollections() {
        val m = ObservableMap(emptyMap<String, String>())
        var keys: Set<String>? = null
        var values: Collection<String>? = null
        var entries: Set<*>? = null

        Kobx.autorun {
            keys = m.keys
        }
        Kobx.autorun {
            values = m.values
        }
        Kobx.autorun {
            entries = m.entries
        }

        m["a"] = "1"
        assertEquals(setOf("a"), keys)
        assertEquals(listOf("1"), values)
        assertEquals(setOf(SimpleEntry("a", "1")), entries)

        keys = null
        values = null
        entries = null
        m["a"] = "1"
        assertNull(keys)
        assertNull(values)
        assertNull(entries)

        m["a"] = "2"
        assertEquals(listOf("2"), values)
        assertEquals(setOf(SimpleEntry("a", "2")), entries)

        m["b"] = "3"
        assertEquals(setOf("a", "b"), keys)
        assertEquals(listOf("2", "3"), values)
        assertEquals(setOf(SimpleEntry("a", "2"), SimpleEntry("b", "3")), entries)

        m.containsKey("c")
        assertEquals(setOf("a", "b"), keys)
        assertEquals(listOf("2", "3"), values)
        assertEquals(setOf(SimpleEntry("a", "2"), SimpleEntry("b", "3")), entries)

        m.remove("a")
        assertEquals(setOf("b"), keys)
        assertEquals(listOf("3"), values)
        assertEquals(setOf(SimpleEntry("b", "3")), entries)
    }

    @Test
    fun testCleanup() {
        val m = ObservableMap(mapOf("a" to 1))
        var aValue : Int? = null
        val disposer = Kobx.autorun {
            aValue = m["a"]
        }

        var obs = m.data["a"]!!

        assertEquals(1, aValue)
        assertEquals(1, obs.observers.size)
        assertEquals(1, m.hasMap["a"]!!.observers.size)

        assertEquals(1, m.remove("a"))
        assertEquals(null, m.remove("non-existing"))

        assertEquals(null, aValue)
        assertEquals(0, obs.observers.size)
        assertEquals(1, m.hasMap["a"]!!.observers.size)

        m["a"] = 2
        obs = m.data["a"]!!

        assertEquals(2, aValue)
        assertEquals(1, obs.observers.size)
        assertEquals(1, m.hasMap["a"]!!.observers.size)

        disposer.dispose()
        assertEquals(2, aValue)
        assertEquals(0, obs.observers.size)
        assertEquals(false, m.hasMap.containsKey("a"))
    }
}