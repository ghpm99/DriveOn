package com.driveon;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class FrameSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    private OnTouchEventListener touchListener;
    private SensorDTO sensorDTO;
    private SurfaceHolder holder;
    private Bitmap bitmap;

    public FrameSurfaceView(Context context,OnTouchEventListener listener,SensorDTO sensorDTO) {
        super(context);
        init();
        this.touchListener = listener;
        this.sensorDTO = sensorDTO;
    }

    public FrameSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init(){
        holder = getHolder();
        holder.addCallback(this);
    }

    public void updateFrame(Bitmap frame) {
        if (!holder.getSurface().isValid()) return;

        Canvas canvas = holder.lockCanvas();
        if (canvas != null) {
            Rect dst = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
            canvas.drawBitmap(frame, null, dst, null);
            holder.unlockCanvasAndPost(canvas);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {}

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {}

     public void setOnTouchEventListener(OnTouchEventListener listener) {
        this.touchListener = listener;
    }

    public void setSensorDTO(SensorDTO sensorDTO) {
        this.sensorDTO = sensorDTO;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (touchListener != null) {
            touchListener.onTouch(
                event.getX(),
                event.getY(),
                event.getActionMasked()
            );
        }
        return true;
    }
}
