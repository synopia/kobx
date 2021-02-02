package kobx.remote

import kobx.api.KobxProvider
import kobx.types.DidChange
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

abstract class BaseEntity(override val id : Int, val em: EntityManager): Entity {
    override val attributes = mutableListOf<Attribute<*>>()

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

}

