package kobx

import kobx.types.MapChangeType
import kobx.types.MapDidChange
import kobx.types.ObservableMap
import kotlin.test.Test
import kotlin.test.assertEquals

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
}