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


fun <T> observable(value: T) = object : ReadWriteProperty<Any, T?> {
    val observable = Observable.box(value)

    override fun getValue(thisRef: Any, property: KProperty<*>): T? {
        return observable.get()
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T?) {
        observable.set(value)
    }
}