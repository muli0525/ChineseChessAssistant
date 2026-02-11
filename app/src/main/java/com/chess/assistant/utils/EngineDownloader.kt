package com.chess.assistant.utils

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

/**
 * 引擎下载器
 * 自动下载 Pikafish 象棋引擎
 */
class EngineDownloader(private val context: Context) {

    companion object {
        // Pikafish 引擎下载地址
        private const val ENGINE_URL = "https://github.com/official-pikafish/Pikafish/releases/latest/download/pikafish-android-arm64"
        
        // 备用下载地址
        private const val BACKUP_ENGINE_URL = "https://github.com/muli0525/ChineseChessAssistant/releases/download/v1.0/pikafish-android-arm64"
    }

    /**
     * 检查引擎是否存在
     */
    fun isEngineExists(): Boolean {
        val engineFile = getEngineFile()
        return engineFile.exists() && engineFile.canExecute()
    }

    /**
     * 获取引擎文件
     */
    fun getEngineFile(): File {
        val engineDir = File(context.filesDir, "engines")
        if (!engineDir.exists()) {
            engineDir.mkdirs()
        }
        return File(engineDir, "pikafish")
    }

    /**
     * 下载引擎
     */
    suspend fun downloadEngine(
        onProgress: (Int) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val engineFile = getEngineFile()
            
            // 如果引擎已存在，直接返回
            if (isEngineExists()) {
                return@withContext Result.success(engineFile)
            }

            // 下载引擎
            downloadFile(ENGINE_URL, engineFile, onProgress)
            
            // 设置执行权限
            engineFile.setExecutable(true)
            
            if (isEngineExists()) {
                Result.success(engineFile)
            } else {
                // 尝试备用地址
                downloadFile(BACKUP_ENGINE_URL, engineFile, onProgress)
                engineFile.setExecutable(true)
                if (isEngineExists()) {
                    Result.success(engineFile)
                } else {
                    Result.failure(Exception("Failed to download engine from all sources"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 下载文件
     */
    private fun downloadFile(
        url: String,
        destination: File,
        onProgress: (Int) -> Unit
    ) {
        val connection = URL(url).openConnection()
        connection.connect()
        
        val totalSize = connection.contentLength
        var downloadedSize = 0
        
        connection.getInputStream().use { input ->
            FileOutputStream(destination).use { output ->
                val buffer = ByteArray(4096)
                var bytesRead: Int
                
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    downloadedSize += bytesRead
                    
                    if (totalSize > 0) {
                        val progress = (downloadedSize * 100 / totalSize)
                        onProgress(progress)
                    }
                }
            }
        }
    }

    /**
     * 获取引擎路径
     */
    fun getEnginePath(): String {
        return getEngineFile().absolutePath
    }

    /**
     * 检查是否有新版本
     */
    suspend fun checkForUpdates(): Boolean = withContext(Dispatchers.IO) {
        // 简化版：始终返回 false
        // 实际可以检查 GitHub releases
        false
    }

    /**
     * 删除引擎文件
     */
    fun deleteEngine(): Boolean {
        return try {
            getEngineFile().delete()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取引擎版本信息
     */
    suspend fun getEngineVersion(): String = withContext(Dispatchers.IO) {
        if (isEngineExists()) {
            try {
                val process = Runtime.getRuntime().exec(arrayOf(getEnginePath(), "version"))
                val reader = process.inputStream.bufferedReader()
                reader.readText().trim()
            } catch (e: Exception) {
                "Unknown"
            }
        } else {
            "Not installed"
        }
    }
}
