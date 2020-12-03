package kobx.core

fun<R> createAction0(actionName: String, fn: () -> R, autoAction: Boolean = false): () -> R {
    return {
        executeAction(actionName, autoAction, fn)
    }
}
fun<T1, T2, T3, R> createAction3(actionName: String, fn: (T1, T2, T3) -> R, autoAction: Boolean = false): (T1,T2,T3) -> R {
    return { a1,a2,a3->
        executeAction(actionName, autoAction, fn, a1, a2, a3)
    }
}

data class ActionRunInfo(
    val prevDerivation: IDerivation?,
    val prevAllowStateChanges: Boolean,
    val prevAllowStateReads: Boolean,
    val startTime: Long,
    var error: Throwable?,
    val parentActionId: Int,
    val actionId: Int,
    val runAsAction: Boolean = false
)

fun<R> executeAction(actionName: String, canRunAsDerivation: Boolean, fn: ()->R) : R {
    val runInfo = startAction(actionName, canRunAsDerivation)
    try {
        return fn()
    } catch (e: Throwable) {
        runInfo.error = e
        throw e
    } finally {
        endAction(runInfo)
    }
}
fun<T1, T2, T3, R> executeAction(actionName: String, canRunAsDerivation: Boolean, fn: (T1,T2,T3)->R, a1:T1,a2:T2,a3:T3) : R {
    val runInfo = startAction(actionName, canRunAsDerivation)
    try {
        return fn(a1, a2, a3)
    } catch (e:Throwable) {
        runInfo.error = e
        throw e
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
    val result = ActionRunInfo(prevDerivation, prevAllowStateChanges, prevAllowStateReads, startTime, null, GlobalState.currentActionId, GlobalState.nextActionId++)
    GlobalState.currentActionId = result.actionId
    return result
}

fun endAction(runInfo: ActionRunInfo) {
    if( GlobalState.currentActionId!=runInfo.actionId) {
        throw IllegalStateException()
    }
    GlobalState.currentActionId = runInfo.parentActionId
    if( runInfo.error!=null ) {
        GlobalState.suppressReactionErrors = true
    }
    GlobalState.allowStateChangesEnd(runInfo.prevAllowStateChanges)
    GlobalState.allowStateReadsEnd(runInfo.prevAllowStateReads)
    GlobalState.endBatch()
    if( runInfo.runAsAction ) {
        GlobalState.untrackedEnd(runInfo.prevDerivation)
    }
    GlobalState.suppressReactionErrors = false

}

