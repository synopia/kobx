package kobx

import kobx.api.autorun
import kobx.api.observable
import kobx.api.runInAction
import kobx.api.toDependencyTree
import kobx.core.resetKobx
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class BasicTest {
    class Author(name: String) {
        var name by observable(name)
    }
    class Message{
        var title by observable("")
        var author by observable(Author(""))
    }

    @BeforeTest
    fun reset() {
        resetKobx()
    }

    @Test
    fun testBasic() {
        var changes = emptyList<String>()
        val msg = Message()
        val disposer = autorun {
            changes += "${msg.title} from ${msg.author.name}"
        }

        runInAction {
            msg.title = "X"
        }
        runInAction {
            msg.author = Author("ZZ")
        }
        runInAction {
            msg.author.name = "Y"
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
            msg.author.name = "Y"
        }
        assertEquals(listOf(" from ", "X from ", "X from ZZ", "X from Y"), changes)

    }
}

