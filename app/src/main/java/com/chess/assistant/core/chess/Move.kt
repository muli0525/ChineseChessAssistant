package com.chess.assistant.core.chess

/**
 * 象棋走棋数据类
 */
data class Move(
    val from: Position,
    val to: Position,
    val piece: ChessPiece,
    val capturedPiece: ChessPiece? = null,
    val isCheck: Boolean = false,
    val isCheckmate: Boolean = false
) {
    /**
     * 获取走棋记录（中文）
     */
    val notation: String
        get() = createNotation()

    /**
     * 获取走棋描述
     */
    val description: String
        get() = getMoveDescription()

    /**
     * 创建走棋记录
     */
    private fun createNotation(): String {
        val notationSymbol = getNotationSymbol(piece.type, piece.color)
        val fromFile = (from.x + 1).toString()
        val toFile = (to.x + 1).toString()

        return when (piece.type) {
            PieceType.JU, PieceType.MA, PieceType.XIANG, PieceType.SHI -> {
                "$notationSymbol$fromFile-${toFile}"
            }
            PieceType.JIANG -> {
                if (capturedPiece != null) {
                    "吃${notationSymbol}${toFile}"
                } else {
                    "平$toFile"
                }
            }
            PieceType.PAO -> {
                if (capturedPiece != null) {
                    "吃${notationSymbol}${toFile}"
                } else {
                    "平$toFile"
                }
            }
            PieceType.BING -> {
                val action = if (capturedPiece != null) "吃" else if (from.y == 3 && to.y == 4) "进" else if (from.y == 4 && to.y == 3) "退" else "平"
                "$action$toFile"
            }
        }
    }

    /**
     * 获取走棋描述
     */
    private fun getMoveDescription(): String {
        val pieceName = piece.getSymbol()
        val fromX = from.x + 1
        val fromY = 10 - from.y
        val toX = to.x + 1
        val toY = 10 - to.y
        val action = if (capturedPiece != null) "吃子" else "移动"

        return "$pieceName ($fromX,$fromY) $action 到 ($toX,$toY)"
    }

    companion object {
        /**
         * 获取棋子符号
         */
        private fun getNotationSymbol(type: PieceType, color: PieceColor): String {
            return when (type) {
                PieceType.JU -> "車"
                PieceType.MA -> "馬"
                PieceType.XIANG -> "象"
                PieceType.SHI -> "士"
                PieceType.JIANG -> if (color == PieceColor.RED) "帥" else "將"
                PieceType.PAO -> "炮"
                PieceType.BING -> ""
            }
        }
    }

    override fun toString(): String {
        return "Move: ${piece.getSymbol()} ${from.x},${from.y} -> ${to.x},${to.y} ${if (capturedPiece != null) "capturing ${capturedPiece.getSymbol()}" else ""}"
    }
}
