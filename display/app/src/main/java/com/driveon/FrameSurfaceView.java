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
    private Rect dstRect; // Aloca uma vez

    // Variáveis "Dirty" para o NetworkWorker ler
    public volatile boolean hasPendingTouch = false;
    public volatile float touchX, touchY;
    public volatile int touchAction;

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
        if (connectionManager == null) return true;

        int action = event.getActionMasked();

        // Otimização: Não precisamos de TODOS os moves se o Tablet for lento.
        // Mas para desenhar preciso, enviamos todos. O buffer TCP segura a onda.

        // Formata string "TOUCH X Y ACTION\n"
        // StringBuilder local é rápido na UI Thread
        String sb = "TOUCH " +
                (int) event.getX() + " " +
                (int) event.getY() + " " +
                action + "\n";

        // Adiciona na fila Thread-Safe (não bloqueia a UI)
        connectionManager.touchQueue.offer(sb);

        return true;
    }

    @Override public void surfaceCreated(SurfaceHolder holder) {}
    @Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}
    @Override public void surfaceDestroyed(SurfaceHolder holder) {}
}