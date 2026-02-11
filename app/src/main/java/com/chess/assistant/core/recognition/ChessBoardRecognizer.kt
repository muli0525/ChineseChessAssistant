package com.chess.assistant.core.recognition

import android.graphics.Bitmap
import android.graphics.Color
import com.chess.assistant.core.chess.ChessBoard
import com.chess.assistant.core.chess.ChessPiece
import com.chess.assistant.core.chess.PieceColor
import com.chess.assistant.core.chess.PieceType
import com.chess.assistant.core.chess.Position
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileOutputStream

/**
 * 象棋棋盘识别器
 * 使用 OpenCV 进行图像处理和棋盘识别
 */
class ChessBoardRecognizer(private val dataDirPath: String) {

    private var isInitialized = false

    /**
     * 初始化 OpenCV
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            // 复制 OpenCV 分类器文件
            prepareOpenCVExtracts()

            // 初始化 OpenCV
            isInitialized = OpenCVLoader.initLocal()
            isInitialized
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 准备 OpenCV 资源文件
     */
    private fun prepareOpenCVExtracts() {
        try {
            val dataDir = File(dataDirPath)
            if (!dataDir.exists()) {
                dataDir.mkdirs()
            }

            // 复制 Haar 分类器文件
            val cascadeFile = File(dataDirPath, "cascade.xml")
            if (!cascadeFile.exists()) {
                // 复制内置的级联分类器
                copyAssetFile("cascade.xml", cascadeFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 从 assets 复制文件
     */
    private fun copyAssetFile(assetName: String, outputFile: File) {
        try {
            val inputStream = Thread.currentThread().contextClassLoader?.getResourceAsStream(assetName)
            if (inputStream != null) {
                FileOutputStream(outputFile).use { output ->
                    inputStream.copyTo(output)
                }
                inputStream.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 识别棋盘
     */
    suspend fun recognizeBoard(bitmap: Bitmap): RecognitionResult = withContext(Dispatchers.Default) {
        if (!isInitialized) {
            return@withContext RecognitionResult(
                success = false,
                error = "OpenCV not initialized"
            )
        }

        try {
            // 转换 Bitmap 到 Mat
            val mat = Mat()
            Utils.bitmapToMat(bitmap, mat)

            // 预处理图像
            val processedMat = preprocessImage(mat)

            // 检测棋盘格子
            val gridPositions = detectGridPositions(processedMat)

            // 检测棋子
            val pieces = detectPieces(processedMat, gridPositions)

            // 创建棋盘
            val chessBoard = ChessBoard()
            for (piece in pieces) {
                // Only add pieces that have a recognized type
                if (piece.type != null) {
                    chessBoard.addPiece(piece.type, piece.color, piece.position)
                }
            }

            // 清理资源
            processedMat.release()
            mat.release()

            RecognitionResult(
                success = true,
                chessBoard = chessBoard,
                pieces = pieces,
                confidence = 0.85
            )
        } catch (e: Exception) {
            e.printStackTrace()
            RecognitionResult(
                success = false,
                error = e.message ?: "Recognition failed"
            )
        }
    }

    /**
     * 预处理图像
     */
    private fun preprocessImage(mat: Mat): Mat {
        val result = Mat()

        // 转换为灰度图
        Imgproc.cvtColor(mat, result, Imgproc.COLOR_BGR2GRAY)

        // 高斯模糊去噪
        Imgproc.GaussianBlur(result, result, Size(5.0, 5.0), 0.0)

        // 自适应阈值
        Imgproc.adaptiveThreshold(
            result,
            result,
            255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY_INV,
            11,
            2.0
        )

        return result
    }

    /**
     * 检测棋盘网格位置
     */
    private fun detectGridPositions(mat: Mat): List<Position> {
        val positions = mutableListOf<Position>()

        // 计算网格参数
        val width = mat.width()
        val height = mat.height()

        // 棋盘应该是 9x10 的格子
        val cellWidth = width / 9.0
        val cellHeight = height / 10.0

        // 检测所有可能的交叉点
        for (y in 0 until 10) {
            for (x in 0 until 9) {
                val posX = (x * cellWidth + cellWidth / 2).toInt()
                val posY = (y * cellHeight + cellHeight / 2).toInt()

                if (posX in 0 until width && posY in 0 until height) {
                    positions.add(Position(x, y))
                }
            }
        }

        return positions
    }

    /**
     * 检测棋子
     */
    private fun detectPieces(mat: Mat, gridPositions: List<Position>): List<DetectedPiece> {
        val pieces = mutableListOf<DetectedPiece>()

        // 使用霍夫圆检测圆形区域（棋子）
        // HoughCircles 返回 Mat，每行包含 x, y, radius
        val circles = Mat()
        Imgproc.HoughCircles(
            mat,
            circles,
            Imgproc.HOUGH_GRADIENT,
            1.0,
            mat.height() / 10.0,
            100.0,
            30.0,
            mat.height() / 25,
            mat.height() / 12
        )

        // 将检测到的圆映射到最近的网格位置
        for (i in 0 until circles.cols()) {
            val circleData = FloatArray(3)
            circles.get(0, i, circleData)

            val centerX = circleData[0].toInt()
            val centerY = circleData[1].toInt()
            val radius = circleData[2].toInt()

            // 找到最近的网格位置
            val gridPos = findNearestGridPosition(centerX, centerY, gridPositions)

            if (gridPos != null) {
                // 识别棋子颜色
                val pieceColor = recognizePieceColor(mat, centerX, centerY, radius)

                // 识别棋子类型
                val pieceType = recognizePieceType(mat, centerX, centerY, radius)

                if (pieceType != null) {
                    pieces.add(
                        DetectedPiece(
                            position = gridPos,
                            type = pieceType,
                            color = pieceColor,
                            confidence = 0.8
                        )
                    )
                }
            }
        }

        circles.release()

        return pieces
    }

    /**
     * 找到最近的网格位置
     */
    private fun findNearestGridPosition(
        x: Int,
        y: Int,
        gridPositions: List<Position>
    ): Position? {
        val width = 9
        val height = 10

        // 估算网格大小
        // 这里简化处理，实际应该根据图像计算
        return try {
            val gridX = ((x.toFloat() / width) * 8).toInt().coerceIn(0, 8)
            val gridY = ((y.toFloat() / height) * 9).toInt().coerceIn(0, 9)

            Position(gridX, gridY)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 识别棋子颜色
     */
    private fun recognizePieceColor(mat: Mat, centerX: Int, centerY: Int, radius: Int): PieceColor {
        // 分析棋子区域的颜色
        val redCount = 0
        val blackCount = 0

        // 采样棋子区域的像素
        val sampleSize = radius / 2
        for (dx in -sampleSize..sampleSize) {
            for (dy in -sampleSize..sampleSize) {
                if (dx * dx + dy * dy <= sampleSize * sampleSize) {
                    val pixelX = (centerX + dx).coerceIn(0, mat.width() - 1)
                    val pixelY = (centerY + dy).coerceIn(0, mat.height() - 1)

                    val pixel = mat.get(pixelY, pixelX)
                    if (pixel != null && pixel.size >= 3) {
                        // 简化的颜色判断
                        val intensity = pixel[0] + pixel[1] + pixel[2]
                        if (intensity > 300) {
                            // 亮色（红色棋子）
                            return PieceColor.RED
                        } else {
                            // 暗色（黑色棋子）
                            return PieceColor.BLACK
                        }
                    }
                }
            }
        }

        return PieceColor.RED // 默认返回红色
    }

    /**
     * 识别棋子类型
     */
    private fun recognizePieceType(
        mat: Mat,
        centerX: Int,
        centerY: Int,
        radius: Int
    ): PieceType? {
        // 这里可以添加更复杂的棋子类型识别逻辑
        // 简化处理：根据位置判断（初始布局）
        // 实际应用中应该使用机器学习或更复杂的特征提取

        // 返回 null 表示无法识别具体类型
        // 在实际应用中，这里应该调用训练好的分类器
        return null
    }

    /**
     * 释放资源
     */
    fun release() {
        // 清理 OpenCV 资源
    }
}

/**
 * 识别结果
 */
data class RecognitionResult(
    val success: Boolean,
    val chessBoard: ChessBoard? = null,
    val pieces: List<DetectedPiece> = emptyList(),
    val confidence: Double = 0.0,
    val error: String? = null
)

/**
 * 检测到的棋子
 */
data class DetectedPiece(
    val position: Position,
    val type: PieceType?,
    val color: PieceColor,
    val confidence: Double
)

/**
 * 棋子类型（用于简化识别）
 */
enum class SimplifiedPieceType {
    JU, MA, XIANG, SHI, JIANG, PAO, BING, UNKNOWN
}
