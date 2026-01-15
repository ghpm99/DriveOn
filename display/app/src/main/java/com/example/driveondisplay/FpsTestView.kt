package com.example.driveondisplay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.random.Random

class FpsTestView(context: Context) : View(context) {

    private data class Ball(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        val r: Float
    )

    // ===== CONFIG =====
    private val BALL_COUNT = 40
    private val BG_COLOR = Color.rgb(15, 15, 15)

    // ===== PAINTS =====
    private val paintBall = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
    }

    private val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 42f
    }

    // ===== STATE =====
    private val balls = ArrayList<Ball>(BALL_COUNT)

    private var lastFrameTime = 0L
    private var fps = 0
    private var frames = 0
    private var lastFpsTime = 0L

    init {
        repeat(BALL_COUNT) {
            balls.add(
                Ball(
                    x = Random.nextFloat() * 800f,
                    y = Random.nextFloat() * 600f,
                    vx = Random.nextFloat() * 12f - 6f,
                    vy = Random.nextFloat() * 12f - 6f,
                    r = Random.nextFloat() * 12f + 8f
                )
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        val now = SystemClock.elapsedRealtime()

        if (lastFrameTime == 0L) {
            lastFrameTime = now
            lastFpsTime = now
        }

        val dt = (now - lastFrameTime) / 1000f
        lastFrameTime = now

        canvas.drawColor(BG_COLOR)

        updateBalls(dt)
        drawBalls(canvas)
        calculateFps(now)
        drawFps(canvas)

        // ✅ render sincronizado com VSYNC
        postInvalidateOnAnimation()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        parent?.requestDisallowInterceptTouchEvent(true)
        return true
    }

    private fun updateBalls(dt: Float) {
        val w = width.toFloat()
        val h = height.toFloat()

        for (b in balls) {
            b.x += b.vx * dt * 60f
            b.y += b.vy * dt * 60f

            if (b.x - b.r < 0 || b.x + b.r > w) b.vx = -b.vx
            if (b.y - b.r < 0 || b.y + b.r > h) b.vy = -b.vy
        }
    }

    private fun drawBalls(c: Canvas) {
        for (b in balls) {
            c.drawCircle(b.x, b.y, b.r, paintBall)
        }
    }

    private fun calculateFps(now: Long) {
        frames++
        if (now - lastFpsTime >= 1000) {
            fps = frames
            frames = 0
            lastFpsTime = now
        }
    }

    private fun drawFps(c: Canvas) {
        c.drawText("FPS: $fps", 30f, 60f, paintText)
        c.drawText("Bolas: $BALL_COUNT", 30f, 110f, paintText)
    }
}
