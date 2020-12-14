package kobx.api

import kobx.core.ComputedValue
import kobx.core.ComputedValueOptions
import kobx.types.ObservableList
import kobx.types.ObservableValue
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class KobxProvider<T>(val provider: (Any,String)->ReadWriteProperty<Any, T>) {
    operator fun provideDelegate(thisRef: Any, property: KProperty<*>) : ReadWriteProperty<Any, T> {
        return provider(thisRef, property.name)
    }
}
interface KobxFactory {
    fun <T> box(value:T): KobxProvider<T>
    fun <T> list(values: List<T>) : KobxProvider<List<T>>
    fun <T> listOf(vararg values: T) = list(values.toList())

    fun<T> computed(get: ()->T) : KobxProvider<T>
}

object Kobx  {
    fun <T> box(value: T) = ObservableValue(value)
    fun <T> list(values: List<T>) = ObservableList(values)
    fun <T> listOf(vararg values: T) = list(values.toList())
    fun <T> computed(get: () -> T) = ComputedValue(ComputedValueOptions(get))
}