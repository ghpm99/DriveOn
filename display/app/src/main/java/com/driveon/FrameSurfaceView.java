package com.driveon;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class FrameSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    private SurfaceHolder holder;
    private Rect dstRect; // Aloca uma vez

    // Variáveis "Dirty" para o NetworkWorker ler
    public volatile boolean hasPendingTouch = false;
    public volatile float touchX, touchY;
    public volatile int touchAction;

    public FrameSurfaceView(Context context) {
        super(context);
        holder = getHolder();
        holder.addCallback(this);
    }

    public void updateFrame(Bitmap frame) {
        if (!holder.getSurface().isValid()) return;

        Canvas canvas = holder.lockCanvas();
        if (canvas != null) {
            // Inicializa Rect apenas na primeira vez ou se mudar tamanho
            if (dstRect == null) {
                dstRect = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
            }

            // Desenha sem filtrar (filter=false é mais rápido e o pixel art fica nítido)
            canvas.drawBitmap(frame, null, dstRect, null);
            holder.unlockCanvasAndPost(canvas);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Apenas armazena os valores. O NetworkWorker vai serializar e enviar.
        // Isso evita travar a UI Thread com I/O de rede.
        this.touchX = event.getX();
        this.touchY = event.getY();
        this.touchAction = event.getActionMasked();
        this.hasPendingTouch = true;
        return true;
    }

    @Override public void surfaceCreated(SurfaceHolder holder) {}
    @Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}
    @Override public void surfaceDestroyed(SurfaceHolder holder) {}
}