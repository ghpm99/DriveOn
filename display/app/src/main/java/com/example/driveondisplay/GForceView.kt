package com.example.driveondisplay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import kotlin.math.min

class GForceView(
    context: Context,
    private val state: GForceState
) : View(context), Runnable {

    private var running = false
    private var lastFpsTime = 0L
    private var frameCount = 0
    private var fps = 0

    private val frameDelay = 16L // ~60 FPS

    private val bgPaint = Paint().apply { color = Color.rgb(10, 10, 10) }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.argb(80, 255, 255, 255)
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.rgb(220, 20, 20)
    }

    fun start() {
        running = true
        post(this)
    }

    fun stop() {
        running = false
    }

    override fun run() {
        if (!running) return
        invalidate()
        postDelayed(this, frameDelay)
    }

    override fun onDraw(canvas: Canvas) {
        val now = System.currentTimeMillis()
        frameCount++

        if (now - lastFpsTime >= 1000) {
            fps = frameCount
            frameCount = 0
            lastFpsTime = now
        }

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val cx = width * 0.5f
        val cy = height * 0.5f
        val radius = min(cx, cy) * 0.42f

        canvas.drawCircle(cx, cy, radius, gridPaint)
        canvas.drawLine(cx - radius, cy, cx + radius, cy, gridPaint)
        canvas.drawLine(cx, cy - radius, cx, cy + radius, gridPaint)

        val px = cx + state.gx * radius
        val py = cy - state.gy * radius

        canvas.drawCircle(px, py, 14f, dotPaint)
        drawFPS(canvas)

        postInvalidateOnAnimation()
    }

    private fun drawFPS(c: Canvas) {
        c.drawText(
            "FPS: $fps",
            30f,
            50f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.GREEN
                textSize = 36f
            }
        )
    }
}
