package kobx

import kobx.remote.*
import kobx.types.DidChange
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals

class TestClientServer {
    open class Peer(val em: TrackChangesManager) {
        val json = em.createJson()

        fun syncEntity(entity: Entity, peer: Peer) {
            em.outgoing += EntityCreated(entity)
            entity.changed().forEach {
                em.outgoing += it.createEvent()
            }
            sendUpdates(peer)
        }

        fun sendUpdates(peer: Peer) {
            val text = json.encodeToString(em.outgoing)
            peer.receiveUpdates(text)
            em.outgoing.clear()
        }

        open fun receiveUpdates(text: String) {
            val evt = json.decodeFromString<List<DidChange>>(text)
            evt.forEach { it.apply(em) }
            em.outgoing.clear()
        }
    }
    class Client(em: TrackChangesManager) : Peer(em){
        var initialized = false
        lateinit var root: Session

        override fun receiveUpdates(text: String) {
            super.receiveUpdates(text)
            if( !initialized ) {
                root = em.entities.values.first() as Session
                initialized = true
            }
        }
    }
    class User(id: Int, em: EntityManager): BaseEntity(id, em) {
        override val type: String = "U"

        var username by box("")
    }
    class Room(id: Int, em: EntityManager): BaseEntity(id, em) {
        override val type: String = "R"

        var title by box("")

    }
    class Session(id: Int, em: EntityManager): BaseEntity(id, em) {
        override val type: String = "S"

        var sessionId by box("")
        var username by box("")
    }

    class Server(em: TrackChangesManager): Peer(em) {
        val users = mutableListOf<User>()
        val rooms = mutableListOf<Room>()
        val sessions = mutableMapOf<Client, Session>()

        init {
            val room = Room(em.nextEntityId(), em)
            room.title = "Global"
            rooms += room
        }

        fun join(client: Client) {
            em.outgoing.clear()

            val session = Session(em.nextEntityId(), em)
            sessions[client] = session
            sendUpdates(client)

            rooms.forEach { syncEntity(it, client) }
        }
    }

    @Test
    fun testIt() {
        EntityFactory.register("U") { em, id -> User(id, em) }
        EntityFactory.register("R") { em, id -> Room(id, em) }
        EntityFactory.register("S") { em, id -> Session(id, em) }
        val serverEm = TrackChangesManager()
        val clientEm = TrackChangesManager()
        val server = Server(serverEm)
        val client = Client(clientEm)
        server.join(client)

        val clientSession = client.root
        clientSession.username = "Client01"
        client.sendUpdates(server)

        assertEquals("Client01", server.sessions[client]!!.username)
    }
}