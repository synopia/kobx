package kobx.core

data class ResultOrError<out T>(val result: T? =null, val error: Throwable? =null)

enum class DerivationState {
    NOT_TRACKING,
    UP_TO_DATE,
    POSSIBLY_STALE,
    STALE
}

interface IDerivation: IDepTreeNode {
    override var observing: MutableList<IObservable>
    var newObserving: MutableList<IObservable>?
    var dependenciesState: DerivationState

    var runId: Int
    var unboundDepsCount: Int
    var mapId: String
    var isTracing: Boolean
    var requiresObservable: Boolean?

    fun onBecomeStale()

    fun shouldCompute(): Boolean {
        when(dependenciesState) {
            DerivationState.UP_TO_DATE->return false
            DerivationState.NOT_TRACKING->return true
            DerivationState.STALE->return true
            DerivationState.POSSIBLY_STALE->{
                val prevAllowStateReads = GlobalState.allowStateReadsStart(true)
                val prevUntracked = GlobalState.untrackedStart()
                observing.forEach { obs->
                    if( obs is ComputedValue<*> ) {
                        if( GlobalState.disableErrorBoundaries ) {
                            obs.get()
                        } else {
                            try {
                                obs.get()
                            } catch (e:Throwable) {
                                GlobalState.untrackedEnd(prevUntracked)
                                GlobalState.allowStateReadsEnd(prevAllowStateReads)
                                return true
                            }
                        }
                        if( dependenciesState==DerivationState.STALE ) {
                            GlobalState.untrackedEnd(prevUntracked)
                            GlobalState.allowStateReadsEnd(prevAllowStateReads)
                            return true
                        }
                    }
                }
                changeDependenciesStateTo0()
                GlobalState.untrackedEnd(prevUntracked)
                GlobalState.allowStateReadsEnd(prevAllowStateReads)
                return false
            }
        }
    }

    fun isComputingDerivation() = GlobalState.trackingDerivation!=null

    fun <T> trackDerivedFunction(f: ()->T): ResultOrError<T> {
        val prevAllowStateReads = GlobalState.allowStateReadsStart(true)
        changeDependenciesStateTo0()
        newObserving = mutableListOf()
        unboundDepsCount = 0
        runId = ++GlobalState.runId
        val prevTracking = GlobalState.trackingDerivation
        GlobalState.trackingDerivation = this
        GlobalState.inBatch++
        val result = if( GlobalState.disableErrorBoundaries ) {
            ResultOrError(f())
        } else {
            try {
                ResultOrError(f())
            } catch (e:Throwable) {
                ResultOrError(null, e)
            }
        }
        GlobalState.inBatch--
        GlobalState.trackingDerivation = prevTracking
        bindDependencies()

        warnAboutDerivationWithoutDependencies()
        GlobalState.allowStateReadsEnd(prevAllowStateReads)
        return result
    }

    fun warnAboutDerivationWithoutDependencies() {
        if( observing.isNotEmpty() ) {
            return
        }
        if( GlobalState.reactionRequiresObservable || requiresObservable==true ) {
            println("Derivation $name is created/updated without reading any observable value")
        }
    }

    fun bindDependencies() {
        val prevObserving = observing
        observing = newObserving!!

        var lowestNewObservingDerivationState = DerivationState.UP_TO_DATE
        var i0 = 0
        for( i in (0 until unboundDepsCount)) {
            val dep = observing[i]
            if( dep.diffValue==0 ) {
                dep.diffValue = 1
                if( i0!=i ) {
                    observing[i0] = dep
                }
                i0 ++
            }
            if( dep is IDerivation) {
                if (dep.dependenciesState.ordinal > lowestNewObservingDerivationState.ordinal) {
                    lowestNewObservingDerivationState = dep.dependenciesState
                }
            }
        }
        observing = observing.subList(0, i0)
        newObserving = null

        for( i in (prevObserving.size-1 downTo 0)) {
            val dep = prevObserving[i]
            if( dep.diffValue==0 ) {
                dep.removeObserver(this)
            }
            dep.diffValue = 0
        }

        for( i in (i0-1 downTo 0)) {
            val dep = observing[i]
            if( dep.diffValue==1 ){
                dep.diffValue = 0
                dep.addObserver(this)
            }
        }

        if( lowestNewObservingDerivationState!=DerivationState.UP_TO_DATE ) {
            dependenciesState = lowestNewObservingDerivationState
            onBecomeStale()
        }
    }

    fun clearObserving() {
        val obs = observing.toList()
        observing.clear()
        obs.forEach { it.removeObserver(this) }
        dependenciesState = DerivationState.NOT_TRACKING
    }

    fun changeDependenciesStateTo0() {
        if( dependenciesState==DerivationState.UP_TO_DATE ) {
            return
        }
        dependenciesState = DerivationState.UP_TO_DATE
        for( i in (observing.size-1 downTo 0)) {
            observing[i].lowestObserverState = DerivationState.UP_TO_DATE
        }
    }
}