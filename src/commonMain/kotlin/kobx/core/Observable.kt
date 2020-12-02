package kobx.core

interface IDepTreeNode {
    var name: String
    var observing: MutableList<IObservable>
}

interface IObservable : IDepTreeNode {
    var diffValue: Int
    var lastAccessedBy: Int
    var isBeingObserved: Boolean

    var lowestObserverState: DerivationState
    var isPendingUnobservation: Boolean

    val observers: MutableSet<IDerivation>

    fun onBUO()
    fun onBO()

    val onBUOL: MutableSet<()->Unit>
    val onBOL: MutableSet<()->Unit>

    fun hasObservers() = observers.isNotEmpty()
    fun addObserver(node: IDerivation) {
        observers += node
        if( lowestObserverState.ordinal>node.dependenciesState.ordinal) {
            lowestObserverState = node.dependenciesState
        }
    }
    fun removeObserver(node: IDerivation) {
        observers.remove(node)
        if( observers.isEmpty() ) {
            queueForUnobservation()
        }
    }

    fun queueForUnobservation() {
        if( !isPendingUnobservation ) {
            isPendingUnobservation= true
            GlobalState.pendingUnobservations += this
        }
    }

    fun reportObserved() : Boolean {
        val derivation = GlobalState.trackingDerivation
        if( derivation!=null ) {
            if( derivation.runId!=lastAccessedBy) {
                lastAccessedBy = derivation.runId
                derivation.newObserving!! += this
                derivation.unboundDepsCount++
                if( !isBeingObserved && (GlobalState.trackingContext!=null|| GlobalState.trackingReaction!=null) ) {
                    isBeingObserved = true
                    onBO()
                }
            }
            return true
        } else if( observers.isEmpty() && GlobalState.inBatch>0 ) {
            queueForUnobservation()
        }
        return false
    }

    fun propagateChanged() {
        if( lowestObserverState==DerivationState.STALE) {
            return
        }
        lowestObserverState = DerivationState.STALE
        observers.forEach { obs->
            if( obs.dependenciesState==DerivationState.UP_TO_DATE ) {
                obs.onBecomeStale()
            }
            obs.dependenciesState = DerivationState.STALE
        }
    }

    fun propagateChangeConfirmed() {
        if( lowestObserverState==DerivationState.STALE) {
            return
        }
        lowestObserverState = DerivationState.STALE
        observers.forEach { obs->
            if( obs.dependenciesState==DerivationState.POSSIBLY_STALE ) {
                obs.dependenciesState = DerivationState.STALE
            } else if( obs.dependenciesState==DerivationState.UP_TO_DATE ) {
                lowestObserverState = DerivationState.UP_TO_DATE
            }
        }
    }

    fun propagateMaybeChanged() {
        if( lowestObserverState!=DerivationState.UP_TO_DATE) {
            return
        }
        lowestObserverState = DerivationState.POSSIBLY_STALE
        observers.forEach { obs->
            if( obs.dependenciesState==DerivationState.UP_TO_DATE ) {
                obs.dependenciesState = DerivationState.POSSIBLY_STALE
                obs.onBecomeStale()
            }
        }
    }

    fun checkIfStateReadsAreAllowed() {
        if( !GlobalState.allowStateReads && GlobalState.observablesRequiresReaction ) {
            println("Observable ${this.name} being read outside a reactive context")
        }
    }
}