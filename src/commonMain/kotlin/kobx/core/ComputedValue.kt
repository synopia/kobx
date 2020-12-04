package kobx.core

import kobx.api.Kobx
import kobx.api.autorun
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

interface IComputedValue<T> {
    fun get(): T
    fun set(value: T)
    fun observe(listener: (ComputedDidChange<T>)->Unit, fireImmediately: Boolean = false) : ReactionDisposer
}

data class ComputedDidChange<T>(val newValue: T, val oldValue: T?)

data class ComputedValueOptions<T>(
    val get: ()->T,
    val set: ((T)->Unit)? = null,
    val name: String? = null,
    val context: Any? = null,
    val requiresReaction: Boolean = false,
    val keepAlive: Boolean = true
)


class ComputedValue<T>(opts: ComputedValueOptions<T>):
    IObservable, IComputedValue<T>, IDerivation, ReadWriteProperty<Any, T> {
    override var dependenciesState: DerivationState = DerivationState.NOT_TRACKING
    override var observing: MutableList<IObservable> = mutableListOf()
    override var newObserving: MutableList<IObservable>? = null
    override var isBeingObserved: Boolean = false
    override var isPendingUnobservation: Boolean = false
    override val observers: MutableSet<IDerivation> = mutableSetOf()
    override var diffValue: Int = 0
    override var runId: Int = 0
    override var lastAccessedBy: Int = 0
    override var lowestObserverState: DerivationState = DerivationState.UP_TO_DATE
    override var unboundDepsCount: Int = 0
    override var mapId: String = "#${GlobalState.nextId()}"
    internal var value: ResultOrError<T>? = null
    override var name: String = opts.name ?: "ComputedValue@${GlobalState.nextId()}"
    private var isComputing: Boolean = false
    var isRunningSetter: Boolean = false
    var derivation: ()->T = opts.get
    private var setter: ((T)->Unit)? = null
    override var isTracing: Boolean = false
    private var requiresReaction: Boolean = opts.requiresReaction
    private var keepAlive: Boolean = opts.keepAlive
    override var requiresObservable: Boolean? = true
    override var onBUOL: MutableSet<() -> Unit> = mutableSetOf()
    override var onBOL: MutableSet<() -> Unit> = mutableSetOf()

    init {
        if( opts.set!=null ) {
//            setter = createAction("${name}-setter", opts.set)
        }
    }

    override fun onBecomeStale() {
        propagateMaybeChanged()
    }

    override fun onBO() {
        onBOL.forEach { it() }
    }

    override fun onBUO() {
        onBUOL.forEach { it() }
    }

    override fun get(): T {
        if( isComputing ) {
            throw IllegalStateException()
        }
        if( GlobalState.inBatch==0 && observers.isEmpty() && !keepAlive ) {
            if( shouldCompute() ) {
                warnAboutUntrackedRead()
                GlobalState.startBatch()
                value = computeValue(false)
                GlobalState.endBatch()
            }
        } else {
            reportObserved()
            if( shouldCompute() ) {
                val prev = GlobalState.trackingContext
                if( keepAlive && prev==null ) {
                    GlobalState.trackingContext = this
                }
                if( trackAndCompute() ) {
                    propagateChangeConfirmed()
                }
                GlobalState.trackingContext = prev
            }
        }
        if( value?.error!=null ) {
            throw value!!.error!!
        }
        return value!!.result!!
    }

    override fun set(value: T) {
        if( setter!=null ) {
            if( isRunningSetter ) {
                throw IllegalStateException()
            }
            isRunningSetter = true
            setter!!(value)
            isRunningSetter = false
        }
    }

    private fun trackAndCompute() : Boolean {
        val oldValue = value
        val wasSuspended = dependenciesState==DerivationState.NOT_TRACKING
        val newValue = computeValue(true)
        val changed = wasSuspended || oldValue!=newValue
        if( changed ) {
            value = newValue
        }
        return changed
    }

    private fun computeValue(track: Boolean) : ResultOrError<T> {
        isComputing = true
        val prev = GlobalState.allowStateChangesStart(false)
        val result = if( track ){
            trackDerivedFunction(derivation)
        } else {
            if( GlobalState.disableErrorBoundaries ){
                ResultOrError(derivation())
            } else {
                try {
                    ResultOrError(derivation())
                } catch (e:Throwable){
                    ResultOrError(null, e)
                }
            }
        }
        GlobalState.allowStateChangesEnd(prev)
        isComputing = false
        return result
    }

    fun suspend() {
        if( !keepAlive ) {
            clearObserving()
            value = null
        }
    }

    override fun observe(listener: (ComputedDidChange<T>) -> Unit, fireImmediately: Boolean) : ReactionDisposer {
        var firstTime = true
        var prevValue : T? = null
        return Kobx.autorun {
            val newValue = get()
            if( !firstTime || fireImmediately ){
                val prevU = GlobalState.untrackedStart()
                listener(ComputedDidChange(newValue, prevValue))
                GlobalState.untrackedEnd(prevU)
            }
            firstTime =false
            prevValue = newValue
        }
    }

    private fun warnAboutUntrackedRead() {
        if( requiresReaction ) {
            throw IllegalStateException("ComputedValue $name is read outside a reactive context")
        }
        if( isTracing ) {
            println("ComputedValue $name is being read outside a reactive context. Doing a full recompute")
        }
        if( GlobalState.computedRequiresReaction ) {
            println("ComputedValue $name is being read outside a reactive context. Doing a full recompute")
        }
    }

    override fun toString(): String {
        return "$name[$derivation]"
    }


    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        return get()
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        set(value)
    }

}