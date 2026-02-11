package com.chess.assistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.chess.assistant.core.capture.ScreenCaptureService
import com.chess.assistant.core.chess.ChessBoard
import com.chess.assistant.core.chess.PieceColor
import com.chess.assistant.core.engine.EngineManager
import com.chess.assistant.core.overlay.OverlayService
import com.chess.assistant.core.recognition.ChessBoardRecognizer
import com.chess.assistant.ui.theme.ChineseChessAssistantTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 象棋助手主界面
 */
class MainActivity : ComponentActivity() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // 核心组件
    private lateinit var engineManager: EngineManager
    private lateinit var recognizer: ChessBoardRecognizer
    private lateinit var captureService: ScreenCaptureService

    // 权限请求
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        checkOverlayPermission()
    }

    private val capturePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            startScreenCapture(result.data)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化核心组件
        engineManager = EngineManager()
        recognizer = ChessBoardRecognizer(filesDir.absolutePath)
        captureService = ScreenCaptureService(this)

        setContent {
            ChineseChessAssistantTheme {
                MainScreen(
                    engineManager = engineManager,
                    onStartOverlay = { showOverlay() },
                    onRequestOverlayPermission = { requestOverlayPermission() },
                    onRequestCapturePermission = { requestCapturePermission() },
                    onAnalyzeBoard = { analyzeCurrentScreen() }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        engineManager.shutdown()
    }

    /**
     * 检查悬浮窗权限
     */
    private fun checkOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(this)
    }

    /**
     * 请求悬浮窗权限
     */
    private fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
        }
    }

    /**
     * 请求屏幕捕获权限
     */
    private fun requestCapturePermission() {
        val mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        capturePermissionLauncher.launch(captureIntent)
    }

    /**
     * 开始屏幕捕获
     */
    private fun startScreenCapture(resultData: Intent?) {
        if (resultData != null) {
            scope.launch {
                captureService.setMediaProjection(null) // 需要从 MediaProjectionManager 获取
                captureService.startCapture()
            }
        }
    }

    /**
     * 显示悬浮窗
     */
    private fun showOverlay() {
        if (checkOverlayPermission()) {
            OverlayService.show(this)
            Toast.makeText(this, "悬浮窗已显示", Toast.LENGTH_SHORT).show()
        } else {
            requestOverlayPermission()
            Toast.makeText(this, "请先授予悬浮窗权限", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * 分析当前屏幕
     */
    private fun analyzeCurrentScreen() {
        scope.launch {
            try {
                // 捕获屏幕
                val bitmap = captureService.captureFrame()
                if (bitmap != null) {
                    // 识别棋盘
                    val result = recognizer.recognizeBoard(bitmap)
                    if (result.success) {
                        Toast.makeText(
                            this@MainActivity,
                            "识别成功！棋子数量: ${result.pieces.size}",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "识别失败: ${result.error}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "无法捕获屏幕",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "分析失败: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}

/**
 * 主界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    engineManager: EngineManager,
    onStartOverlay: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onRequestCapturePermission: () -> Unit,
    onAnalyzeBoard: () -> Unit
) {
    val context = LocalContext.current
    var showSettings by remember { mutableStateOf(false) }
    var engineState by remember { mutableStateOf<String>("未启动") }
    var selectedColor by remember { mutableStateOf(PieceColor.RED) }
    var isAnalyzing by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("象棋助手") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onStartOverlay,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Text("悬浮窗")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 引擎状态
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "引擎状态: $engineState",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                isAnalyzing = true
                                // 启动引擎
                                val success = engineManager.start("/data/data/com.chess.assistant/pikafish")
                                engineState = if (success) "已启动" else "启动失败"
                                isAnalyzing = false
                            }
                        },
                        enabled = !isAnalyzing
                    ) {
                        Text(if (isAnalyzing) "启动中..." else "启动引擎")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 玩家颜色选择
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "选择你的颜色",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { selectedColor = PieceColor.RED },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedColor == PieceColor.RED)
                                    Color.Red else Color.Gray
                            )
                        ) {
                            Text("红方")
                        }
                        Button(
                            onClick = { selectedColor = PieceColor.BLACK },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedColor == PieceColor.BLACK)
                                    Color.Black else Color.Gray
                            )
                        ) {
                            Text("黑方")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 屏幕分析
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "屏幕分析",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onRequestCapturePermission,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("请求截屏权限")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onAnalyzeBoard,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("分析当前屏幕")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 使用说明
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "使用说明",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "1. 点击右上角设置图标配置引擎\n" +
                                "2. 授予悬浮窗权限\n" +
                                "3. 启动悬浮窗\n" +
                                "4. 打开其他象棋APP，悬浮窗会显示着法建议",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
