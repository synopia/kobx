package kobx.types

import kobx.core.Atom
import kobx.core.GlobalState
import kobx.core.IObservable
import kotlinx.serialization.*
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeStructure
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

data class ValueWillChange<T>(
    val obj: IObservableValue<T>,
    val newValue: T
)

object ValueDidChangeSerializerInt: KSerializer<ValueDidChange<Int>> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("VDC") {
        element<Int>("i")
        element<Int>("n")
        element<Int?>("o")
    }

    override fun deserialize(decoder: Decoder): ValueDidChange<Int> {

    }

    @ExperimentalSerializationApi
    override fun serialize(encoder: Encoder, value: ValueDidChange<Int>) {
        encoder.encodeStructure(descriptor) {
            encodeIntElement(descriptor, 0, 0)
            encodeIntElement(descriptor, 1, value.newValue)
            encodeNullableSerializableElement(descriptor, 2, Int.serializer(), value.oldValue)
        }
    }
}

@Serializable(with = ValueDidChangeSerializerInt::class)
data class ValueDidChange<out T>(
    @Contextual val obj: ObservableValue<out T>,
    val newValue: T,
    val oldValue: T?
) : DidChange()

internal data class ChangedValue<T>(val value: T, val hasChanged: Boolean)

interface IObservableValue<T> {
    fun get(): T
    fun set(value: T)
    fun intercept(handler: IInterceptor<ValueWillChange<T>>): ()->Unit
    fun observe(listener: (ValueDidChange<T>)->Unit, fireImmediately: Boolean=false) : ()->Unit
}

class ObservableValue<T>(
    value: T,
    name: String = "ObservableValue@${GlobalState.nextId()}",
    val enhancer: IEnhancer<T> = { it }
): Atom(name),
    IObservableValue<T>,
    IInterceptable<ValueWillChange<T>>,
    IListenable<ValueDidChange<T>>,
    ReadWriteProperty<Any, T>
{
    var value: T = enhancer(value)
    override var interceptors: MutableList<IInterceptor<ValueWillChange<T>>>? = null
    override var changeListeners: MutableList<(ValueDidChange<T>) -> Unit>? = null

    override fun set(value: T) {
        val (newValue, changed) = prepareNewValue(value)
        if( changed ) {
            setNewValue(newValue)
        }
    }

    internal fun prepareNewValue(value: T): ChangedValue<T> {
        checkIfStateModificationsAreAllowed()
        var newValue = value
        if( hasInterceptors() ) {
            newValue = interceptChange(ValueWillChange(this, newValue))?.newValue ?: newValue
        }
        newValue = enhancer(newValue)
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