package com.example.driveondisplay

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.min

class GForceView(
    context: Context,
    private val state: GForceState
) : View(context) {

    // Paleta Audi RS
    private val bgColor = Color.rgb(10, 10, 10)
    private val gridColor = Color.argb(80, 255, 255, 255)
    private val rsRed = Color.rgb(220, 20, 20)
    private val G_LIMIT = 1.2f

    private val paintGrid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = gridColor
    }

    private val paintTrail = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        color = rsRed
    }

    private val paintDot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = rsRed
    }

    private val paintMax = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
        textSize = 36f
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(bgColor)

        val cx = width * 0.5f
        val cy = height * 0.5f
        val radius = min(cx, cy) * 0.42f

        drawGrid(canvas, cx, cy, radius)
        drawTrail(canvas, cx, cy, radius)
        drawCurrentPoint(canvas, cx, cy, radius)
//        drawMaxIndicators(canvas, cx, cy, radius)

        invalidate() // loop contínuo
    }

    private fun drawGrid(c: Canvas, cx: Float, cy: Float, r: Float) {
        c.drawCircle(cx, cy, r, paintGrid)
        c.drawCircle(cx, cy, r * 0.5f, paintGrid)
        c.drawLine(cx - r, cy, cx + r, cy, paintGrid)
        c.drawLine(cx, cy - r, cx, cy + r, paintGrid)
    }

    private fun drawTrail(c: Canvas, cx: Float, cy: Float, r: Float) {
        val scale = r / 1.5f
        val size = state.trailX.size

        var lastX = 0f
        var lastY = 0f
        var first = true

        for (i in 0 until size) {
            val idx = (state.trailIndex + i) % size
            val x = cx + state.trailX[idx] * scale
            val y = cy - state.trailY[idx] * scale

            if (!first) {
                c.drawLine(lastX, lastY, x, y, paintTrail)
            }
            first = false
            lastX = x
            lastY = y
        }
    }

    private fun drawCurrentPoint(c: Canvas, cx: Float, cy: Float, r: Float) {
        val scale = r * 0.7f
        val x = cx + (state.gx / G_LIMIT) * scale
        val y = cy - (state.gy / G_LIMIT) * scale
        c.drawCircle(x, y, 12f, paintDot)
    }

    private fun drawMaxIndicators(c: Canvas, cx: Float, cy: Float, r: Float) {
        c.drawText("↑ %.2fG".format(state.maxPosY), cx - 50, cy - r - 20, paintMax)
        c.drawText("↓ %.2fG".format(state.maxNegY), cx - 50, cy + r + 40, paintMax)
        c.drawText("← %.2fG".format(state.maxNegX), cx - r - 140, cy + 10, paintMax)
        c.drawText("→ %.2fG".format(state.maxPosX), cx + r + 20, cy + 10, paintMax)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        val action = event.actionMasked

        NetworkClient.sendTouch(x, y, action)

        return true
    }

}
