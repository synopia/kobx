package kobx.core

enum class EnforceActions {
    False,
    True,
    Always
}

internal var GlobalState = State()

fun resetKobx() {
    GlobalState = State()
}

class State {
    var suppressReactionErrors: Boolean = false
    var inBatch: Int = 0
    var trackingDerivation: IDerivation? = null
    var trackingContext: IComputedValue<*>? = null
    var trackingReaction: Reaction? = null
    val pendingUnobservations = mutableListOf<IObservable>()
    val pendingReactions = mutableListOf<Reaction>()
    var isRunningReactions = false
    var allowStateChanges = false
    var allowStateReads = true
    var guid = 0
    var disableErrorBoundaries: Boolean = false
    var enforceActions = EnforceActions.True
    var computedRequiresReaction = false
    var reactionRequiresObservable = false
    var observablesRequiresReaction = true
    var runId = 0
    var currentActionId = 0
    var nextActionId = 1

    fun nextId() : Int {
        return ++guid
    }

    fun startBatch() {
        inBatch++
    }

    fun endBatch() {
        inBatch--
        if( inBatch==0 ) {
            runReactions()

            pendingUnobservations.forEach { obs->
                obs.isPendingUnobservation = false
                if( obs.observers.isEmpty() ) {
                    if( obs.isBeingObserved ) {
                        obs.isBeingObserved = false
                        obs.onBUO()
                    }
                    if( obs is ComputedValue<*> ) {
                        obs.suspend()
                    }
                }
            }
            pendingUnobservations.clear()
        }
    }

    fun <T> untracked(action: ()->T): T {
        val prev = untrackedStart()
        try {
            return action()
        } finally {
            untrackedEnd(prev)
        }
    }

    fun untrackedStart() : IDerivation? {
        val prev = GlobalState.trackingDerivation
        GlobalState.trackingDerivation = null
        return prev
    }
    fun untrackedEnd(prev: IDerivation?) {
        GlobalState.trackingDerivation = prev
    }

    fun allowStateReadsStart(allowStateReads: Boolean) : Boolean {
        val prev = GlobalState.allowStateReads
        GlobalState.allowStateReads = allowStateReads
        return prev
    }
    fun allowStateReadsEnd(prev: Boolean) {
        GlobalState.allowStateReads = prev
    }

    fun <T> allowStateChanges(allowStateChanges: Boolean, func: ()->T): T {
        val prev = allowStateChangesStart(allowStateChanges)
        try {
            return func()
        } finally {
            allowStateChangesEnd(prev)
        }
    }

    fun allowStateChangesStart(allowStateChanges: Boolean) : Boolean{
        val prev = this.allowStateChanges
        this.allowStateChanges = allowStateChanges
        return prev
    }

    fun allowStateChangesEnd(prev:Boolean) {
        allowStateChanges = prev
    }
}