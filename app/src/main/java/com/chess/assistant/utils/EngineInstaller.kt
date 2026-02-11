package com.chess.assistant.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * 引擎安装器
 * 从 APK assets 中提取 Pikafish 引擎
 */
class EngineInstaller(private val context: Context) {

    companion object {
        private const val TAG = "EngineInstaller"
        private const val ENGINE_ASSET_PATH = "engines/pikafish"
        private const val ENGINE_FILE_NAME = "pikafish"
    }

    /**
     * 检查引擎是否已安装
     */
    fun isEngineInstalled(): Boolean {
        return try {
            val engineFile = getEngineFile()
            engineFile.exists() && engineFile.canExecute() && engineFile.length() > 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取引擎文件
     */
    fun getEngineFile(): File {
        val engineDir = File(context.filesDir, "engines")
        if (!engineDir.exists()) {
            engineDir.mkdirs()
        }
        return File(engineDir, ENGINE_FILE_NAME)
    }

    /**
     * 安装引擎（从 assets 提取）
     */
    suspend fun installEngine(
        onProgress: (Int) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val engineFile = getEngineFile()
            
            // 如果引擎已安装且有效，直接返回
            if (isEngineInstalled()) {
                Log.d(TAG, "引擎已安装: ${engineFile.absolutePath}")
                return@withContext Result.success(engineFile)
            }

            // 从 assets 提取引擎
            Log.d(TAG, "正在从 assets 提取引擎...")
            
            context.assets.open(ENGINE_ASSET_PATH).use { input ->
                FileOutputStream(engineFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytesRead = 0L
                    
                    // 获取引擎文件大小
                    val assetSize = context.assets.openFd(ENGINE_ASSET_PATH).use { it.length() }
                    
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        
                        // 更新进度
                        if (assetSize > 0) {
                            val progress = ((totalBytesRead * 100) / assetSize).toInt()
                            onProgress(progress)
                        }
                    }
                }
            }

            // 设置执行权限
            engineFile.setExecutable(true)
            
            Log.d(TAG, "引擎安装完成: ${engineFile.absolutePath}")
            Log.d(TAG, "引擎文件大小: ${engineFile.length()} bytes")
            
            if (isEngineInstalled()) {
                Result.success(engineFile)
            } else {
                Result.failure(Exception("引擎安装失败：文件无效"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "引擎安装失败", e)
            Result.failure(e)
        }
    }

    /**
     * 获取引擎路径
     */
    fun getEnginePath(): String {
        return getEngineFile().absolutePath
    }

    /**
     * 删除引擎文件
     */
    fun uninstallEngine(): Boolean {
        return try {
            getEngineFile().delete()
        } catch (e: Exception) {
            Log.e(TAG, "删除引擎失败", e)
            false
        }
    }

    /**
     * 获取引擎版本信息
     */
    suspend fun getEngineVersion(): String = withContext(Dispatchers.IO) {
        if (isEngineInstalled()) {
            try {
                val process = Runtime.getRuntime().exec(arrayOf(getEnginePath(), "version"))
                val reader = process.inputStream.bufferedReader()
                val version = reader.readText().trim()
                reader.close()
                process.destroy()
                version
            } catch (e: Exception) {
                Log.e(TAG, "获取引擎版本失败", e)
                "Unknown"
            }
        } else {
            "Not installed"
        }
    }

    /**
     * 检查引擎是否需要更新（检查 assets 中的版本）
     */
    suspend fun checkForUpdates(): Boolean = withContext(Dispatchers.IO) {
        // 简化版：始终返回 false
        // 实际可以检查 assets 中的版本号与远程版本对比
        false
    }
}
