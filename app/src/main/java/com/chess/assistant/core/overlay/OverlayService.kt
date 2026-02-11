package com.chess.assistant.core.overlay

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.chess.assistant.core.chess.ChessBoard
import com.chess.assistant.core.chess.Move
import com.chess.assistant.core.chess.PieceColor
import com.chess.assistant.core.engine.MoveSuggestion
import com.chess.assistant.core.ui.ChessBoardView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * 悬浮窗服务
 * 在屏幕上方显示象棋棋盘和着法提示
 */
class OverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private lateinit var windowManager: WindowManager
    private var overlayView: ComposeView? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // UI 状态
    private var _isVisible by mutableStateOf(false)
    val isVisible: Boolean get() = _isVisible

    private var _chessBoard by mutableStateOf(ChessBoard())
    val chessBoard: ChessBoard get() = _chessBoard

    private var _suggestedMove by mutableStateOf<Move?>(null)
    val suggestedMove: Move? get() = _suggestedMove

    private var _playerColor by mutableStateOf(PieceColor.RED)
    val playerColor: PieceColor get() = _playerColor

    // 悬浮窗参数
    private var layoutParams: LayoutParams? = null

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> showOverlay()
            ACTION_HIDE -> hideOverlay()
            ACTION_UPDATE_BOARD -> updateBoard(intent)
            ACTION_SHOW_MOVE -> showMoveSuggestion(intent)
            ACTION_SET_COLOR -> setPlayerColor(intent)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        hideOverlay()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        serviceScope.cancel()
        super.onDestroy()
    }

    /**
     * 显示悬浮窗
     */
    private fun showOverlay() {
        if (_isVisible) return

        lifecycleRegistry.currentState = Lifecycle.State.STARTED

        // 创建布局参数
        layoutParams = createLayoutParams()

        // 创建 ComposeView
        overlayView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)

            setContent {
                ChessBoardView(
                    chessBoard = _chessBoard,
                    suggestedMove = _suggestedMove,
                    playerColor = _playerColor,
                    onPieceClick = { /* 处理棋子点击 */ },
                    onMoveComplete = { /* 处理走棋完成 */ }
                )
            }
        }

        // 添加悬浮窗
        try {
            windowManager.addView(overlayView, layoutParams)
            _isVisible = true
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 隐藏悬浮窗
     */
    private fun hideOverlay() {
        if (!_isVisible) return

        try {
            overlayView?.let {
                windowManager.removeView(it)
            }
            overlayView = null
            _isVisible = false
            lifecycleRegistry.currentState = Lifecycle.State.CREATED
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 更新棋盘
     */
    private fun updateBoard(intent: Intent) {
        // 从 intent 中获取棋盘数据并更新
        // 这里可以扩展为接收序列化棋盘数据
        val boardData = intent.getStringExtra(EXTRA_BOARD_DATA)
        if (boardData != null) {
            // 解析棋盘数据
            // 更新 _chessBoard
        }
    }

    /**
     * 显示着法建议
     */
    private fun showMoveSuggestion(intent: Intent) {
        val fromX = intent.getIntExtra(EXTRA_MOVE_FROM_X, -1)
        val fromY = intent.getIntExtra(EXTRA_MOVE_FROM_Y, -1)
        val toX = intent.getIntExtra(EXTRA_MOVE_TO_X, -1)
        val toY = intent.getIntExtra(EXTRA_MOVE_TO_Y, -1)

        if (fromX >= 0 && fromY >= 0 && toX >= 0 && toY >= 0) {
            val piece = _chessBoard.getPieceAt(
                com.chess.assistant.core.chess.Position(fromX, fromY)
            )
            if (piece != null) {
                _suggestedMove = Move(
                    from = com.chess.assistant.core.chess.Position(fromX, fromY),
                    to = com.chess.assistant.core.chess.Position(toX, toY),
                    piece = piece
                )
            }
        }
    }

    /**
     * 设置玩家颜色
     */
    private fun setPlayerColor(intent: Intent) {
        val color = intent.getStringExtra(EXTRA_PLAYER_COLOR)
        _playerColor = when (color) {
            "RED" -> PieceColor.RED
            "BLACK" -> PieceColor.BLACK
            else -> PieceColor.RED
        }
    }

    /**
     * 创建悬浮窗布局参数
     */
    private fun createLayoutParams(): LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            LayoutParams.TYPE_PHONE
        }

        val flags = LayoutParams.FLAG_NOT_FOCUSABLE or
                LayoutParams.FLAG_NOT_TOUCH_MODAL or
                LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                LayoutParams.FLAG_LAYOUT_NO_LIMITS

        val params = LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            flags,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 100
        params.y = 100

        // 设置初始尺寸
        params.width = (400 * resources.displayMetrics.density).toInt()
        params.height = (450 * resources.displayMetrics.density).toInt()

        return params
    }

    /**
     * 更新悬浮窗位置
     */
    fun updatePosition(x: Int, y: Int) {
        layoutParams?.let { params ->
            params.x = x
            params.y = y
            overlayView?.let {
                windowManager.updateViewLayout(it, params)
            }
        }
    }

    /**
     * 更新悬浮窗大小
     */
    fun updateSize(width: Int, height: Int) {
        layoutParams?.let { params ->
            params.width = width
            params.height = height
            overlayView?.let {
                windowManager.updateViewLayout(it, params)
            }
        }
    }

    companion object {
        const val ACTION_SHOW = "com.chess.assistant.action.SHOW"
        const val ACTION_HIDE = "com.chess.assistant.action.HIDE"
        const val ACTION_UPDATE_BOARD = "com.chess.assistant.action.UPDATE_BOARD"
        const val ACTION_SHOW_MOVE = "com.chess.assistant.action.SHOW_MOVE"
        const val ACTION_SET_COLOR = "com.chess.assistant.action.SET_COLOR"

        const val EXTRA_BOARD_DATA = "board_data"
        const val EXTRA_MOVE_FROM_X = "move_from_x"
        const val EXTRA_MOVE_FROM_Y = "move_from_y"
        const val EXTRA_MOVE_TO_X = "move_to_x"
        const val EXTRA_MOVE_TO_Y = "move_to_y"
        const val EXTRA_PLAYER_COLOR = "player_color"

        /**
         * 显示悬浮窗
         */
        fun show(context: Context) {
            val intent = Intent(context, OverlayService::class.java).apply {
                action = ACTION_SHOW
            }
            context.startForegroundService(intent)
        }

        /**
         * 隐藏悬浮窗
         */
        fun hide(context: Context) {
            val intent = Intent(context, OverlayService::class.java).apply {
                action = ACTION_HIDE
            }
            context.startService(intent)
        }

        /**
         * 更新棋盘
         */
        fun updateBoard(context: Context, boardData: String) {
            val intent = Intent(context, OverlayService::class.java).apply {
                action = ACTION_UPDATE_BOARD
                putExtra(EXTRA_BOARD_DATA, boardData)
            }
            context.startService(intent)
        }

        /**
         * 显示着法建议
         */
        fun showMove(context: Context, fromX: Int, fromY: Int, toX: Int, toY: Int) {
            val intent = Intent(context, OverlayService::class.java).apply {
                action = ACTION_SHOW_MOVE
                putExtra(EXTRA_MOVE_FROM_X, fromX)
                putExtra(EXTRA_MOVE_FROM_Y, fromY)
                putExtra(EXTRA_MOVE_TO_X, toX)
                putExtra(EXTRA_MOVE_TO_Y, toY)
            }
            context.startService(intent)
        }

        /**
         * 设置玩家颜色
         */
        fun setColor(context: Context, color: PieceColor) {
            val intent = Intent(context, OverlayService::class.java).apply {
                action = ACTION_SET_COLOR
                putExtra(EXTRA_PLAYER_COLOR, color.name)
            }
            context.startService(intent)
        }
    }
}
