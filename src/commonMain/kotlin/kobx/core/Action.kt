package kobx.core

fun<R> createAction0(actionName: String, fn: () -> R, autoAction: Boolean = false): () -> R {
    return {
        executeAction0(actionName, autoAction, fn)
    }
}

data class ActionRunInfo(
    val prevDerivation: IDerivation?,
    val prevAllowStateChanges: Boolean,
    val prevAllowStateReads: Boolean,
    val startTime: Long,
    val parentActionId: Int,
    val actionId: Int,
    val runAsAction: Boolean = false
)

fun<R> executeAction0(actionName: String, canRunAsDerivation: Boolean, fn: ()->R) : R {
    val runInfo = startAction(actionName, canRunAsDerivation)
    try {
        return fn()
    } finally {
        endAction(runInfo)
    }
}

fun startAction(actionName: String, canRunAsDerivation: Boolean): ActionRunInfo {
    val startTime = 0L
    val prevDerivation = GlobalState.trackingDerivation
    val runAsAction = !canRunAsDerivation || prevDerivation==null
    GlobalState.startBatch()
    var prevAllowStateChanges = GlobalState.allowStateChanges
    if( runAsAction ) {
        GlobalState.untrackedStart()
        prevAllowStateChanges = GlobalState.allowStateChangesStart(true)
    }
    val prevAllowStateReads = GlobalState.allowStateReadsStart(true)
    val result = ActionRunInfo(prevDerivation, prevAllowStateChanges, prevAllowStateReads, startTime, GlobalState.currentActionId, GlobalState.nextActionId++)
    GlobalState.currentActionId = result.actionId
    return result
}

fun endAction(runInfo: ActionRunInfo) {
    if( GlobalState.currentActionId!=runInfo.actionId) {
        throw IllegalStateException()
    }
    GlobalState.currentActionId = runInfo.parentActionId
    GlobalState.allowStateChangesEnd(runInfo.prevAllowStateChanges)
    GlobalState.allowStateReadsEnd(runInfo.prevAllowStateReads)
    GlobalState.endBatch()
    if( runInfo.runAsAction ) {
        GlobalState.untrackedEnd(runInfo.prevDerivation)
    }
}

