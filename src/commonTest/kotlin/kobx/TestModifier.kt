package kobx

import kobx.types.ObservableValue
import kotlin.test.Test

class TestModifier {
    @Test
    fun test() {

        val obs = ObservableValue(10, enhancer={ it })
        obs.observe({it})
    }
}