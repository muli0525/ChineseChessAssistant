package com.chess.assistant.core.chess

/**
 * 象棋棋盘类
 * 管理棋盘状态、棋子、走棋历史和游戏规则
 */
class ChessBoard {

    private val _pieces = mutableListOf<ChessPiece>()
    private val moveHistory = mutableListOf<Move>()
    private var _currentPlayer: PieceColor = PieceColor.RED

    /**
     * 获取所有棋子
     */
    val pieces: List<ChessPiece>
        get() = _pieces.toList()

    /**
     * 获取当前玩家
     */
    val currentPlayer: PieceColor
        get() = _currentPlayer

    /**
     * 获取历史走棋记录
     */
    val history: List<Move>
        get() = moveHistory.toList()

    /**
     * 检查棋盘是否为空
     */
    val isEmpty: Boolean
        get() = _pieces.isEmpty()

    /**
     * 初始化默认棋盘布局
     */
    fun setupInitialPosition() {
        _pieces.clear()
        moveHistory.clear()
        _currentPlayer = PieceColor.RED

        // 黑方棋子 (上方)
        addPiece(PieceType.JU, PieceColor.BLACK, Position(0, 0))
        addPiece(PieceType.MA, PieceColor.BLACK, Position(1, 0))
        addPiece(PieceType.XIANG, PieceColor.BLACK, Position(2, 0))
        addPiece(PieceType.SHI, PieceColor.BLACK, Position(3, 0))
        addPiece(PieceType.JIANG, PieceColor.BLACK, Position(4, 0))
        addPiece(PieceType.SHI, PieceColor.BLACK, Position(5, 0))
        addPiece(PieceType.XIANG, PieceColor.BLACK, Position(6, 0))
        addPiece(PieceType.MA, PieceColor.BLACK, Position(7, 0))
        addPiece(PieceType.JU, PieceColor.BLACK, Position(8, 0))

        addPiece(PieceType.PAO, PieceColor.BLACK, Position(1, 2))
        addPiece(PieceType.PAO, PieceColor.BLACK, Position(7, 2))

        addPiece(PieceType.BING, PieceColor.BLACK, Position(0, 3))
        addPiece(PieceType.BING, PieceColor.BLACK, Position(2, 3))
        addPiece(PieceType.BING, PieceColor.BLACK, Position(4, 3))
        addPiece(PieceType.BING, PieceColor.BLACK, Position(6, 3))
        addPiece(PieceType.BING, PieceColor.BLACK, Position(8, 3))

        // 红方棋子 (下方)
        addPiece(PieceType.JU, PieceColor.RED, Position(0, 9))
        addPiece(PieceType.MA, PieceColor.RED, Position(1, 9))
        addPiece(PieceType.XIANG, PieceColor.RED, Position(2, 9))
        addPiece(PieceType.SHI, PieceColor.RED, Position(3, 9))
        addPiece(PieceType.JIANG, PieceColor.RED, Position(4, 9))
        addPiece(PieceType.SHI, PieceColor.RED, Position(5, 9))
        addPiece(PieceType.XIANG, PieceColor.RED, Position(6, 9))
        addPiece(PieceType.MA, PieceColor.RED, Position(7, 9))
        addPiece(PieceType.JU, PieceColor.RED, Position(8, 9))

        addPiece(PieceType.PAO, PieceColor.RED, Position(1, 7))
        addPiece(PieceType.PAO, PieceColor.RED, Position(7, 7))

        addPiece(PieceType.BING, PieceColor.RED, Position(0, 6))
        addPiece(PieceType.BING, PieceColor.RED, Position(2, 6))
        addPiece(PieceType.BING, PieceColor.RED, Position(4, 6))
        addPiece(PieceType.BING, PieceColor.RED, Position(6, 6))
        addPiece(PieceType.BING, PieceColor.RED, Position(8, 6))
    }

    /**
     * 添加棋子
     */
    fun addPiece(type: PieceType, color: PieceColor, position: Position) {
        _pieces.add(ChessPiece(type, color, position))
    }

    /**
     * 获取指定位置的棋子
     */
    fun getPieceAt(position: Position): ChessPiece? {
        return _pieces.find { it.position == position }
    }

    /**
     * 检查位置是否有棋子
     */
    fun hasPieceAt(position: Position): Boolean {
        return _pieces.any { it.position == position }
    }

    /**
     * 执行走棋
     */
    fun makeMove(move: Move): Boolean {
        val piece = getPieceAt(move.from) ?: return false

        // 验证是否是当前玩家的棋子
        if (piece.color != _currentPlayer) {
            return false
        }

        // 验证走法是否合法
        if (!isValidMove(move)) {
            return false
        }

        // 执行吃子（如果目标位置有敌方棋子）
        val captured = getPieceAt(move.to)
        if (captured != null) {
            _pieces.remove(captured)
        }

        // 更新棋子位置
        val updatedPiece = piece.withPosition(move.to)
        _pieces.remove(piece)
        _pieces.add(updatedPiece)

        // 记录走棋历史
        val actualMove = move.copy(capturedPiece = captured)
        moveHistory.add(actualMove)

        // 切换玩家
        _currentPlayer = if (_currentPlayer == PieceColor.RED) PieceColor.BLACK else PieceColor.RED

        return true
    }

    /**
     * 撤销上一步走棋
     */
    fun undoMove(): Boolean {
        if (moveHistory.isEmpty()) {
            return false
        }

        val lastMove = moveHistory.removeAt(moveHistory.size - 1)

        // 恢复被吃的棋子
        lastMove.capturedPiece?.let { captured ->
            _pieces.add(captured)
        }

        // 恢复移动的棋子
        val movedPiece = getPieceAt(lastMove.to)
        if (movedPiece != null) {
            _pieces.remove(movedPiece)
            _pieces.add(lastMove.piece)
        }

        // 切换玩家
        _currentPlayer = if (_currentPlayer == PieceColor.RED) PieceColor.BLACK else PieceColor.RED

        return true
    }

    /**
     * 验证走法是否合法
     */
    fun isValidMove(move: Move): Boolean {
        val piece = getPieceAt(move.from) ?: return false

        // 检查目标位置是否已有己方棋子
        val targetPiece = getPieceAt(move.to)
        if (targetPiece != null && targetPiece.color == piece.color) {
            return false
        }

        // 验证具体走法规则
        return when (piece.type) {
            PieceType.JU -> validateJuMove(move)
            PieceType.MA -> validateMaMove(move)
            PieceType.XIANG -> validateXiangMove(move)
            PieceType.SHI -> validateShiMove(move)
            PieceType.JIANG -> validateJiangMove(move)
            PieceType.PAO -> validatePaoMove(move)
            PieceType.BING -> validateBingMove(move)
        }
    }

    /**
     * 車的走法（直线）
     */
    private fun validateJuMove(move: Move): Boolean {
        val dx = move.to.x - move.from.x
        val dy = move.to.y - move.from.y

        // 車只能直线移动
        if (dx != 0 && dy != 0) {
            return false
        }

        // 检查路径上是否有障碍
        val stepX = if (dx > 0) 1 else if (dx < 0) -1 else 0
        val stepY = if (dy > 0) 1 else if (dy < 0) -1 else 0

        var currentX = move.from.x + stepX
        var currentY = move.from.y + stepY

        while (currentX != move.to.x || currentY != move.to.y) {
            if (hasPieceAt(Position(currentX, currentY))) {
                return false
            }
            currentX += stepX
            currentY += stepY
        }

        return true
    }

    /**
     * 馬的走法（日字腿）
     */
    private fun validateMaMove(move: Move): Boolean {
        val dx = kotlin.math.abs(move.to.x - move.from.x)
        val dy = kotlin.math.abs(move.to.y - move.from.y)

        // 馬走日字
        if (!((dx == 1 && dy == 2) || (dx == 2 && dy == 1))) {
            return false
        }

        // 检查蹩马腿
        val blockX = move.from.x + if (dx == 2) (if (move.to.x > move.from.x) -1 else 1) else 0
        val blockY = move.from.y + if (dy == 2) (if (move.to.y > move.from.y) -1 else 1) else 0

        if (dx == 2 && hasPieceAt(Position(blockX, move.from.y))) {
            return false
        }
        if (dy == 2 && hasPieceAt(Position(move.from.x, blockY))) {
            return false
        }

        return true
    }

    /**
     * 象的走法（田字，不能过河）
     */
    private fun validateXiangMove(move: Move): Boolean {
        val dx = kotlin.math.abs(move.to.x - move.from.x)
        val dy = kotlin.math.abs(move.to.y - move.from.y)
        val pieceColor = getPieceAt(move.from)?.color ?: return false

        // 象走田字
        if (dx != 2 || dy != 2) {
            return false
        }

        // 象不能过河
        if (move.to.isAcrossRiver(pieceColor)) {
            return false
        }

        // 检查象眼
        val eyeX = (move.from.x + move.to.x) / 2
        val eyeY = (move.from.y + move.to.y) / 2
        if (hasPieceAt(Position(eyeX, eyeY))) {
            return false
        }

        return true
    }

    /**
     * 士的走法（斜走一步，在九宫内）
     */
    private fun validateShiMove(move: Move): Boolean {
        val piece = getPieceAt(move.from) ?: return false
        val dx = kotlin.math.abs(move.to.x - move.from.x)
        val dy = kotlin.math.abs(move.to.y - move.from.y)

        // 士斜走一步
        if (dx != 1 || dy != 1) {
            return false
        }

        // 士只能在九宫内
        if (!move.to.isInPalace(piece.color)) {
            return false
        }

        return true
    }

    /**
     * 将/帥的走法（一步直线，在九宫内）
     */
    private fun validateJiangMove(move: Move): Boolean {
        val piece = getPieceAt(move.from) ?: return false
        val dx = kotlin.math.abs(move.to.x - move.from.x)
        val dy = kotlin.math.abs(move.to.y - move.from.y)

        // 将/帥只能走一步直线
        if (!((dx == 1 && dy == 0) || (dx == 0 && dy == 1))) {
            return false
        }

        // 将/帥只能在九宫内
        if (!move.to.isInPalace(piece.color)) {
            return false
        }

        // 将帅对面规则
        val opponentKing = _pieces.find {
            it.type == PieceType.JIANG && it.color != piece.color
        }
        if (opponentKing != null) {
            if (move.to.x == opponentKing.position.x) {
                // 检查中间是否有棋子阻挡
                val minY = minOf(move.to.y, opponentKing.position.y)
                val maxY = maxOf(move.to.y, opponentKing.position.y)
                for (y in (minY + 1) until maxY) {
                    if (hasPieceAt(Position(move.to.x, y))) {
                        return false
                    }
                }
            }
        }

        return true
    }

    /**
     * 炮的走法（移动同車，吃子需隔一子）
     */
    private fun validatePaoMove(move: Move): Boolean {
        val dx = move.to.x - move.from.x
        val dy = move.to.y - move.from.y

        // 炮只能直线移动
        if (dx != 0 && dy != 0) {
            return false
        }

        // 计算路径上的棋子数量
        val stepX = if (dx > 0) 1 else if (dx < 0) -1 else 0
        val stepY = if (dy > 0) 1 else if (dy < 0) -1 else 0

        var currentX = move.from.x + stepX
        var currentY = move.from.y + stepY
        var piecesBetween = 0

        while (currentX != move.to.x || currentY != move.to.y) {
            if (hasPieceAt(Position(currentX, currentY))) {
                piecesBetween++
            }
            currentX += stepX
            currentY += stepY
        }

        val targetPiece = getPieceAt(move.to)

        // 不吃子时路径必须为空
        if (targetPiece == null || targetPiece.color == getPieceAt(move.from)?.color ?: return false) {
            return piecesBetween == 0
        }

        // 吃子时必须只有一个棋子隔开
        return piecesBetween == 1
    }

    /**
     * 兵的走法（过河前只能向前，过河后可横走）
     */
    private fun validateBingMove(move: Move): Boolean {
        val piece = getPieceAt(move.from) ?: return false
        val dx = move.to.x - move.from.x
        val dy = move.to.y - move.from.y

        // 兵只能走一步
        if (kotlin.math.abs(dx) + kotlin.math.abs(dy) != 1) {
            return false
        }

        // 红兵只能向上（y减小）
        if (piece.color == PieceColor.RED && dy > 0) {
            return false
        }

        // 黑兵只能向下（y增大）
        if (piece.color == PieceColor.BLACK && dy < 0) {
            return false
        }

        // 未过河不能横走
        if (!piece.position.isAcrossRiver(piece.color) && dx != 0) {
            return false
        }

        return true
    }

    /**
     * 检查将军
     */
    fun isInCheck(color: PieceColor): Boolean {
        val king = _pieces.find { it.type == PieceType.JIANG && it.color == color }
            ?: return false

        val opponentColor = if (color == PieceColor.RED) PieceColor.BLACK else PieceColor.RED

        return _pieces
            .filter { it.color == opponentColor }
            .any { piece ->
                val move = Move(king.position, king.position, piece)
                val hypotheticalMove = move.copy(to = king.position)
                isValidMove(hypotheticalMove)
            }
    }

    /**
     * 检查是否有合法走法
     */
    fun hasValidMoves(color: PieceColor): Boolean {
        val playerPieces = _pieces.filter { it.color == color }

        return playerPieces.any { piece ->
            // Generate all possible target positions on the board
            val allPositions = mutableListOf<Position>()
            for (x in 0..8) {
                for (y in 0..9) {
                    allPositions.add(Position(x, y))
                }
            }

            allPositions.any { target ->
                val move = Move(piece.position, target, piece)
                isValidMove(move)
            }
        }
    }

    /**
     * 更新游戏状态
     */
    fun updateGameState(): GameState {
        val currentColor = _currentPlayer

        return when {
            isCheckmate(currentColor) -> GameState.CHECKMATE
            isInCheck(currentColor) -> GameState.CHECK
            !hasValidMoves(currentColor) -> GameState.STALEMATE
            else -> GameState.PLAYING
        }
    }

    /**
     * 检查将死
     */
    private fun isCheckmate(color: PieceColor): Boolean {
        if (!isInCheck(color)) {
            return false
        }
        return !hasValidMoves(color)
    }

    /**
     * 获取所有合法走法
     */
    fun getValidMoves(piece: ChessPiece): List<Move> {
        val allPositions = mutableListOf<Position>()
        for (x in 0..8) {
            for (y in 0..9) {
                allPositions.add(Position(x, y))
            }
        }

        return allPositions.map { target ->
            Move(piece.position, target, piece)
        }.filter { isValidMove(it) }
    }

    /**
     * 重置棋盘
     */
    fun reset() {
        _pieces.clear()
        moveHistory.clear()
        _currentPlayer = PieceColor.RED
    }

    /**
     * 设置指定位置
     */
    fun setPosition(pieces: Map<Position, Pair<PieceType, PieceColor>>) {
        _pieces.clear()
        moveHistory.clear()
        _currentPlayer = PieceColor.RED

        pieces.forEach { (position, pieceData) ->
            addPiece(pieceData.first, pieceData.second, position)
        }
    }

    /**
     * 清除棋盘
     */
    fun clearBoard() {
        _pieces.clear()
        moveHistory.clear()
        _currentPlayer = PieceColor.RED
    }
}

/**
 * 游戏状态
 */
enum class GameState {
    PLAYING,
    CHECK,
    CHECKMATE,
    STALEMATE
}
