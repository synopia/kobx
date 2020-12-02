package kobx.api

object Kobx {
    fun <T> observable(value: T) = Observable.box(value)
    fun <T> observable(list: List<T>) = Observable.list(list)

}