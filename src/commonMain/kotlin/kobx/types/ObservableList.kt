package kobx.types

import kobx.core.Atom
import kobx.core.GlobalState
import kobx.core.IAtom
import kobx.remote.EntityManager
import kobx.remote.ListDidChangeSerializer
import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

@Serializable
enum class ListChangeType {
    Update,
    Splice
}
data class ListWillChange<T>(
    val obj: IObservableList<T>,
    val type: ListChangeType,
    val index: Int,
    val newValue: T? = null,
    val added: List<T>? = null,
    val removed: Int? = null
) {
    companion object {
        fun <T> update(obj: IObservableList<T>, index: Int, newValue: T) =
            ListWillChange(obj, ListChangeType.Update, index, newValue)
        fun <T> splice(obj: IObservableList<T>, index: Int, added: List<T>, removed: Int) =
            ListWillChange(obj, ListChangeType.Splice, index, null, added, removed)
    }
}
@Serializable(with = ListDidChangeSerializer::class)
data class ListDidChange<T>(
    val obj: ObservableList<T>,
    val changeType: ListChangeType,
    val index: Int,
    val newValue: T? = null,
    val oldValue: T? = null,
    val added: List<T>? = null,
    val removed: List<T>? = null
) : DidChange(){
    companion object {
        fun <T> update(obj: ObservableList<T>, index: Int, newValue: T, oldValue: T?) =
            ListDidChange(obj, ListChangeType.Update, index, newValue, oldValue)
        fun <T> splice(obj: ObservableList<T>, index: Int, added: List<T>, removed: List<T>) =
            ListDidChange(obj, ListChangeType.Splice, index, null, null, listOrNull(added), listOrNull(removed))
        private fun<T> listOrNull(list:List<T>): List<T>? = if (list.isEmpty()) null else list
    }

    override fun apply(em: EntityManager) {
        when(changeType) {
            ListChangeType.Update->obj.set(index, newValue!!)
            ListChangeType.Splice->{
                obj.spliceWithArray(index, removed?.size ?: 0, added!!)
            }
        }
    }
}


interface IObservableList<T>: MutableList<T> {
    fun intercept(handler: IInterceptor<ListWillChange<T>>): () -> Unit
    fun observe(listener: (ListDidChange<T>) -> Unit, fireImmediately: Boolean = false): () -> Unit

    fun spliceWithArray(index: Int, deleteCount: Int, newItems: List<T>): List<T>
    fun splice(index: Int, deleteCount: Int, vararg newItems: T) =
        spliceWithArray(index, deleteCount, newItems.toList())

    fun replace(newItems: List<T>) = spliceWithArray(0, size, newItems)
}

class ObservableList<T>(
    initialData: List<T>,
    name: String = "ObservableList@${GlobalState.nextId()}"
): MutableList<T>,
    IObservableList<T>,
    IInterceptable<ListWillChange<T>>,
    IListenable<ListDidChange<T>>,
    ReadWriteProperty<Any, List<T>>
{
    internal val list = initialData.toMutableList()
    override var interceptors: MutableList<IInterceptor<ListWillChange<T>>>? = null
    override var changeListeners: MutableList<(ListDidChange<T>) -> Unit>? = null
    val atom : IAtom = Atom(name)

    override fun intercept(handler: IInterceptor<ListWillChange<T>>): () -> Unit {
        return registerInterceptor(handler)
    }

    override fun observe(listener: (ListDidChange<T>) -> Unit, fireImmediately: Boolean): () -> Unit {
        if( fireImmediately) {
            listener(ListDidChange.splice(this, 0, list.toList(), emptyList()))
        }
        return registerListener(listener)
    }

    override fun set(index: Int, element: T) : T{
        atom.checkIfStateModificationsAreAllowed()
        val oldValue = list[index]
        var new = element
        if(oldValue!=new) {
            if (hasInterceptors()) {
                new = interceptChange(ListWillChange.update(this, index, new))?.newValue ?: new
            }
            list[index] = new
            notifyListChildUpdate(index, new, oldValue)
        }
        return oldValue
    }

    private fun notifyListChildUpdate(index: Int, newValue: T, oldValue: T) {
        atom.reportChanged()
        if( hasListeners() ) {
            notifyListeners(ListDidChange.update(this, index, newValue, oldValue))
        }
    }

    override fun spliceWithArray(index: Int, deleteCount: Int, newItems: List<T>) : List<T> {
        atom.checkIfStateModificationsAreAllowed()
        val size = list.size
        val i = when {
            index>size -> {
                size
            }
            index<0 -> {
                max(0, size+index)
            }
            else -> {
                index
            }
        }
        var d = max(0, min(deleteCount, size-i))
        var items = newItems
        if( hasInterceptors() ) {
            val change = interceptChange(ListWillChange.splice(this, i, newItems, d))
            if( change!=null ) {
                d = change.removed!!
                items = change.added!!
            }
        }

        val res = spliceItemsIntoValues(i, d, items)
        if( d!=0 || items.isNotEmpty() ) {
            notifyListSplice(i, items, res)
        }
        return res
    }

    private fun spliceItemsIntoValues(index: Int, deleteCount: Int, newItems: List<T>) : List<T> {
        val start = list.subList(0, index).toList()
        val removed = list.subList(index, index+deleteCount).toList()
        val end = list.subList(index+deleteCount, list.size).toList()
        list.clear()
        list.addAll(start)
        list.addAll(newItems)
        list.addAll(end)
        return removed
    }

    private fun notifyListSplice(index: Int, added: List<T>, removed: List<T>) {
        atom.reportChanged()
        if( hasListeners() ) {
            notifyListeners(ListDidChange.splice(this, index, added, removed))
        }
    }

    override val size: Int
        get() {
            atom.reportObserved()
            return list.size
        }

    override fun add(element: T): Boolean {
        spliceWithArray(list.size, 0, listOf(element))
        return true
    }

    override fun remove(element: T): Boolean {
        spliceWithArray(list.indexOf(element), 1, emptyList() )
        return true
    }

    override fun contains(element: T): Boolean {
        atom.reportObserved()
        return list.contains(element)
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        atom.reportObserved()
        return list.containsAll(elements)
    }

    override fun get(index: Int): T {
        atom.reportObserved()
        return list[index]
    }

    override fun indexOf(element: T): Int {
        atom.reportObserved()
        return list.indexOf(element)
    }

    override fun isEmpty(): Boolean {
        atom.reportObserved()
        return list.isEmpty()
    }

    override fun iterator(): MutableIterator<T> {
        val it = list.iterator()
        return object : MutableIterator<T> {
            override fun hasNext(): Boolean {
                atom.reportObserved()
                return it.hasNext()
            }

            override fun next(): T {
                atom.reportObserved()
                return it.next()
            }

            override fun remove() {
                throw NotImplementedError()
            }
        }
    }

    override fun lastIndexOf(element: T): Int {
        atom.reportObserved()
        return list.lastIndexOf(element)
    }

    override fun add(index: Int, element: T) {
        spliceWithArray(index, 0, listOf(element))
    }

    override fun addAll(index: Int, elements: Collection<T>): Boolean {
        spliceWithArray(index, 0, elements.toList())
        return true
    }

    override fun addAll(elements: Collection<T>): Boolean {
        spliceWithArray(list.size, 0, elements.toList())
        return true
    }

    override fun clear() {
        spliceWithArray(0, list.size, emptyList())
    }

    override fun listIterator(): MutableListIterator<T> {
        val it = list.listIterator()
        return object : MutableListIterator<T> {
            var lastIndex = -1
            override fun hasPrevious(): Boolean {
                atom.reportObserved()
                return it.hasPrevious()
            }

            override fun nextIndex(): Int {
                atom.reportObserved()
                lastIndex = it.nextIndex()
                return lastIndex
            }

            override fun previous(): T {
                atom.reportObserved()
                return it.previous()
            }

            override fun previousIndex(): Int {
                atom.reportObserved()
                lastIndex = it.previousIndex()
                return lastIndex
            }

            override fun add(element: T) {
                throw NotImplementedError()
            }

            override fun set(element: T) {
                it.set(element)
            }

            override fun hasNext(): Boolean {
                atom.reportObserved()
                return it.hasNext()
            }

            override fun next(): T {
                atom.reportObserved()
                return it.next()
            }

            override fun remove() {
                throw NotImplementedError()
            }
        }

    }

    override fun listIterator(index: Int): MutableListIterator<T> {
        throw NotImplementedError()
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        throw NotImplementedError()
    }

    override fun removeAt(index: Int): T {
        return spliceWithArray(index, 1, emptyList())[0]
    }

    override fun retainAll(elements: Collection<T>): Boolean {
        throw NotImplementedError()
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> {
        atom.reportObserved()
        return list.subList(fromIndex, toIndex)
    }

    override fun toString(): String {
        return "${atom.name}[${this.joinToString(", ")}]"
    }

    override fun getValue(thisRef: Any, property: KProperty<*>) : List<T> {
        return this
    }
    override fun setValue(thisRef: Any, property: KProperty<*>, value: List<T>) {
        replace(value)
    }
}

