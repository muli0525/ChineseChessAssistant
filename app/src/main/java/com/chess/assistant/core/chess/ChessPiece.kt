package com.chess.assistant.core.chess

/**
 * 象棋棋子数据类
 */
data class ChessPiece(
    val type: PieceType,
    val color: PieceColor,
    val position: Position
) {
    /**
     * 获取棋子显示符号
     */
    fun getSymbol(): String {
        return when (type) {
            PieceType.JU -> "車"
            PieceType.MA -> "馬"
            PieceType.XIANG -> "象"
            PieceType.SHI -> "士"
            PieceType.JIANG -> if (color == PieceColor.RED) "帥" else "將"
            PieceType.PAO -> "炮"
            PieceType.BING -> "兵"
        }
    }

    /**
     * 获取棋子简写符号（用于走棋记录）
     */
    fun getShortSymbol(): String {
        return when (type) {
            PieceType.JU -> "車"
            PieceType.MA -> "馬"
            PieceType.XIANG -> "象"
            PieceType.SHI -> "士"
            PieceType.JIANG -> if (color == PieceColor.RED) "帥" else "將"
            PieceType.PAO -> "炮"
            PieceType.BING -> "兵"
        }
    }

    /**
     * 获取英文符号（用于调试）
     */
    fun getEnglishSymbol(): String {
        return when (type) {
            PieceType.JU -> "R"
            PieceType.MA -> "N"
            PieceType.XIANG -> "B"
            PieceType.SHI -> "A"
            PieceType.JIANG -> "K"
            PieceType.PAO -> "C"
            PieceType.BING -> "P"
        } + if (color == PieceColor.RED) " (r)" else " (b)"
    }

    /**
     * 检查棋子是否在正确位置
     */
    fun isInCorrectPosition(): Boolean {
        return when (type) {
            PieceType.JIANG -> position.isInPalace(color)
            PieceType.SHI -> position.isInPalace(color)
            PieceType.XIANG -> !position.isAcrossRiver(color)
            else -> true
        }
    }

    /**
     * 创建带新位置的棋子
     */
    fun withPosition(newPosition: Position): ChessPiece {
        return copy(position = newPosition)
    }

    override fun toString(): String {
        return "${color.name} ${type.name} at $position"
    }
}
