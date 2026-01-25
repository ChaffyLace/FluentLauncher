package com.example.fluentui.ui.system

import kotlin.math.max

object GridEngine {
    const val COLUMNS = 6

    // Déplace une tuile et résout les conflits (Push Down Logic)
    fun moveTile(
        tiles: List<TileConfig>,
        tileId: String,
        targetCol: Int,
        targetRow: Int
    ): List<TileConfig> {
        val movingTile = tiles.find { it.id == tileId } ?: return tiles
        val others = tiles.filter { it.id != tileId }.toMutableList()

        // 1. Clamping : On s'assure que la tuile reste dans la grille horizontale
        val safeCol = targetCol.coerceIn(0, COLUMNS - movingTile.size.cols)
        val safeRow = targetRow.coerceAtLeast(0)

        // 2. On place la tuile candidate
        var current = movingTile.copy(col = safeCol, row = safeRow)

        // 3. Résolution des collisions (Algorithme "Bulldozer")
        // On place la tuile, et on regarde qui elle écrase.
        // Ceux qu'elle écrase sont poussés vers le bas.
        val result = mutableListOf<TileConfig>()
        result.add(current)

        // On trie les autres par hauteur pour gérer les cascades proprement
        val sortedOthers = others.sortedWith(compareBy({ it.row }, { it.col }))

        for (other in sortedOthers) {
            var candidate = other
            // Tant que 'candidate' touche n'importe quelle tuile déjà placée (dont celle qu'on bouge)
            // On la descend d'un cran
            while (hasCollision(candidate, result)) {
                candidate = candidate.copy(row = candidate.row + 1)
            }
            result.add(candidate)
        }

        return result
    }

    // Est-ce que 'candidate' touche une des tuiles dans 'others' ?
    private fun hasCollision(candidate: TileConfig, others: List<TileConfig>): Boolean {
        return others.any { existing ->
            isOverlapping(candidate, existing)
        }
    }

    // Maths AABB (Axis-Aligned Bounding Box)
    private fun isOverlapping(a: TileConfig, b: TileConfig): Boolean {
        val aLeft = a.col
        val aRight = a.col + a.size.cols
        val aTop = a.row
        val aBottom = a.row + a.size.rows

        val bLeft = b.col
        val bRight = b.col + b.size.cols
        val bTop = b.row
        val bBottom = b.row + b.size.rows

        return aLeft < bRight && aRight > bLeft && aTop < bBottom && aBottom > bTop
    }

    // Pour trouver une place libre lors de l'ajout
    fun findFirstFreeSlot(tiles: List<TileConfig>, size: TileSize): Pair<Int, Int> {
        for (r in 0..100) {
            for (c in 0..(COLUMNS - size.cols)) {
                val candidate = TileConfig("temp", "", null, size, TileType.APPLICATION, "", c, r)
                if (!hasCollision(candidate, tiles)) return Pair(c, r)
            }
        }
        return Pair(0, 0)
    }
}