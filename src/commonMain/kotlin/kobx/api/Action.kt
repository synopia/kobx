package kobx.api

import kobx.core.executeAction0

fun <R> runInAction(fn: ()->R ): R {
    return executeAction0("", false, fn)
}