package kobx.core

interface IAtom: IObservable {
    fun reportChanged()

    fun checkIfStateModificationsAreAllowed() {
        if( !GlobalState.allowStateChanges && (hasObservers() || GlobalState.enforceActions==EnforceActions.Always )) {
            if( GlobalState.enforceActions!=EnforceActions.False ){
                println("Since strict-mode is enabled, changing (observed) observable values without using an action is not allowed. Tried to modify: ${this.name}")
            } else {
                println("Side effects like changing state are not allowed at this point. Are you trying to modify state from, for example, a computed value or the render function of a React component? You can wrap side effects in 'runInAction' (or decorate functions with 'action') if needed. Tried to modify: ${this.name}")
            }
        }
    }
}

open class Atom(override var name: String="Atom@${GlobalState.nextId()}"): IAtom {
    override var observing: MutableList<IObservable> = mutableListOf()
    override var isPendingUnobservation: Boolean = false
    override var isBeingObserved: Boolean = false
    override val observers: MutableSet<IDerivation> = mutableSetOf()

    override var diffValue: Int = 0
    override var lastAccessedBy: Int = 0
    override var lowestObserverState: DerivationState = DerivationState.NOT_TRACKING

    override val onBOL: MutableSet<() -> Unit> = mutableSetOf()
    override val onBUOL: MutableSet<() -> Unit> = mutableSetOf()

    override fun onBO() {
        onBOL.forEach { it() }
    }

    override fun onBUO() {
        onBUOL.forEach { it() }
    }

    override fun reportChanged() {
        GlobalState.startBatch()
        propagateChanged()
        GlobalState.endBatch()
    }

    override fun toString(): String {
        return name
    }

    companion object {
        fun make(name: String, onBecomeObservedHandler: (()->Unit)? = null, onBecomeUnobservedHandler: (()->Unit)? = null) : IAtom {
            val atom = Atom(name)
            if( onBecomeObservedHandler!=null) {
                atom.onBOL.add(onBecomeObservedHandler)
            }
            if( onBecomeUnobservedHandler!=null ) {
                atom.onBUOL.add(onBecomeUnobservedHandler)
            }
            return atom
        }
    }
}

