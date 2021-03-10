package kobx.remote

import kobx.types.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

interface Attribute<T>: ReadWriteProperty<Any, T>{
    val entity: Entity
    val name: String
    val id: Int
    val default: T

    fun current(): T
    fun createEvent(): DidChange
}
data class ValueAttribute<T>(
    override val entity: Entity,
    override val name: String,
    override val id: Int,
    val obs: ObservableValue<T>
): Attribute<T> {
    override val default: T = obs.get()

    override fun current(): T {
        return obs.get()
    }

    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        return obs.get()
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        obs.set(value)
    }

    override fun createEvent(): DidChange {
        return ValueDidChange(obs, obs.get(), null)
    }
}
data class ListAttribute<T>(
    override val entity: Entity,
    override val name: String,
    override val id: Int,
    val obs: ObservableList<T>
): Attribute<MutableList<T>> {
    override val default: MutableList<T> = obs.list.toMutableList()

    override fun current(): MutableList<T> {
        return obs.list
    }

    override fun getValue(thisRef: Any, property: KProperty<*>): MutableList<T> {
        return obs
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: MutableList<T>) {
        obs.replace(value)
    }

    override fun createEvent(): DidChange {
        return ListDidChange(obs, ListChangeType.Splice, 0, added = obs.list)
    }
}

data class MapAttribute<K,V>(
    override val entity: Entity,
    override val name: String,
    override val id: Int,
    val obs: ObservableMap<K,V>
): Attribute<MutableMap<K,V>> {
    override val default: MutableMap<K, V> = obs

    override fun current(): MutableMap<K, V> {
        return obs
    }

    override fun getValue(thisRef: Any, property: KProperty<*>): MutableMap<K, V> {
        return obs
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: MutableMap<K, V>) {
        obs.clear()
        obs.putAll(value)
    }

    override fun createEvent(): DidChange {
        throw IllegalStateException()
//        return MapDidChange(obs, MapChangeType.Add)
    }
}