package com.example.fluentui.ui.system

import com.example.fluentui.ui.system.TileConfig
import com.example.fluentui.ui.system.TileSize

object GridUtils {
    const val COLUMNS = 6 // Standard 6 colonnes

    /**
     * Vérifie si deux tuiles se chevauchent
     */
    fun isOverlapping(t1: TileConfig, targetRow: Int, targetCol: Int, size: TileSize): Boolean {
        // t1 est la tuile existante dans la grille
        // target... sont les coordonnées de la tuile qu'on veut poser

        val t1Right = t1.col + t1.size.cols
        val t1Bottom = t1.row + t1.size.rows

        val targetRight = targetCol + size.cols
        val targetBottom = targetRow + size.rows

        return t1.col < targetRight &&
                t1Right > targetCol &&
                t1.row < targetBottom &&
                t1Bottom > targetRow
    }

    /**
     * Trouve la première place libre pour une nouvelle tuile
     */
    fun findFreeSlot(tiles: List<TileConfig>, size: TileSize): Pair<Int, Int> {
        for (row in 0..20) { // On cherche sur 20 lignes max
            for (col in 0..(COLUMNS - size.cols)) {
                val occupied = tiles.any { isOverlapping(it, row, col, size) }
                if (!occupied) return Pair(col, row)
            }
        }
        return Pair(0, 0)
    }
}