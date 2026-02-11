package com.chess.assistant.core.chess

/**
 * 位置数据类
 * @param x 横坐标 (0-8，从左到右)
 * @param y 纵坐标 (0-9，从上到下)
 */
data class Position(val x: Int, val y: Int) {

    init {
        require(x in 0..8) { "X coordinate must be between 0 and 8, got $x" }
        require(y in 0..9) { "Y coordinate must be between 0 and 9, got $y" }
    }

    /**
     * 检查位置是否在九宫格内
     */
    fun isInPalace(color: PieceColor): Boolean {
        return when (color) {
            PieceColor.RED -> x in 3..5 && y in 0..2
            PieceColor.BLACK -> x in 3..5 && y in 7..9
        }
    }

    /**
     * 检查是否过河
     */
    fun isAcrossRiver(color: PieceColor): Boolean {
        return when (color) {
            PieceColor.RED -> y > 4
            PieceColor.BLACK -> y < 5
        }
    }

    override fun toString(): String {
        return "($x, $y)"
    }

    companion object {
        /**
         * 检查两个位置是否相邻（水平或垂直）
         */
        fun isAdjacent(pos1: Position, pos2: Position): Boolean {
            val dx = kotlin.math.abs(pos1.x - pos2.x)
            val dy = kotlin.math.abs(pos1.y - pos2.y)
            return (dx == 1 && dy == 0) || (dx == 0 && dy == 1)
        }
    }
}
