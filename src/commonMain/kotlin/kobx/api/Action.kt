package kobx.api

import kobx.core.executeAction

fun <R> runInAction(fn: ()->R ): R {
    return executeAction("", false, fn)
}