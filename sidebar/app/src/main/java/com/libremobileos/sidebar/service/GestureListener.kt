package com.libremobileos.sidebar.service

import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import com.libremobileos.sidebar.utils.Logger

/**
 * @author KindBrave
 * @since 2023/9/27
 */
class GestureListener(private val callback: Callback) : MGestureManager.MGestureListener {
    private val logger = Logger(TAG)
    private var initialTouchX = 0.0f
    private var initialTouchY = 0.0f
    private var isLongPress = false
    private val longPressHandler = Handler(Looper.getMainLooper())
    private val longPressRunnable = Runnable {
        isLongPress = true
        callback.beginMoveSideline()
    }

    // Handler and Runnable for inactivity timer
    private val inactivityHandler = Handler(Looper.getMainLooper())
    private val inactivityRunnable = Runnable {
        callback.setGesturePillTransparent(true)
    }

    companion object {
        private const val TAG = "GestureListener"
        private const val INACTIVITY_TIMEOUT = 3000L // 3 seconds
    }

    override fun singleFingerSlipAction(
        gestureEvent: MGestureManager.GestureEvent?,
        startEvent: MotionEvent?,
        endEvent: MotionEvent?,
        velocity: Float
    ): Boolean {
        if (null != gestureEvent) {
            if (gestureEvent == MGestureManager.GestureEvent.SINGLE_GINGER_LEFT_SLIP || gestureEvent == MGestureManager.GestureEvent.SINGLE_GINGER_RIGHT_SLIP) {
                callback.showSidebar()
            }
            return true
        }
        return false
    }

    override fun onTouchEvent(event: MotionEvent) {
        logger.d("onTouchEvent action=${event.action} x=${event.rawX} y=${event.rawY}")
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialTouchX = event.rawX
                initialTouchY = event.rawY

                isLongPress = false
                longPressHandler.postDelayed(longPressRunnable, 500)

                // Reset inactivity timer
                inactivityHandler.removeCallbacks(inactivityRunnable)
                inactivityHandler.postDelayed(inactivityRunnable, INACTIVITY_TIMEOUT)

                // Make the gesture pill visible on touch
                callback.setGesturePillTransparent(false)
            }
            MotionEvent.ACTION_MOVE -> {
                if (isLongPress) {
                    callback.moveSideline((event.rawX - initialTouchX).toInt(), (event.rawY - initialTouchY).toInt(), event.rawX.toInt(), event.rawY.toInt())
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                longPressHandler.removeCallbacks(longPressRunnable)
                isLongPress = false
                callback.endMoveSideline()

                // Reset inactivity timer
                inactivityHandler.removeCallbacks(inactivityRunnable)
                inactivityHandler.postDelayed(inactivityRunnable, INACTIVITY_TIMEOUT)
            }
        }
    }

    interface Callback {
        fun showSidebar()
        fun beginMoveSideline()
        fun moveSideline(xChanged: Int, yChanged: Int, touchX: Int, touchY: Int)
        fun endMoveSideline()
        fun setGesturePillTransparent(transparent: Boolean)
    }
}
