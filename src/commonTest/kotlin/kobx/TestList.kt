package kobx

import kobx.api.Kobx
import kobx.api.autorun
import kobx.core.ComputedValue
import kobx.core.ComputedValueOptions
import kobx.types.*
import kotlin.test.Test
import kotlin.test.assertEquals

class TestList {
    fun <T> assertEquals(expected: List<T>, actual:ObservableList<T>) {
        assertEquals(expected, actual.list)
    }
    @Test
    fun test1() {
        val a = Kobx.list(mutableListOf<Int>())
        assertEquals(0, a.size)
        assertEquals(listOf(), a)

        a += 1
        assertEquals(1, a.size)
        assertEquals(listOf(1), a)

        a += 2
        assertEquals(2, a.size)
        assertEquals(listOf(1, 2), a)

        val sum = ComputedValue(ComputedValueOptions({
            -1 + a.fold(1, { acc, v -> acc+v})
        }))

        assertEquals(3, sum.get())

        a[1] = 3
        assertEquals(2, a.size)
        assertEquals(listOf(1, 3), a)
        assertEquals(4, sum.get())

        a.splice(1, 1, 4, 5)
        assertEquals(3, a.size)
        assertEquals(listOf(1, 4, 5), a)
        assertEquals(10, sum.get())

        a.replace(listOf(2, 4))
        assertEquals(6, sum.get())

        a.splice(1, 1)
        assertEquals(2, sum.get())
        assertEquals(listOf(2), a)

        a.spliceWithArray(0, 0, listOf(4, 3))
        assertEquals(9, sum.get())
        assertEquals(listOf(4, 3, 2), a)

        a.clear()
        assertEquals(0, sum.get())
        assertEquals(emptyList<Int>(), a)

        a.replace(listOf(1, 2))
        a.reverse()
        assertEquals(listOf(2, 1), a)

        a += 3
        a.sort()
        assertEquals(listOf(1, 2, 3), a)
        assertEquals(6, sum.get())

        assertEquals(2, a[1])
        a[2] = 4
        assertEquals(4, a[2])
    }

    @Test
    fun testObserve() {
        val ar = Kobx.listOf(1, 4)
        val buf = mutableListOf<ListDidChange<Int>>()
        val disposer = ar.observe({ buf += it}, true)

        ar[1] = 3           // 1,3
        ar += 0             // 1,3,0
        ar.removeFirst()    // 3,0
        ar.addAll(listOf(1,2))  // 3, 0, 1,2
        ar.splice(1,2,3,4)  // 3,3,4,2
        assertEquals(listOf(3,3,4,2), ar)

        val expected = listOf(
            ListDidChange.splice(ar, 0, listOf(1,4), emptyList()),
            ListDidChange.update(ar, 1, 3, 4),
            ListDidChange.splice(ar, 2, listOf(0), emptyList()),
            ListDidChange.splice(ar, 0, emptyList(), listOf(1)),
            ListDidChange.splice(ar, 2, listOf(1,2), emptyList()),
            ListDidChange.splice(ar, 1, listOf(3,4), listOf(0,1)),
        )
        assertEquals(expected, buf)
        disposer()
        ar[0] = 5
        assertEquals(expected, buf)
    }

    @Test
    fun testModification1() {
        val a = Kobx.listOf(1,2,3)
        val r = a.splice(-10, 5, 4, 5, 6)
        assertEquals(listOf(4,5,6), a)
        assertEquals(listOf(1,2,3), r)
    }

    @Test
    fun testString() {
        val x = Kobx.listOf(1)
        var c=0
        Kobx.autorun {
            println(x.toString())
            c++
        }
        x += 1
        assertEquals(2, c)
    }
    @Test
    fun testSorting() {
        val x = Kobx.listOf(4,2,3)
        val sortedX = ComputedValue(ComputedValueOptions({ x.sorted() }))
        var sorted = emptyList<Int>()
        Kobx.autorun {
            sorted = sortedX.get()
        }

        assertEquals(listOf(4,2,3), x)
        assertEquals(listOf(2,3,4), sorted)
        x += 1
        assertEquals(listOf(4,2,3, 1), x)
        assertEquals(listOf(1, 2,3,4), sorted)
        x.removeFirst()
        assertEquals(listOf(2,3, 1), x)
        assertEquals(listOf(1, 2,3), sorted)

    }
}