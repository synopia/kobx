package kobx

import kobx.api.Kobx
import kobx.api.autorun
import kobx.core.GlobalState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class TestErrorHandling {
    class Foo(x: Int) {
        var x: Int by Kobx.box(x)
        fun y() : Int {
            return Kobx.computed {
                if( x==2) {
                    throw IllegalStateException("xxx")
                }
                x*2
            }.get()
        }
    }

    private fun checkState() {
        assertEquals(false, GlobalState.isRunningReactions)
        assertEquals(null, GlobalState.trackingDerivation)
        assertEquals(0, GlobalState.inBatch)
        assertEquals(false, GlobalState.allowStateChanges)
        assertEquals(0, GlobalState.pendingUnobservations.size)
    }
    @Test
    fun testError1() {
        val a = Kobx.computed<Int> {
            throw IllegalStateException("blah")
        }
        assertFails { a.get() }
        checkState()
    }

    @Test
    fun testRecover1() {
        val a = Foo(1)

        assertEquals(2, a.y())
        a.x = 2

        assertFails { a.y() }
        checkState()
        a.x = 3
        assertEquals(6,a.y())
        checkState()
    }

    @Test
    fun testRecoverAutorun() {
        var b: Int? = null
        val a = Foo(1)
        Kobx.autorun {
            b = a.y()
        }
        checkState()
        a.x = 2

        assertFails {
            assertEquals(2, a.y())
        }
        assertEquals(2,b)
        checkState()

        a.x = 3
        assertEquals(6, a.y())
        assertEquals(6, b)
        checkState()
    }

    @Test
    fun testRecoverAutorun2() {
        var b: Int? = null
        val a = Foo(2)
        Kobx.autorun {
            b = a.y()
        }
        assertEquals(null,b)
        checkState()
        a.x = 3
        assertEquals(6,b)
        checkState()

    }

    @Test
    fun testChangeInAutorun() {
        val x = Kobx.box(3)
        val z = Kobx.box(3)

        Kobx.autorun {
            if( x.get()!=3) {
                z.set(x.get())
            }
        }

        assertEquals(3, x.get())
        assertEquals(3, z.get())

        x.set(5)

        assertEquals(5, x.get())
        assertEquals(5, z.get())

        checkState()
    }
}