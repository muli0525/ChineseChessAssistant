package com.chess.assistant.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.Paint
import android.graphics.Typeface
import com.chess.assistant.core.chess.ChessBoard
import com.chess.assistant.core.chess.ChessPiece
import com.chess.assistant.core.chess.Move
import com.chess.assistant.core.chess.PieceColor
import com.chess.assistant.core.chess.Position

/**
 * 象棋棋盘视图
 */
@Composable
fun ChessBoardView(
    chessBoard: ChessBoard,
    suggestedMove: Move? = null,
    playerColor: PieceColor = PieceColor.RED,
    onPieceClick: (ChessPiece) -> Unit = {},
    onMoveComplete: (Move) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedPosition by remember { mutableStateOf<Position?>(null) }
    var currentDragPosition by remember { mutableStateOf<Position?>(null) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 棋盘
        ChessBoardCanvas(
            chessBoard = chessBoard,
            suggestedMove = suggestedMove,
            selectedPosition = selectedPosition,
            onSquareClick = { position ->
                val piece = chessBoard.getPieceAt(position)
                if (piece != null && piece.color == playerColor) {
                    selectedPosition = position
                    onPieceClick(piece)
                } else if (selectedPosition != null) {
                    // 执行走棋
                    val movePiece = chessBoard.getPieceAt(selectedPosition!!)
                    if (movePiece != null) {
                        val move = Move(
                            from = selectedPosition!!,
                            to = position,
                            piece = movePiece,
                            capturedPiece = chessBoard.getPieceAt(position)
                        )
                        if (chessBoard.makeMove(move)) {
                            onMoveComplete(move)
                        }
                    }
                    selectedPosition = null
                }
            },
            onDragStart = { position ->
                val piece = chessBoard.getPieceAt(position)
                if (piece != null && piece.color == playerColor) {
                    selectedPosition = position
                }
            },
            onDragEnd = { start, end ->
                if (start != null && end != null) {
                    val movePiece = chessBoard.getPieceAt(start)
                    if (movePiece != null) {
                        val move = Move(
                            from = start,
                            to = end,
                            piece = movePiece,
                            capturedPiece = chessBoard.getPieceAt(end)
                        )
                        if (chessBoard.makeMove(move)) {
                            onMoveComplete(move)
                        }
                    }
                }
                selectedPosition = null
                currentDragPosition = null
            },
            onDrag = { _, end ->
                currentDragPosition = end
            }
        )

        // 着法提示
        suggestedMove?.let { move ->
            MoveSuggestionCard(move = move)
        }
    }
}

/**
 * 象棋棋盘画布
 */
@Composable
fun ChessBoardCanvas(
    chessBoard: ChessBoard,
    suggestedMove: Move?,
    selectedPosition: Position?,
    onSquareClick: (Position) -> Unit,
    onDragStart: (Position) -> Unit,
    onDragEnd: (Position?, Position?) -> Unit,
    onDrag: (Offset, Position) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val boardSize = with(density) { 360.dp.toPx() }
    val lineSpacing = boardSize / 9
    val padding = 20f // Define padding here

    BoxWithConstraints(
        modifier = modifier
            .size(with(density) { 400.dp })
            .background(Color(0xFFDEB887)) // 木质颜色
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding.dp) // Use dp suffix
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val x = ((offset.x - padding) / lineSpacing).toInt().coerceIn(0, 8)
                        val y = ((offset.y - padding) / lineSpacing).toInt().coerceIn(0, 9)
                        onSquareClick(Position(x, y))
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val x = ((offset.x - padding) / lineSpacing).toInt().coerceIn(0, 8)
                            val y = ((offset.y - padding) / lineSpacing).toInt().coerceIn(0, 9)
                            onDragStart(Position(x, y))
                        },
                        onDragEnd = {
                            onDragEnd(null, null)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val x = ((change.position.x - padding) / lineSpacing).toInt().coerceIn(0, 8)
                            val y = ((change.position.y - padding) / lineSpacing).toInt().coerceIn(0, 9)
                            onDrag(change.position, Position(x, y))
                        }
                    )
                }
        ) {
            drawBoard(padding, lineSpacing)
            drawPieces(chessBoard, lineSpacing, padding, selectedPosition, suggestedMove)
            drawSuggestionArrow(suggestedMove, lineSpacing, padding)
        }
    }
}

/**
 * 绘制棋盘
 */
private fun DrawScope.drawBoard(padding: Float, lineSpacing: Float) {
    val paint = Paint().apply {
        color = android.graphics.Color.BLACK
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    // 绘制9条竖线
    for (x in 0..8) {
        val startX = padding + x * lineSpacing
        drawLine(
            color = Color.Black,
            start = Offset(startX, padding),
            end = Offset(startX, padding + 9 * lineSpacing),
            strokeWidth = 2f
        )
    }

    // 绘制10条横线
    for (y in 0..9) {
        val startY = padding + y * lineSpacing
        drawLine(
            color = Color.Black,
            start = Offset(padding, startY),
            end = Offset(padding + 8 * lineSpacing, startY),
            strokeWidth = 2f
        )
    }

    // 绘制九宫格斜线
    // 上方
    drawLine(
        color = Color.Black,
        start = Offset(padding + 3 * lineSpacing, padding),
        end = Offset(padding + 5 * lineSpacing, padding + 2 * lineSpacing),
        strokeWidth = 2f
    )
    drawLine(
        color = Color.Black,
        start = Offset(padding + 5 * lineSpacing, padding),
        end = Offset(padding + 3 * lineSpacing, padding + 2 * lineSpacing),
        strokeWidth = 2f
    )

    // 下方
    drawLine(
        color = Color.Black,
        start = Offset(padding + 3 * lineSpacing, padding + 7 * lineSpacing),
        end = Offset(padding + 5 * lineSpacing, padding + 9 * lineSpacing),
        strokeWidth = 2f
    )
    drawLine(
        color = Color.Black,
        start = Offset(padding + 5 * lineSpacing, padding + 7 * lineSpacing),
        end = Offset(padding + 3 * lineSpacing, padding + 9 * lineSpacing),
        strokeWidth = 2f
    )

    // 楚河汉界文字
    with(drawContext.canvas.nativeCanvas) {
        val textPaint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 24f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        drawText("楚", (padding + 1 * lineSpacing).toFloat(), (padding + 4.5f * lineSpacing), textPaint)
        drawText("河", (padding + 3 * lineSpacing).toFloat(), (padding + 4.5f * lineSpacing), textPaint)
        drawText("漢", (padding + 5 * lineSpacing).toFloat(), (padding + 4.5f * lineSpacing), textPaint)
        drawText("界", (padding + 7 * lineSpacing).toFloat(), (padding + 4.5f * lineSpacing), textPaint)
    }
}

/**
 * 绘制棋子
 */
private fun DrawScope.drawPieces(
    chessBoard: ChessBoard,
    lineSpacing: Float,
    padding: Float,
    selectedPosition: Position?,
    suggestedMove: Move?
) {
    chessBoard.pieces.forEach { piece ->
        val centerX = padding + piece.position.x * lineSpacing
        val centerY = padding + piece.position.y * lineSpacing
        val radius = lineSpacing * 0.45f

        // 绘制棋子背景
        val pieceColor = if (piece.color == PieceColor.RED) {
            Color(0xFFFFCC00)
        } else {
            Color(0xFF8B4513)
        }

        drawCircle(
            color = pieceColor,
            radius = radius,
            center = Offset(centerX, centerY)
        )

        // 绘制棋子边框
        drawCircle(
            color = Color.Black,
            radius = radius,
            center = Offset(centerX, centerY),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
        )

        // 绘制棋子文字
        with(drawContext.canvas.nativeCanvas) {
            val textPaint = Paint().apply {
                color = if (piece.color == PieceColor.RED) {
                    android.graphics.Color.RED
                } else {
                    android.graphics.Color.BLACK
                }
                textSize = 28f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD
            }
            drawText(
                piece.getSymbol(),
                centerX,
                centerY + 10f,
                textPaint
            )
        }

        // 绘制选中标记
        if (selectedPosition == piece.position) {
            drawCircle(
                color = Color(0xFF00FF00),
                radius = radius + 5f,
                center = Offset(centerX, centerY),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
            )
        }

        // 绘制建议标记
        suggestedMove?.let { move ->
            if (move.from == piece.position || move.to == piece.position) {
                drawCircle(
                    color = Color(0xFFFFFF00),
                    radius = radius + 5f,
                    center = Offset(centerX, centerY),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                )
            }
        }
    }
}

/**
 * 绘制建议箭头
 */
private fun DrawScope.drawSuggestionArrow(
    suggestedMove: Move?,
    lineSpacing: Float,
    padding: Float
) {
    suggestedMove?.let { move ->
        val startX = padding + move.from.x * lineSpacing
        val startY = padding + move.from.y * lineSpacing
        val endX = padding + move.to.x * lineSpacing
        val endY = padding + move.to.y * lineSpacing

        // 绘制箭头
        val arrowColor = Color(0xFF00FF00)
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }

        drawPath(
            path = path,
            color = arrowColor,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 4f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        )

        // 绘制箭头头部
        val arrowHeadSize = 15f
        val angle = kotlin.math.atan2(
            (endY - startY).toDouble(),
            (endX - startX).toDouble()
        )
        val arrowHeadPath = Path().apply {
            moveTo(endX, endY)
            lineTo(
                (endX - arrowHeadSize * kotlin.math.cos(angle - 0.5)).toFloat(),
                (endY - arrowHeadSize * kotlin.math.sin(angle - 0.5)).toFloat()
            )
            moveTo(endX, endY)
            lineTo(
                (endX - arrowHeadSize * kotlin.math.cos(angle + 0.5)).toFloat(),
                (endY - arrowHeadSize * kotlin.math.sin(angle + 0.5)).toFloat()
            )
        }

        drawPath(
            path = arrowHeadPath,
            color = arrowColor,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 4f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        )
    }
}

/**
 * 着法建议卡片
 */
@Composable
fun MoveSuggestionCard(move: Move) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF3E0)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "建议着法",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF5D4037)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${move.piece.getSymbol()} (${move.from.x + 1},${10 - move.from.y}) → (${move.to.x + 1},${10 - move.to.y})",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF3E2723)
            )
            if (move.capturedPiece != null) {
                Text(
                    text = "吃子: ${move.capturedPiece.getSymbol()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFD84315)
                )
            }
        }
    }
}
