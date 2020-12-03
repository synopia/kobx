package kobx.api

import kobx.core.*
import kobx.core.GlobalState

data class AutorunOptions(
    val delay: Int = 0,
    val name: String? = null,
    val requiresObservable: Boolean? = null,
    val scheduler: ((()->Unit)->Any)? = null,
    val onError: (IDerivation.(Throwable)->Unit)? = null,
    val fireImmediately: Boolean? = null
)

fun Kobx.autorun(opts: AutorunOptions=AutorunOptions(), view: (IReaction)->Unit) : ReactionDisposer {
    val name = opts.name ?: "Autorun@${GlobalState.nextId()}"
    val runSync = opts.scheduler==null && opts.delay==0

    fun reactionRunner(reaction: Reaction) {
        view(reaction)
    }
    val reaction = if( runSync ) {
        Reaction(name,
            { this.track { reactionRunner(this) }},
            opts.onError,
            opts.requiresObservable)
    } else {
        throw NotImplementedError()
    }

    reaction.schedule()

    return reaction.getDisposer()
}

fun<T> Kobx.reaction(
    expression: IReaction.() -> T,
    effect: IReaction.(arg: T, prev: T) -> Unit,
    opts: AutorunOptions = AutorunOptions()
) : ReactionDisposer {
    val name = opts.name ?: "Reaction@${GlobalState.nextId()}"
    val effectAction = createAction3(name,
        if( opts.onError!=null ) wrapErrorHandler(opts.onError, effect) else effect
    )
    val runSync = opts.scheduler==null && opts.delay==0

    var firstTime = true
    var value: T? = null
    var oldValue: T? = null

    fun reactionRunner(reaction: Reaction) {
        var changed = false
        if( reaction.isDisposed ){
            return
        }
        reaction.track {
            val nextValue = GlobalState.allowStateChanges(false) { expression(reaction) }
            changed = firstTime || value!=nextValue
            oldValue = value
            value = nextValue
        }
        if( firstTime && opts.fireImmediately==true ) {
            effectAction(reaction, value!!, value!!)
        } else if( !firstTime && changed ) {
            effectAction(reaction, value!!, oldValue!!)
        }
        firstTime = false
    }

    val reaction = Reaction(name, {
        if( firstTime || runSync ) {
            reactionRunner(this)
        } else if( !isScheduled ) {
            isScheduled = true
            throw NotImplementedError()
        }
    }, opts.onError, opts.requiresObservable)

    reaction.schedule()

    return reaction.getDisposer()
}

private fun<T> wrapErrorHandler(
    errorHandler: (IDerivation.(Error)->Unit),
    effect: Reaction.(arg: T, prev: T) -> Unit) : Reaction.(arg: T, prev: T) -> Unit {
    return { arg, prev ->
        try {
            effect(arg, prev)
        } catch (e:Error) {
            errorHandler(e)
        }
    }
}