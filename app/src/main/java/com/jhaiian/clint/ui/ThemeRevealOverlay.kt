package com.jhaiian.clint.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.View

class ThemeRevealOverlay(
    context: Context,
    private val bitmap: Bitmap,
    private val cx: Int,
    private val cy: Int
) : View(context) {

    var revealRadius: Float = 0f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val clipPath = Path()

    override fun onDraw(canvas: Canvas) {
        clipPath.reset()
        clipPath.addCircle(cx.toFloat(), cy.toFloat(), revealRadius, Path.Direction.CW)
        canvas.save()
        canvas.clipOutPath(clipPath)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        canvas.restore()
    }
}
