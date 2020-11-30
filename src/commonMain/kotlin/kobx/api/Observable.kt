package kobx.api

import kobx.types.ObservableValue
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun <T> observable(value: T) = object : ReadWriteProperty<Any, T> {
    val observable = ObservableValue<T>(value)

    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        return observable.get()
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        observable.set(value)
    }
}