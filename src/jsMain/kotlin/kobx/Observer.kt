package kobx

import kobx.core.Reaction
import react.*


fun <P : RProps> observer(
    displayName: String? = null,
    func: RBuilder.(props: P) -> Unit
): FunctionalComponent<P> {
    return functionalComponent<P>(displayName) { p ->
        val (tick, setTick) = useState(0)
        fun tick() {
            setTick(tick+1)
        }
        val r = Reaction("bla", { tick() })

        var rendering : dynamic = {}
        r.track {
            rendering = func(p)
        }
        return@functionalComponent rendering
    }
}