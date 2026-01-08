package com.example.driveondisplay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView

class RenderView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        drawPlaceholder(holder)
    }

    override fun surfaceChanged(
        holder: SurfaceHolder,
        format: Int,
        width: Int,
        height: Int
    ) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {}

    private fun drawPlaceholder(holder: SurfaceHolder) {
        val canvas: Canvas = holder.lockCanvas()
        canvas.drawColor(Color.BLACK)
        holder.unlockCanvasAndPost(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        val action = event.action

        // TODO: enviar touch para o notebook
        // NetworkClient.sendTouch(x, y, action)

        return true
    }
}
