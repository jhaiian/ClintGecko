package com.jhaiian.clint.browser.webview

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlin.math.abs

class ClintSwipeRefreshLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SwipeRefreshLayout(context, attrs) {

    var canChildScrollUpCallback: (() -> Boolean)? = null
    var touchVerdictCallback: ((Long) -> Boolean?)? = null

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var gestureEligible = false
    private var armed = false
    private var dragging = false
    private var downX = 0f
    private var downY = 0f
    private var armX = 0f
    private var armY = 0f

    init {
        setDistanceToTriggerSync((TRIGGER_DISTANCE_DP * resources.displayMetrics.density).toInt())
    }

    override fun canChildScrollUp(): Boolean {
        if (dragging) return false
        return canChildScrollUpCallback?.invoke() ?: super.canChildScrollUp()
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                resetGesture()
                downX = ev.x
                downY = ev.y
                gestureEligible = isEnabled && !isRefreshing && (touchVerdictCallback != null || !canChildScrollUp())
                false
            }
            MotionEvent.ACTION_MOVE -> {
                if (!gestureEligible || (!dragging && !admitMove(ev))) return false
                dragging = super.onInterceptTouchEvent(ev)
                dragging
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (armed) super.onInterceptTouchEvent(ev)
                resetGesture()
                false
            }
            else -> {
                if (!dragging) gestureEligible = false
                false
            }
        }
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        val ending = ev.actionMasked == MotionEvent.ACTION_UP || ev.actionMasked == MotionEvent.ACTION_CANCEL
        val handled = dragging && super.onTouchEvent(ev)
        if (ending) resetGesture()
        return handled
    }

    private fun admitMove(ev: MotionEvent): Boolean {
        if (ev.pointerCount > 1 || (armed && canChildScrollUp())) {
            gestureEligible = false
            return false
        }
        if (armed) return isVerticalPull(ev.x - armX, ev.y - armY)
        if (!isVerticalPull(ev.x - downX, ev.y - downY) || canChildScrollUp()) return false
        val callback = touchVerdictCallback
        when (if (callback == null) true else callback(ev.downTime)) {
            null -> return false
            false -> {
                gestureEligible = false
                return false
            }
            true -> Unit
        }
        armed = true
        armX = ev.x
        armY = ev.y
        val down = MotionEvent.obtain(ev)
        down.action = MotionEvent.ACTION_DOWN
        super.onInterceptTouchEvent(down)
        down.recycle()
        return false
    }

    private fun isVerticalPull(dx: Float, dy: Float): Boolean {
        if (dy < -touchSlop || (abs(dx) > touchSlop && abs(dx) > abs(dy))) {
            gestureEligible = false
            return false
        }
        return true
    }

    private fun resetGesture() {
        gestureEligible = false
        armed = false
        dragging = false
    }

    private companion object {
        const val TRIGGER_DISTANCE_DP = 80
    }
}
