package kobx.types

import kobx.core.Atom
import kobx.core.GlobalState
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

data class ValueWillChange<T>(
    val obj: IObservableValue<T>,
    val newValue: T
)

data class ValueDidChange<T>(
    val obj: IObservableValue<T>,
    val newValue: T,
    val oldValue: T
)

data class ChangedValue<T>(val value: T, val hasChanged: Boolean)

interface IObservableValue<T> {
    fun get(): T
    fun set(value: T)
    fun intercept(handler: IInterceptor<ValueWillChange<T>>): ()->Unit
    fun observe(listener: (ValueDidChange<T>)->Unit, fireImmediately: Boolean=false) : ()->Unit
}

class ObservableValue<T>(
    var value: T,
    name: String = "ObservableValue@${GlobalState.nextId()}"
): Atom(name),
    IObservableValue<T>,
    IInterceptable<ValueWillChange<T>>,
    IListenable<ValueDidChange<T>>,
    ReadWriteProperty<Any, T>
{
    override var interceptors: MutableList<IInterceptor<ValueWillChange<T>>>? = null
    override var changeListeners: MutableList<(ValueDidChange<T>) -> Unit>? = null

    override fun set(value: T) {
        val (newValue, changed) = prepareNewValue(value)
        if( changed ) {
            setNewValue(newValue)
        }
    }

    fun prepareNewValue(value: T): ChangedValue<T> {
        checkIfStateModificationsAreAllowed()
        var newValue = value
        if( hasInterceptors() ) {
            newValue = interceptChange(ValueWillChange(this, newValue))?.newValue ?: newValue
        }
        return ChangedValue(newValue, newValue!=this.value)
    }

    fun setNewValue(newValue: T){
        val oldValue = value
        value = newValue
        reportChanged()
        if( hasListeners() ) {
            notifyListeners(ValueDidChange(this, newValue, oldValue))
        }
    }

    override fun get(): T {
        reportObserved()
        return value
    }

    override fun intercept(handler: IInterceptor<ValueWillChange<T>>): () -> Unit {
        return registerInterceptor(handler)
    }

    override fun observe(listener: (ValueDidChange<T>) -> Unit, fireImmediately: Boolean): () -> Unit {
        if( fireImmediately ) {
            listener(ValueDidChange(this, value, value))
        }
        return registerListener(listener)
    }

    override fun toString(): String {
        return "$name[$value]"
    }

    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        return get()
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        set(value)
    }

}