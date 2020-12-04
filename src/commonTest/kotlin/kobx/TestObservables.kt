package kobx

import kobx.api.Kobx
import kobx.api.autorun
import kobx.api.transaction
import kotlin.test.Test
import kotlin.test.assertEquals

class TestObservables {
    @Test
    fun testBasic() {
        val x = Kobx.box(3)
        val b = mutableListOf<Int>()
        x.observe({ b += it.newValue })
        assertEquals(3, x.get())

        x.set(5)
        assertEquals(5, x.get())
        assertEquals(listOf(5), b)
    }

    @Test
    fun testBasic2() {
        val x = Kobx.box(3)
        val z = Kobx.computed { x.get()*2 }
        val y = Kobx.computed { x.get()*3 }

        assertEquals(6, z.get())
        assertEquals(9, y.get())

        x.set(5)
        assertEquals(10, z.get())
        assertEquals(15, y.get())
    }

    @Test
    fun testComputedStructural() {
        val x1 = Kobx.box(3)
        val x2 = Kobx.box(5)
        val y = Kobx.computed {
            mapOf("sum" to x1.get()+x2.get())
        }
        val b = mutableListOf<Map<String, Int>>()
        y.observe({ b+= it.newValue }, true)

        assertEquals(8, y.get()["sum"])

        x1.set(4)
        assertEquals(9, y.get()["sum"])

        Kobx.transaction {
            x1.set(5)
            x2.set(4)
        }

        assertEquals(listOf(mapOf("sum" to 8), mapOf("sum" to 9)), b)
    }

    @Test
    fun testDynamic() {
        val x = Kobx.box(3)
        val y = Kobx.computed { x.get()*x.get() }

        assertEquals(9, y.get())
        val b = mutableListOf<Int>()
        y.observe({ b+= it.newValue})

        x.set(5)
        assertEquals(25, y.get())

        assertEquals(listOf(25), b)
    }

    @Test
    fun testBatch() {
        val a = Kobx.box(2)
        val b = Kobx.box(3)
        val c = Kobx.computed { a.get()*b.get() }
        val d = Kobx.computed { c.get()*b.get() }
        val buf = mutableListOf<Int>()
        d.observe({ buf += it.newValue})

        a.set(4)
        b.set(5)

        assertEquals(listOf(36, 100),buf)

        val x = Kobx.transaction {
            a.set(2)
            b.set(3)
            a.set(6)
            assertEquals(100, d.value!!.result)
            assertEquals(54, d.get())
            2
        }

        assertEquals(2, x)
        assertEquals(listOf(36, 100, 54), buf)
    }

    @Test
    fun testScope() {
        val vat = Kobx.box(0.2)
        class Order() {
            var price by Kobx.box(20.0)
            var amount by Kobx.box(2)
            val total by Kobx.computed { (1+vat.get())*price*amount }
        }

        val order = Order()
        order.price = 10.0
        order.amount = 3
        assertEquals(36.0, order.total)

        val totals = mutableListOf<Double>()
        val sub = Kobx.autorun {
            totals+=order.total
        }
        order.amount = 4
        sub.dispose()
        order.amount = 5
        assertEquals(listOf(36.0, 48.0), totals)
    }

    @Test
    fun testCount() {
        var bCount = 0
        var cCount = 0
        val a = Kobx.box(3)
        val b = Kobx.computed {
            bCount++
            4 + a.get()-a.get()
        }
        val c = Kobx.computed {
            cCount++
            b.get()
        }

        assertEquals(4, b.get())
        assertEquals(4, c.get())
        assertEquals(1, bCount)
        assertEquals(1, cCount)

        a.set(5)

        assertEquals(4, b.get())
        assertEquals(4, c.get())
        assertEquals(2, bCount)
        assertEquals(1, cCount)

    }
}