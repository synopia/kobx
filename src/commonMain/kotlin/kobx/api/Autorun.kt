package kobx.api

import kobx.core.GlobalState
import kobx.core.IReaction
import kobx.core.Reaction
import kobx.core.ReactionDisposer

fun Kobx.autorun(view: (IReaction)->Unit) : ReactionDisposer {
    val name = "Autorun@${GlobalState.nextId()}"
    val reaction = Reaction(name, { this.track { view(this) }})
    reaction.schedule()

    return reaction.getDisposer()
}