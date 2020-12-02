package kobx.api

import kobx.core.GlobalState

fun<T> Kobx.transaction(action: ()->T): T {
    GlobalState.startBatch()
    try {
        return action()
    } finally {
        GlobalState.endBatch()
    }
}