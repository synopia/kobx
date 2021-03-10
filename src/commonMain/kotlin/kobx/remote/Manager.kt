package kobx.remote

import kobx.types.DidChange
import kobx.types.ObservableList
import kobx.types.ObservableMap
import kobx.types.ObservableValue
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

interface EntityManager {
    fun nextAttributeId(id: Int)
    fun addEntity(entity: Entity)
    fun<T> addAttribute(entity: Entity, name: String, obj: ObservableValue<T>): Attribute<T>
    fun<T> addAttribute(entity: Entity, name: String, obj: ObservableList<T>): Attribute<MutableList<T>>
    fun<K,V> addAttribute(entity: Entity, name: String, obj: ObservableMap<K,V>): Attribute<MutableMap<K,V>>

    fun createJson() = Json {
        serializersModule= SerializersModule {
            polymorphic(DidChange::class) {
                subclass(EntityCreatedSerializer(this@EntityManager))
                subclass(ValueDidChangeSerializer(this@EntityManager))
                subclass(ListDidChangeSerializer(this@EntityManager))
            }
        }
    }

    fun<T> getAttribute(obj: Any) : Attribute<T>
    fun<T> getValue(id: Int): ObservableValue<T>
    fun<T> getList(id: Int): ObservableList<T>

}

open class BaseEntityManager() : EntityManager {
    private var _nextEntityId = 0
    private var _nextId = 0

    val attributes = mutableMapOf<Int, Attribute<*>>()
    val obsAttribute = mutableMapOf<Any, Attribute<*>>()
    val entities = mutableMapOf<Int, Entity>()

    open fun<T> createValueAttribute(entity: Entity, name: String, id: Int, obs: ObservableValue<T>) = ValueAttribute(entity, name, id, obs)
    open fun<T> createListAttribute(entity: Entity, name: String, id: Int, obs: ObservableList<T>) = ListAttribute(entity, name, id, obs)
    open fun<K,V> createMapAttribute(entity: Entity, name: String, id: Int, obs: ObservableMap<K,V>) = MapAttribute(entity, name, id, obs)

    private fun<T> add(key: Any, cb: ()->Attribute<T>) : Attribute<T> {
        val attribute = obsAttribute.getOrPut(key) {
            cb()
        }
        attributes[attribute.id] = attribute
        return attribute as Attribute<T>
    }

    override fun<T> addAttribute(entity: Entity, name: String, obj: ObservableValue<T>) : Attribute<T> {
        return add(obj) {
            createValueAttribute(entity, name, nextId(), obj)
        }
    }
    override fun<T> addAttribute(entity: Entity, name: String, obj: ObservableList<T>) : Attribute<MutableList<T>> {
        return add(obj) {
            createListAttribute(entity, name, nextId(), obj)
        }
    }

    override fun <K, V> addAttribute(
        entity: Entity,
        name: String,
        obj: ObservableMap<K, V>
    ): Attribute<MutableMap<K, V>> {
        return add(obj) {
            createMapAttribute(entity, name, nextId(), obj)
        }
    }

    override fun<T> getAttribute(obj: Any) : Attribute<T> {
        return obsAttribute[obj] as Attribute<T>
    }

    override fun<T> getValue(id: Int): ObservableValue<T> {
        return (attributes[id] as ValueAttribute<T>).obs
    }
    override fun<T> getList(id: Int): ObservableList<T> {
        return (attributes[id] as ListAttribute<T>).obs
    }

    override fun addEntity(entity: Entity) {
        entities[entity.id] = entity
    }

    //    override fun createEntity(entity: Entity): Int {
//        val id = _nextEntityId++
//        entities[id] = entity
//        return id
//    }
    override fun nextAttributeId(id: Int) {
        _nextId = id
    }

    fun nextEntityId(): Int {
        return _nextEntityId++
    }
    fun nextId(): Int {
        return _nextId++
    }
}