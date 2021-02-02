package kobx.remote

import kobx.types.DidChange
import kobx.types.ObservableList
import kobx.types.ObservableValue


class TrackChangesManager(): BaseEntityManager() {
    val outgoing = mutableListOf<DidChange>()
    override fun <T> createValueAttribute(
        entity: Entity,
        name: String,
        id: Int,
        obs: ObservableValue<T>
    ): ValueAttribute<T> {
        obs.observe({
            outgoing += it
        })
        return super.createValueAttribute(entity, name, id, obs)
    }

    override fun <T> createListAttribute(
        entity: Entity,
        name: String,
        id: Int,
        obs: ObservableList<T>
    ): ListAttribute<T> {
        obs.observe({
            outgoing += it
        })
        return super.createListAttribute(entity, name, id, obs)
    }

    override fun addEntity(entity: Entity) {
        super.addEntity(entity)
        outgoing += EntityCreated(entity)
    }
}
//class Receiver {
//    fun receive(evt: DidChange) {
//        evt.apply()
//    }
//}