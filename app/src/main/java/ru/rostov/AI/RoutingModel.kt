package ru.rostov.AI

import java.util.PriorityQueue
import kotlin.math.pow

data class Edge(
    val id: String,
    val length: Double,            // L(e)
    val time: Double,              // T(e)
    val curbHeight: Double,        // H_curb(e)
    val accessibility: List<Double>, // p_i(e)
    val risk: Double,              // R_risk(e)
    val confidence: Double         // C_conf(e)
)

data class EdgeRef(
    val edge: Edge,
    val target: Node
)

data class Node(
    val id: String,
    val edges: MutableList<EdgeRef> = mutableListOf()
)

data class UserProfile(
    val weights: List<Double>,  // w_i
    val curbSafe: Double,       // H_safe
    val curbMax: Double,        // H_max
    val lambdaL: Double,
    val lambdaT: Double,
    val lambdaA: Double,
    val lambdaB: Double,
    val lambdaR: Double,
    val lambdaC: Double
)


fun accessibilityPenalty(edge: Edge, profile: UserProfile): Double {
    var sum = 0.0
    for (i in edge.accessibility.indices) {
        val p = edge.accessibility[i]
        val w = profile.weights.getOrElse(i) { 0.0 }
        sum += w * (1.0 - p)
    }
    return sum
}

fun curbPenalty(height: Double, profile: UserProfile): Double {
    if (height <= profile.curbSafe) return 0.0
    if (height > profile.curbMax) return Double.POSITIVE_INFINITY

    val beta = 1.0
    val gamma = 2.0
    return beta * (height - profile.curbSafe).pow(gamma)
}


fun edgeCost(edge: Edge, profile: UserProfile): Double {
    val L = edge.length
    val T = edge.time
    val A = accessibilityPenalty(edge, profile)
    val B = curbPenalty(edge.curbHeight, profile)
    val R = edge.risk
    val C = 1.0 - edge.confidence

    if (B.isInfinite()) return Double.POSITIVE_INFINITY

    return profile.lambdaL * L +
            profile.lambdaT * T +
            profile.lambdaA * A +
            profile.lambdaB * B +
            profile.lambdaR * R +
            profile.lambdaC * C
}


fun heuristic(a: Node, b: Node): Double {
    return 0.0
}

data class RouteResult(
    val path: List<Node>,
    val cost: Double   // C*
)

fun findAccessibleRoute(
    start: Node,
    goal: Node,
    profile: UserProfile
): RouteResult {

    val open = PriorityQueue(compareBy<Pair<Node, Double>> { it.second })
    val cameFrom = mutableMapOf<Node, Node>()
    val gScore = mutableMapOf<Node, Double>()

    gScore[start] = 0.0
    open.add(start to heuristic(start, goal))

    while (open.isNotEmpty()) {
        val current = open.poll().first

        if (current == goal) {
            val path = reconstructPath(cameFrom, current)
            val cost = gScore[current] ?: Double.POSITIVE_INFINITY
            return RouteResult(path, cost)  // ← C*
        }

        for (ref in current.edges) {
            val edge = ref.edge
            val neighbor = ref.target

            val cost = edgeCost(edge, profile)
            if (cost.isInfinite()) continue

            val tentative = gScore.getOrDefault(current, Double.POSITIVE_INFINITY) + cost

            if (tentative < gScore.getOrDefault(neighbor, Double.POSITIVE_INFINITY)) {
                cameFrom[neighbor] = current
                gScore[neighbor] = tentative
                val f = tentative + heuristic(neighbor, goal)
                open.add(neighbor to f)
            }
        }
    }

    return RouteResult(emptyList(), Double.POSITIVE_INFINITY)
}


fun reconstructPath(
    cameFrom: Map<Node, Node>,
    current: Node
): List<Node> {
    val path = mutableListOf<Node>()
    var cur: Node? = current
    while (cur != null) {
        path.add(cur)
        cur = cameFrom[cur]
    }
    return path.reversed()
}

// Пример использования Математической Модеил в коде

/*
val result = findAccessibleRoute(startNode, endNode, profile)
println("Cost C* = ${result.cost}")
println("Path length = ${result.path.size}")
 */