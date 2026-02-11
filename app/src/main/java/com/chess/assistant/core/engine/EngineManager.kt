package com.chess.assistant.core.engine

import com.chess.assistant.core.chess.ChessBoard
import com.chess.assistant.core.chess.Move
import com.chess.assistant.core.chess.PieceColor
import com.chess.assistant.core.chess.Position
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 象棋引擎管理器
 * 负责管理 UCI 引擎实例，提供高-level API
 */
class EngineManager {

    private val engine = UciEngine()
    private var engineJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    // 引擎状态
    private val _engineState = MutableStateFlow<EngineState>(EngineState.Idle)
    val engineState: StateFlow<EngineState> = _engineState.asStateFlow()

    // 当前最佳着法
    private val _currentBestMove = MutableStateFlow<MoveSuggestion?>(null)
    val currentBestMove: StateFlow<MoveSuggestion?> = _currentBestMove.asStateFlow()

    // 引擎信息
    private val _engineInfo = MutableStateFlow<EngineInfo?>(null)
    val engineInfo: StateFlow<EngineInfo?> = _engineInfo.asStateFlow()

    /**
     * 启动引擎
     */
    suspend fun start(enginePath: String): Boolean {
        _engineState.value = EngineState.Starting

        val success = engine.startEngine(enginePath)
        if (success) {
            _engineState.value = EngineState.Ready
            val info = engine.uci()
            _engineInfo.value = info
            return true
        } else {
            _engineState.value = EngineState.Error("Failed to start engine")
            return false
        }
    }

    /**
     * 停止引擎
     */
    suspend fun stop() {
        engineJob?.cancel()
        engine.quit()
        _engineState.value = EngineState.Idle
        _currentBestMove.value = null
    }

    /**
     * 分析局面并获取最佳着法
     */
    fun analyzePosition(
        chessBoard: ChessBoard,
        playerColor: PieceColor,
        depth: Int = 15
    ) {
        // 取消之前的分析
        engineJob?.cancel()

        engineJob = scope.launch {
            _engineState.value = EngineState.Analyzing

            try {
                // 转换棋盘为 FEN
                val fen = convertBoardToFen(chessBoard)
                engine.setPosition(fen)

                // 开始思考
                val move = engine.go(depth)

                // 解析着法
                if (move.move.isNotEmpty()) {
                    val parsedMove = parseUciMove(move.move, chessBoard, playerColor)
                    _currentBestMove.value = MoveSuggestion(
                        move = parsedMove,
                        depth = move.depth,
                        nodes = move.nodes,
                        timeMs = move.timeMs,
                        evaluation = null
                    )
                }

                _engineState.value = EngineState.Ready
            } catch (e: Exception) {
                _engineState.value = EngineState.Error(e.message ?: "Analysis failed")
            }
        }
    }

    /**
     * 获取多个最佳着法
     */
    fun getTopMoves(
        chessBoard: ChessBoard,
        playerColor: PieceColor,
        count: Int = 3,
        depth: Int = 10
    ): List<MoveSuggestion> {
        // 这里可以扩展为获取多个着法
        // 暂时只返回最佳着法
        val bestMove = _currentBestMove.value
        return if (bestMove != null) {
            listOf(bestMove)
        } else {
            emptyList()
        }
    }

    /**
     * 设置引擎选项
     */
    suspend fun setOption(name: String, value: String) {
        engine.setOption(name, value)
    }

    /**
     * 检查引擎是否就绪
     */
    fun isReady(): Boolean {
        return _engineState.value == EngineState.Ready
    }

    /**
     * 转换棋盘为 UCI FEN 格式
     */
    private fun convertBoardToFen(chessBoard: ChessBoard): String {
        val pieces = mutableMapOf<Pair<Int, Int>, Pair<String, String>>()

        chessBoard.pieces.forEach { piece ->
            val pieceType = when (piece.type.name) {
                "JU" -> "R"
                "MA" -> "N"
                "XIANG" -> "B"
                "SHI" -> "A"
                "JIANG" -> "K"
                "PAO" -> "C"
                "BING" -> "P"
                else -> "P"
            }

            val color = if (piece.color == PieceColor.RED) "RED" else "BLACK"
            pieces[piece.position.x to piece.position.y] = pieceType to color
        }

        val sideToMove = if (chessBoard.currentPlayer == PieceColor.RED) "w" else "b"

        return UciEngine.chessBoardToFen(pieces, sideToMove)
    }

    /**
     * 解析 UCI 着法
     */
    private fun parseUciMove(uciMove: String, chessBoard: ChessBoard, playerColor: PieceColor): Move? {
        if (uciMove.length < 4) return null

        try {
            val fromX = uciMove[0] - 'a'
            val fromY = 9 - (uciMove[1] - '1')
            val toX = uciMove[2] - 'a'
            val toY = 9 - (uciMove[3] - '1')

            val from = Position(fromX, fromY)
            val to = Position(toX, toY)

            val piece = chessBoard.getPieceAt(from) ?: return null

            return Move(
                from = from,
                to = to,
                piece = piece,
                capturedPiece = chessBoard.getPieceAt(to)
            )
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * 关闭引擎
     */
    suspend fun shutdown() {
        engineJob?.cancel()
        engine.quit()
        _engineState.value = EngineState.Idle
    }
}

/**
 * 引擎状态
 */
sealed class EngineState {
    object Idle : EngineState()
    object Starting : EngineState()
    object Ready : EngineState()
    object Analyzing : EngineState()
    data class Error(val message: String) : EngineState()
}

/**
 * 着法建议
 */
data class MoveSuggestion(
    val move: Move?,
    val depth: Int,
    val nodes: Long,
    val timeMs: Long,
    val evaluation: Double? // 正数表示红方优势，负数表示黑方优势
)
