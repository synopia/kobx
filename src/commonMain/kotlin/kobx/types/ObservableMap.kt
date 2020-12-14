package kobx.types

import kobx.api.Kobx
import kobx.api.transaction
import kobx.core.Atom
import kobx.core.GlobalState
import kobx.core.IAtom
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

enum class MapChangeType {
    Update,
    Add,
    Delete
}

data class SimpleEntry<K,V>(override val key: K, override var value: V) : MutableMap.MutableEntry<K,V> {
    override fun setValue(newValue: V): V {
        val old = value
        value = newValue
        return old
    }
}

data class MapWillChange<K,V>(
    val obj: IObservableMap<K,V>,
    val type: MapChangeType,
    val key: K,
    val newValue: V? = null
) {
    companion object {
        fun <K,V> update(obj: IObservableMap<K,V>, key: K, newValue: V) =
            MapWillChange(obj, MapChangeType.Update, key, newValue)
        fun <K,V> add(obj: IObservableMap<K,V>, key: K, newValue: V) =
            MapWillChange(obj, MapChangeType.Add, key, newValue)
        fun <K,V> delete(obj: IObservableMap<K,V>, key: K) =
            MapWillChange(obj, MapChangeType.Delete, key)
    }
}

@Serializable
data class MapDidChange<K,V>(
    @Contextual val obj: IObservableMap<K,V>,
    val type: MapChangeType,
    val key: K,
    val newValue: V? = null,
    val oldValue: V? = null
) : DidChange() {
    companion object {
        fun <K,V> update(obj: IObservableMap<K,V>, key: K, newValue: V, oldValue: V?) =
            MapDidChange(obj, MapChangeType.Update, key, newValue, oldValue)
        fun <K,V> add(obj: IObservableMap<K,V>, key: K, newValue: V) =
            MapDidChange(obj, MapChangeType.Add, key, newValue)
        fun <K,V> delete(obj: IObservableMap<K,V>, key: K, oldValue: V) =
            MapDidChange(obj, MapChangeType.Delete, key, null, oldValue)
    }
}

interface IObservableMap<K,V>: MutableMap<K,V> {
    fun intercept(handler: IInterceptor<MapWillChange<K,V>>): () -> Unit
    fun observe(listener: (MapDidChange<K,V>) -> Unit, fireImmediately: Boolean = false): () -> Unit
}

class ObservableMap<K,V>(
    initialData: Map<K,V>,
    name: String = "ObservableMap@${GlobalState.nextId()}"
): MutableMap<K,V>, IObservableMap<K,V>, IInterceptable<MapWillChange<K,V>>, IListenable<MapDidChange<K,V>> {
    internal val data = mutableMapOf<K, ObservableValue<V?>>()
    internal val hasMap = mutableMapOf<K, ObservableValue<Boolean>>()

    override var interceptors: MutableList<IInterceptor<MapWillChange<K, V>>>? = null
    override var changeListeners: MutableList<(MapDidChange<K, V>) -> Unit>? = null
    private val keysAtom: IAtom = Atom(name)

    init {
        putAll(initialData)
    }


    override fun containsKey(key: K): Boolean {
        if (GlobalState.trackingDerivation == null) {
            return data.containsKey(key)
        }

        val entry = hasMap.getOrPut(key) {
            val newEntry = ObservableValue(data.containsKey(key))
            newEntry.onBUOL += {
                hasMap.remove(key)
            }
            newEntry
        }
        return entry.get()
    }

    override fun put(key: K, value: V): V? {
        val hasKey = data.containsKey(key)
        var v = value
        if (hasInterceptors()) {
            val change = if (hasKey) {
                interceptChange(MapWillChange.update(this, key, value))
            } else {
                interceptChange(MapWillChange.add(this, key, value))
            }
            v = change?.newValue ?: value
        }
        if (hasKey) {
            updateValue(key, v)
        } else {
            addValue(key, v)
        }
        return null
    }

    override fun remove(key: K): V? {
        keysAtom.checkIfStateModificationsAreAllowed()
        if( hasInterceptors() ) {
            val change = interceptChange(MapWillChange.delete(this, key))
            if( change?.newValue==null ) {
                return null
            }
        }
        if( data.containsKey(key)) {
            val change = MapDidChange.delete(this, key, data[key]!!.value!!)
            val old = Kobx.transaction {
                keysAtom.reportChanged()
                updateHasMapEntry(key, false)
                val obs = data[key]!!
                val old = obs.get()!!
                obs.setNewValue(null)
                data.remove(key)
                old
            }
            if( hasListeners() ) {
                notifyListeners(change)
            }
            return old
        }
        return null
    }

    private fun updateHasMapEntry(key: K, value: Boolean) {
        hasMap[key]?.setNewValue(value)
    }
    private fun updateValue(key: K, newValue: V) {
        val obs = data[key]!!
        val (value, changed) = obs.prepareNewValue(newValue)
        if( value!=null && changed ) {
            val change = MapDidChange.update(this, key, value, obs.value)
            obs.setNewValue(value)
            if( hasListeners() ) {
                notifyListeners(change)
            }
        }
    }

    private fun addValue(key: K, newValue: V) {
        keysAtom.checkIfStateModificationsAreAllowed()
        val value = Kobx.transaction {
            val obs = ObservableValue(newValue as V?)
            data[key] = obs
            val value = obs.value!!
            updateHasMapEntry(key, true)
            keysAtom.reportChanged()
            value
        }
        val change = MapDidChange.add(this, key, value)
        if( hasListeners() ) {
            notifyListeners(change)
        }
    }

    override fun get(key: K): V? {
        return if( containsKey(key)) {
            data[key]?.get()
        } else {
            null
        }
    }

    override val keys: MutableSet<K>
        get() {
            keysAtom.reportObserved()
            return data.keys
        }

    override val values: MutableCollection<V>
        get() {
            keysAtom.reportObserved()
            return data.values.map { it.get()!! }.toMutableList()
        }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() {
            keysAtom.reportObserved()
            val map: List<SimpleEntry<K, V>> = data.entries.map { SimpleEntry(it.key, it.value.get()!!) }
            return map.toMutableSet()
        }

    override val size: Int
        get() {
            keysAtom.reportObserved()
            return data.size
        }

    override fun isEmpty(): Boolean {
        return size==0
    }

    override fun clear() {
        Kobx.transaction {
            GlobalState.untracked {
                keys.toList().forEach {
                    remove(it)
                }
            }
        }
    }

    override fun containsValue(value: V): Boolean {
        return values.find { it==value } != null
    }

    override fun putAll(from: Map<out K, V>) {
        Kobx.transaction {
            from.forEach { (k,v)-> this[k]=v }
        }
    }

    override fun intercept(handler: IInterceptor<MapWillChange<K, V>>): () -> Unit {
        return registerInterceptor(handler)
    }

    override fun observe(listener: (MapDidChange<K, V>) -> Unit, fireImmediately: Boolean): () -> Unit {
//        if( fireImmediately) {
//            listener(ListDidChange.splice(this, 0, list.toList(), emptyList()))
//        }
        return registerListener(listener)

    }
}