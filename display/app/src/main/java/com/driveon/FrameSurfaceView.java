package com.driveon;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class FrameSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    private final SurfaceHolder holder;
    private Rect dstRect;

    private ConnectionManager connectionManager;

    public void setConnectionManager(ConnectionManager cm) {
        this.connectionManager = cm;
    }

    public FrameSurfaceView(Context context) {
        super(context);
        holder = getHolder();
        holder.addCallback(this);
    }

    public void updateFrame(Bitmap frame) {
        if (!holder.getSurface().isValid()) return;

        Canvas canvas = holder.lockCanvas();
        if (canvas != null) {
            if (dstRect == null) {
                dstRect = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
            }
            canvas.drawBitmap(frame, null, dstRect, null);
            holder.unlockCanvasAndPost(canvas);
        }
        frame.recycle();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (connectionManager == null) return true;

        // Cria o evento e joga na fila instantaneamente
        TouchEvent touch = new TouchEvent(
                event.getX(),
                event.getY(),
                event.getActionMasked()
        );

        connectionManager.touchQueue.offer(touch);

        return true;
    }

    @Override public void surfaceCreated(SurfaceHolder holder) {}
    @Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}
    @Override public void surfaceDestroyed(SurfaceHolder holder) {}
}