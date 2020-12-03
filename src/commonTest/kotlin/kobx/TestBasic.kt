package kobx

import kobx.api.*
import kobx.core.ComputedValue
import kobx.core.ComputedValueOptions
import kobx.core.resetKobx
import kobx.types.ObservableValue
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class BasicTest {
    class Author(name: String) {
        var name by Kobx.observable(name)
    }
    class Message{
        var title by Kobx.observable("")
        var author by Kobx.observable(Author(""))
    }

    @BeforeTest
    fun reset() {
        resetKobx()
    }

    @Test
    fun testBasic() {
        var changes = emptyList<String>()
        val msg = Message()
        val disposer = Kobx.autorun {
            changes += "${msg.title} from ${msg.author?.name}"
        }

        runInAction {
            msg.title = "X"
        }
        runInAction {
            msg.author = Author("ZZ")
        }
        runInAction {
            msg.author?.name = "Y"
        }
        assertEquals(listOf(" from ", "X from ", "X from ZZ", "X from Y"), changes)
        assertEquals(3, disposer.kobx.toDependencyTree().dependencies.size)

        disposer.dispose()
        assertEquals(0, disposer.kobx.toDependencyTree().dependencies.size)

        runInAction {
            msg.title = "X"
        }
        runInAction {
            msg.author = Author("ZZ")
        }
        runInAction {
            msg.author?.name = "Y"
        }
        assertEquals(listOf(" from ", "X from ", "X from ZZ", "X from Y"), changes)

    }

    @Test
    fun observablesRemoved() {
        var calcs = 0
        val a = ObservableValue(1)
        val b = ObservableValue(2)
        val c = ComputedValue(ComputedValueOptions({
            calcs++
            if( a.get()==1 ) {
                b.get()!!*a.get()!!*b.get()!!
            } else {
                3
            }
        }))

        assertEquals(0, calcs)
        c.observe({})
        assertEquals(4, c.get())
        assertEquals(1, calcs)

        a.set(2)
        assertEquals(3, c.get())
        assertEquals(2, calcs)

        b.set(3)
        assertEquals(3, c.get())
        assertEquals(2, calcs)

        a.set(1)
        assertEquals(9, c.get())
        assertEquals(3, calcs)
    }
}

