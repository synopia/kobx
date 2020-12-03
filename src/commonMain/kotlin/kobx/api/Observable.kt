package kobx.api

import kobx.types.ObservableList
import kobx.types.ObservableValue

object Observable {
    fun <T> box(value:T) = ObservableValue(value)
    fun <T> list(values: List<T>) = ObservableList(values)
    fun <T> listOf(vararg values: T) = ObservableList(values.toList())
}