package com.chess.assistant.core.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.concurrent.TimeUnit

/**
 * UCI 引擎通信类
 * 实现 UCI (Universal Chess Interface) 协议，用于与象棋引擎通信
 * 参考 Pikafish: https://github.com/official-pikafish/Pikafish
 */
class UciEngine {

    private var process: Process? = null
    private var reader: BufferedReader? = null
    private var writer: BufferedWriter? = null
    private var isInitialized = false
    private var engineName = ""

    /**
     * 启动引擎
     */
    suspend fun startEngine(enginePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // 构建进程
            val processBuilder = ProcessBuilder(enginePath)
            processBuilder.redirectErrorStream(true)
            process = processBuilder.start()

            reader = BufferedReader(InputStreamReader(process!!.inputStream))
            writer = BufferedWriter(OutputStreamWriter(process!!.outputStream))

            // 等待引擎初始化
            val ready = readLine(5000)
            if (ready != null) {
                isInitialized = true
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 发送 UCI 命令
     */
    suspend fun sendCommand(command: String): Unit = withContext(Dispatchers.IO) {
        try {
            writer?.write(command)
            writer?.newLine()
            writer?.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 读取引擎响应（带超时）
     */
    private fun readLine(timeout: Long): String? {
        return try {
            reader?.readLine()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 读取引擎的所有响应
     */
    suspend fun readAllResponses(): List<String> = withContext(Dispatchers.IO) {
        val responses = mutableListOf<String>()
        try {
            while (true) {
                val line = reader?.readLine()
                if (line == null || line.isEmpty()) break
                responses.add(line)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        responses
    }

    /**
     * 初始化 UCI 模式
     */
    suspend fun uci(): EngineInfo = withContext(Dispatchers.IO) {
        sendCommand("uci")
        val responses = mutableListOf<String>()
        var foundName = false
        var foundOption = false

        while (true) {
            val line = readLine(1000) ?: break
            if (line.startsWith("id name")) {
                engineName = line.substring(8).trim()
                responses.add(line)
                foundName = true
            } else if (line.startsWith("id author")) {
                responses.add(line)
            } else if (line == "uciok") {
                responses.add(line)
                break
            } else if (line.startsWith("option")) {
                responses.add(line)
                foundOption = true
            }
        }

        EngineInfo(
            name = engineName,
            options = responses.filter { it.startsWith("option") }
        )
    }

    /**
     * 设置引擎选项
     */
    suspend fun setOption(name: String, value: String) {
        sendCommand("setoption name $name value $value")
    }

    /**
     * 设置局面（使用 FEN 格式）
     */
    suspend fun setPosition(fen: String, moves: List<String> = emptyList()) {
        val command = if (moves.isEmpty()) {
            "position fen $fen"
        } else {
            "position fen $fen moves ${moves.joinToString(" ")}"
        }
        sendCommand(command)
    }

    /**
     * 开始思考并获取最佳着法
     */
    suspend fun go(depth: Int = 15): EngineMove = withContext(Dispatchers.IO) {
        sendCommand("go depth $depth")

        var bestMove = ""
        var ponder = ""
        var depthReached = 0
        var nodes = 0L
        var time = 0L

        while (true) {
            val line = readLine(5000) ?: break

            when {
                line.startsWith("bestmove") -> {
                    val parts = line.split(" ")
                    if (parts.size >= 2) {
                        bestMove = parts[1]
                        if (parts.size >= 4) {
                            ponder = parts[3]
                        }
                    }
                    break
                }
                line.startsWith("info depth") -> {
                    // 解析深度
                    val depthMatch = Regex("depth (\\d+)").find(line)
                    depthMatch?.let {
                        depthReached = it.groupValues[1].toIntOrNull() ?: 0
                    }

                    // 解析节点数
                    val nodesMatch = Regex("nodes (\\d+)").find(line)
                    nodesMatch?.let {
                        nodes = it.groupValues[1].toLongOrNull() ?: 0L
                    }

                    // 解析时间
                    val timeMatch = Regex("time (\\d+)").find(line)
                    timeMatch?.let {
                        time = it.groupValues[1].toLongOrNull() ?: 0L
                    }
                }
            }
        }

        EngineMove(
            move = bestMove,
            ponder = ponder,
            depth = depthReached,
            nodes = nodes,
            timeMs = time
        )
    }

    /**
     * 停止引擎思考
     */
    suspend fun stop() {
        sendCommand("stop")
    }

    /**
     * 检查引擎是否正在运行
     */
    fun isRunning(): Boolean {
        return process?.isAlive ?: false
    }

    /**
     * 关闭引擎
     */
    suspend fun quit() = withContext(Dispatchers.IO) {
        sendCommand("quit")
        process?.waitFor(5, TimeUnit.SECONDS)
        process?.destroy()
        isInitialized = false
    }

    /**
     * 转换中国象棋局面为 UCI FEN 格式
     * Pikafish 支持中国象棋，使用标准的中国象棋 FEN
     */
    companion object {
        /**
         * 将棋盘转换为 FEN 字符串
         * 中国象棋 FEN 格式参考 Pikafish 文档
         */
        fun chessBoardToFen(
            pieces: Map<Pair<Int, Int>, Pair<String, String>>,
            sideToMove: String = "w"
        ): String {
            // 构建棋盘部分
            val boardRows = mutableListOf<String>()
            for (row in 9 downTo 0) {
                val rowStr = buildString {
                    var emptyCount = 0
                    for (col in 0..8) {
                        val piece = pieces[col to row]
                        if (piece != null) {
                            if (emptyCount > 0) {
                                append(emptyCount)
                                emptyCount = 0
                            }
                            // 转换棋子表示
                            val pieceChar = when (piece.first) {
                                "JU" -> "R"
                                "MA" -> "N"
                                "XIANG" -> "B"
                                "SHI" -> "A"
                                "JIANG" -> "K"
                                "PAO" -> "C"
                                "BING" -> "P"
                                else -> "P"
                            }
                            // 红方用大写，黑方用小写
                            val finalChar = if (piece.second == "RED") pieceChar else pieceChar.lowercase()
                            append(finalChar)
                        } else {
                            emptyCount++
                        }
                    }
                    if (emptyCount > 0) {
                        append(emptyCount)
                    }
                }
                boardRows.add(rowStr)
            }

            // FEN 格式: board side castling passant
            return "${boardRows.joinToString("/")} $sideToMove - - 0 1"
        }
    }
}

/**
 * 引擎信息
 */
data class EngineInfo(
    val name: String,
    val options: List<String>
)

/**
 * 引擎返回的着法信息
 */
data class EngineMove(
    val move: String,
    val ponder: String = "",
    val depth: Int = 0,
    val nodes: Long = 0,
    val timeMs: Long = 0
)
