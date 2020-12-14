package kobx

import kobx.api.AutorunOptions
import kobx.api.Kobx
import kobx.api.reaction
import kobx.core.Reaction
import kotlin.test.Test
import kotlin.test.assertEquals

class TestReaction {
    @Test
    fun testBasic() {
        val a = Kobx.box(1)
        val values : MutableList<Pair<Int,Int>> = mutableListOf()

        val d = Kobx.reaction({ a.get() }, { new,old-> values+=Pair(new,old)})

        a.set(2)
        a.set(3)
        d.dispose()
        a.set(4)

        assertEquals(listOf(2 to 1, 3 to 2), values)
    }

    @Test
    fun testFireImmediately() {
        val a = Kobx.box(1)
        val values : MutableList<Int> = mutableListOf()
        val d= Kobx.reaction({ a.get() }, { new, _ ->values+=new}, AutorunOptions(fireImmediately =true))

        a.set(2)
        a.set(3)
        d.dispose()
        a.set(4)

        assertEquals(listOf(1,2,3), values)
    }

    @Test
    fun testUntracked() {
        val a = Kobx.box(1)
        val b = Kobx.box(2)
        val values = mutableListOf<Int>()

        val d = Kobx.reaction({a.get()}, {new,_->values += new*b.get()}, AutorunOptions(fireImmediately = true))

        a.set(2)
        b.set(7)
        a.set(3)
        d.dispose()
        a.set(4)

        assertEquals(listOf(2,4,21), values)
    }

    @Test
    fun testWithList() {
        var count = 0
        val r = Reaction("bla", { count++ })
        val obs = Kobx.listOf(1)
        r.track {
            obs.forEach {
                println(it)
            }
        }
        assertEquals(0, count)

        obs.splice(1,0,5)

        assertEquals(1, count)
    }
}