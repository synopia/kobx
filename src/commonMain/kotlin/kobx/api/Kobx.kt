package kobx.api

import kobx.core.ComputedValue

object Kobx {
    fun <T> observable(value: T) = Observable.box(value)
    fun <T> observable(list: List<T>) = Observable.list(list)

    fun<T> computed(get: ()->T) = Computed.computed(get)
}