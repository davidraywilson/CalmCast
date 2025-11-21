package com.calmcast.podcast

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.Button

class FloatingPlayerService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingButton: Button? = null
    private var params: WindowManager.LayoutParams? = null
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false
    private val dragThreshold = 10

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        // Check if we have permission to draw overlay
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!android.provider.Settings.canDrawOverlays(this)) {
                stopSelf()
                return
            }
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        floatingButton = Button(this).apply {
            text = "▶"
            textSize = 20f
            setBackgroundColor(0xFF6200EE.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            isAllCaps = false
        }

        params = WindowManager.LayoutParams().apply {
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
            format = PixelFormat.RGBA_8888
            flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            width = 80
            height = 80
            gravity = Gravity.TOP or Gravity.END
            x = 0
            y = 200
        }

        try {
            windowManager.addView(floatingButton, params)
        } catch (e: Exception) {
            stopSelf()
            return
        }

        floatingButton?.setOnTouchListener { _, event ->
            handleTouchEvent(event)
        }
    }

    private fun handleTouchEvent(event: MotionEvent): Boolean {
        val p = params ?: return false

        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = p.x
                initialY = p.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isDragging = false
                true
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.rawX - initialTouchX
                val deltaY = event.rawY - initialTouchY

                if (kotlin.math.abs(deltaX) > dragThreshold || kotlin.math.abs(deltaY) > dragThreshold) {
                    isDragging = true
                    p.x = initialX + deltaX.toInt()
                    p.y = initialY + deltaY.toInt()
                    try {
                        windowManager.updateViewLayout(floatingButton, p)
                    } catch (e: Exception) {
                        // View might have been removed
                    }
                }
                true
            }
            MotionEvent.ACTION_UP -> {
                if (!isDragging) {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                }
                isDragging = false
                true
            }
            else -> false
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_UPDATE_PLAYBACK) {
            val isPlaying = intent.getBooleanExtra(EXTRA_IS_PLAYING, false)
            floatingButton?.text = if (isPlaying) "⏸" else "▶"
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (floatingButton != null) {
                windowManager.removeView(floatingButton)
            }
        } catch (e: Exception) {
            // View might already be removed
        }
        floatingButton = null
    }

    companion object {
        const val ACTION_UPDATE_PLAYBACK = "UPDATE_PLAYBACK_STATE"
        const val EXTRA_IS_PLAYING = "IS_PLAYING"
    }
}