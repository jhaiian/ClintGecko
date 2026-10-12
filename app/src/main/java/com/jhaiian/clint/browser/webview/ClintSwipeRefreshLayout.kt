package com.jhaiian.clint.browser.webview

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class ClintSwipeRefreshLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SwipeRefreshLayout(context, attrs) {

    var canChildScrollUpCallback: (() -> Boolean)? = null

    override fun canChildScrollUp(): Boolean {
        return canChildScrollUpCallback?.invoke() ?: super.canChildScrollUp()
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (canChildScrollUp()) return false
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (canChildScrollUp()) return false
        return super.onTouchEvent(ev)
    }
}
