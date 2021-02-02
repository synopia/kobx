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
): Attribute<List<T>> {
    override val default: List<T> = obs.list.toList()

    override fun current(): List<T> {
        return obs.list
    }

    override fun getValue(thisRef: Any, property: KProperty<*>): List<T> {
        return obs
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: List<T>) {
        obs.replace(value)
    }

    override fun createEvent(): DidChange {
        return ListDidChange(obs, ListChangeType.Splice, 0, added = obs.list)
    }
}
