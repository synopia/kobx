package kobx.api

import kobx.core.IDepTreeNode

data class DependencyTree(
    val name: String,
    val dependencies: List<DependencyTree>
)

fun IDepTreeNode.toDependencyTree(): DependencyTree {
    return DependencyTree(name, observing.map{ it.toDependencyTree() })
}