package kobx.remote

import kobx.api.KobxProvider
import kobx.types.DidChange
import kobx.types.ObservableList
import kobx.types.ObservableMap
import kobx.types.ObservableValue
import kotlinx.serialization.Serializable

@Serializable(with = EntityCreatedSerializer::class)
data class EntityCreated(val entity: Entity): DidChange() {
    override fun apply(em: EntityManager) {
    }
}

interface Entity {
    val id: Int
    val type: String
    val attributes: List<Attribute<*>>

    fun changed() = attributes.filter { it.default!=it.current() }
}

object EntityFactory {
    private val factories = mutableMapOf<String,(EntityManager, Int)->Entity>()
    fun register(cls: String, f: (EntityManager, Int)->Entity) {
        factories[cls] = f
    }
    fun create(em: EntityManager, id: Int, cls: String): Entity {
        return factories[cls]!!(em, id)
    }
}

abstract class BaseEntity(val em: EntityManager, override val id: Int = 0): Entity {
    override val attributes = mutableListOf<Attribute<*>>()
    override val type = ""

    init {
        em.addEntity(this)
    }

    fun <T> box(value:T): KobxProvider<T> {
        return KobxProvider { e, name->
            val a = em.addAttribute(this, name, ObservableValue(value))
            attributes += a
            a
        }
    }

    fun <T> list(vararg values: T): KobxProvider<MutableList<T>> {
        return KobxProvider { e, name ->
            val a = em.addAttribute(this, name, ObservableList(values.toList()))
            attributes += a
            a
        }
    }

    fun <T> list(values: List<T>): KobxProvider<MutableList<T>> {
        return KobxProvider { e, name ->
            val a = em.addAttribute(this, name, ObservableList(values))
            attributes += a
            a
        }
    }

    fun <K, V> map(values: Map<K,V>): KobxProvider<MutableMap<K,V>> {
        return KobxProvider { e, name ->
            val a = em.addAttribute(this, name, ObservableMap(values))
            attributes += a
            a
        }
    }
}

