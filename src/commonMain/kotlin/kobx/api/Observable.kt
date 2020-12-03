package kobx.api

import kobx.types.ObservableList
import kobx.types.ObservableValue
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

object Observable {
    fun <T> box(value:T) = ObservableValue<T>(value)
    fun <T> list(values: List<T>) = ObservableList(values)
    fun <T> listOf(vararg values: T) = ObservableList(values.toList())
}