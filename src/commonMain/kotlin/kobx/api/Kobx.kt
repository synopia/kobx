package kobx.api

import kobx.types.ObservableList
import kobx.types.ObservableValue

object Kobx {
    fun <T> box(value:T) = ObservableValue(value)
    fun <T> list(values: List<T>) = ObservableList(values)
    fun <T> listOf(vararg values: T) = ObservableList(values.toList())

    fun<T> computed(get: ()->T) = Computed.computed(get)
}