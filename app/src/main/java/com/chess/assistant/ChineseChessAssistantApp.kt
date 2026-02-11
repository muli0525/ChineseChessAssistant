package com.chess.assistant

import android.app.Application
import android.util.Log
import org.opencv.android.OpenCVLoader

/**
 * 象棋助手 Application 类
 */
class ChineseChessAssistantApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        initializeOpenCV()
    }

    private fun initializeOpenCV() {
        try {
            val success = OpenCVLoader.initLocal()
            if (success) {
                Log.d(TAG, "OpenCV loaded successfully")
            } else {
                Log.e(TAG, "OpenCV initialization failed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "OpenCV initialization error: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "ChineseChessAssistant"

        @Volatile
        private var instance: ChineseChessAssistantApp? = null

        fun getInstance(): ChineseChessAssistantApp {
            return instance ?: throw IllegalStateException("Application not initialized")
        }
    }
}
