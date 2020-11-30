package kobx.types

import kobx.core.GlobalState

interface IListenable<T> {
    var changeListeners: MutableList<(T)->Unit>?

    fun hasListeners() = changeListeners!=null && changeListeners!!.isNotEmpty()

    fun registerListener(handler: (T)->Unit) : ()->Unit {
        if( changeListeners==null ) {
            changeListeners = mutableListOf()
        }
        changeListeners!! += handler
        return {
            changeListeners!!.remove(handler)
        }
    }

    fun notifyListeners(change: T) {
        val prevU = GlobalState.untrackedStart()
        changeListeners?.forEach { it(change) }
        GlobalState.untrackedEnd(prevU)
    }
}