package kobx.types

import kobx.core.GlobalState

typealias IInterceptor<T> = ((T)->T)

interface IInterceptable<T> {
    var interceptors: MutableList<IInterceptor<T>>?

    fun hasInterceptors() = interceptors!=null && interceptors!!.isNotEmpty()
    fun registerInterceptor(handler: IInterceptor<T>) : ()->Unit {
        if( interceptors==null ) {
            interceptors = mutableListOf()
        }
        interceptors!! += handler
        return {
            interceptors!!.remove(handler)
        }
    }
    fun interceptChange(change: T) : T {
        val prevU = GlobalState.untrackedStart()
        try {
            val list = interceptors?.toList() ?: emptyList()
            var result = change
            list.forEach{
                result = it(result)
            }
            return result
        } finally {
            GlobalState.untrackedEnd(prevU)
        }
    }
}