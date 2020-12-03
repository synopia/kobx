package kobx.api

import kobx.core.ComputedValue
import kobx.core.ComputedValueOptions

object Computed {
    fun<T> computed(get: ()->T) : ComputedValue<T> {
        val opts = ComputedValueOptions(get)
        return ComputedValue(opts)
    }
}