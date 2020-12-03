package kobx.core

interface IReaction {
    fun dispose()
}
data class ReactionDisposer(
    val dispose: ()->Unit,
    val kobx: Reaction
)
class Reaction(
    override var name: String = "Reaction@${GlobalState.nextId()}",
    val onInvalidate: Reaction.()->Unit,
    private val errorHandler: (IDerivation.(error: Throwable)->Unit)? = null,
    override var requiresObservable: Boolean? = false
): IDerivation, IReaction {

    override var observing: MutableList<IObservable> = mutableListOf()
    override var newObserving: MutableList<IObservable>? = mutableListOf()
    override var dependenciesState: DerivationState = DerivationState.NOT_TRACKING
    var diffValue = 0
    override var runId: Int = 0
    override var unboundDepsCount: Int = 0
    override var mapId: String = "#${GlobalState.nextId()}"
    var isDisposed = false
    var isScheduled = false
    private var isTrackPending = false
    private var isRunning = false
    override var isTracing: Boolean = false

    override fun onBecomeStale() {
        schedule()
    }

    fun schedule() {
        if( !isScheduled ) {
            isScheduled = true
            GlobalState.pendingReactions += this
            runReactions()
        }
    }

    fun runReaction() {
        if( !isDisposed ) {
            GlobalState.startBatch()
            isScheduled = false
            if( shouldCompute() ) {
                isTrackPending = true
                try {
                    onInvalidate()
                } catch (e: Throwable) {
                    reportExceptionInDerivation(e)
                }
            }
            GlobalState.endBatch()
        }
    }

    fun track(fn: ()->Unit) {
        if( isDisposed ) {
            return
        }
        GlobalState.startBatch()
        isRunning = true
        val prev = GlobalState.trackingReaction
        GlobalState.trackingReaction = this
        val result = trackDerivedFunction(fn)
        GlobalState.trackingReaction = prev
        isRunning = false
        isTrackPending = false
        if( isDisposed ) {
            clearObserving()
        }
        if( result.error!=null ) {
            reportExceptionInDerivation(result.error)
        }
        GlobalState.endBatch()
    }

    private fun reportExceptionInDerivation(e: Throwable) {
        if( errorHandler!=null ) {
            errorHandler.invoke(this, e)
            return
        }
        if( GlobalState.disableErrorBoundaries) {
            throw e
        }
        if( !GlobalState.suppressReactionErrors ) {
            e.printStackTrace()
        }
//        GlobalState
    }

    override fun dispose() {
        if( !isDisposed ) {
            isDisposed = true
            if( !isRunning ){
                GlobalState.startBatch()
                clearObserving()
                GlobalState.endBatch()
            }
        }
    }

    override fun toString(): String {
        return "Reaction[${name}]"
    }

    fun getDisposer() : ReactionDisposer {
        return ReactionDisposer(this::dispose, this)
    }
}
const val MAX_REACTION_ITERATIONS = 100
var reactionScheduler: (()->Unit)->Unit = { f -> f() }

fun runReactions() {
    if( GlobalState.inBatch>0 || GlobalState.isRunningReactions ) {
        return
    }
    reactionScheduler(::runReactionsHelper)
}

fun runReactionsHelper() {
    GlobalState.isRunningReactions = true
    val allReactions = GlobalState.pendingReactions
    var iterations = 0
    while (allReactions.isNotEmpty() ) {
        if( iterations>MAX_REACTION_ITERATIONS) {
            allReactions.clear()
        }
        val list = allReactions.toList()
        allReactions.clear()
        list.forEach {
            it.runReaction()
        }
        iterations++
    }
    GlobalState.isRunningReactions = false
}
