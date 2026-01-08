package com.example.driveondisplay

import android.content.Context
import android.graphics.*
import android.view.SurfaceHolder
import android.view.SurfaceView

class RenderView(context: Context) :
    SurfaceView(context),
    SurfaceHolder.Callback,
    Runnable {

    private var thread: Thread? = null
    @Volatile private var running = false

    init {
        holder.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        running = true
        thread = Thread(this)
        thread?.start()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        running = false
        thread?.join()
    }

    override fun surfaceChanged(
        holder: SurfaceHolder,
        format: Int,
        width: Int,
        height: Int
    ) {}

    override fun run() {
        val paint = Paint().apply {
            color = Color.GREEN
            textSize = 50f
            isAntiAlias = true
        }

        while (running) {
            val canvas = holder.lockCanvas()
            if (canvas != null) {
                canvas.drawRGB(20, 20, 20)
                canvas.drawText("DriveOn MVP", 100f, 200f, paint)
                holder.unlockCanvasAndPost(canvas)
            }
            Thread.sleep(16) // ~60 FPS
        }
    }
}
