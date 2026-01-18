package com.driveon;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import android.view.View;

public class GForceView extends View {

    private final SensorDTO sensorDTO;
    // Paints
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public GForceView(Context context, SensorDTO sensorDTO) {
        super(context);
        this.sensorDTO = sensorDTO;

        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(3f);
        gridPaint.setColor(Color.GRAY);

        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setColor(Color.RED);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(32f);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.rgb(10, 10, 10));

        float cx = getWidth() * 0.5f;
        float cy = getHeight() * 0.5f;
        float radius = Math.min(cx, cy) * 0.4f;

        // Grade
        canvas.drawCircle(cx, cy, radius, gridPaint);
        canvas.drawLine(cx - radius, cy, cx + radius, cy, gridPaint);
        canvas.drawLine(cx, cy - radius, cx, cy + radius, gridPaint);

        // Mapeia G -> tela
        float px = cx + (this.sensorDTO.getGx() / SensorDTO.G_LIMIT) * radius;
        float py = cy - (this.sensorDTO.getGy() / SensorDTO.G_LIMIT) * radius;

        canvas.drawCircle(px, py, 12f, dotPaint);

        canvas.drawText(String.format("GX: %.2f", this.sensorDTO.getGx()), 20, 40, textPaint);
        canvas.drawText(String.format("GY: %.2f", this.sensorDTO.getGy()), 20, 80, textPaint);

        // Loop de render (controlado)
        postInvalidateOnAnimation();
    }

    public void setOnTouchEventListener(OnTouchEventListener listener) {
        this.touchListener = listener;
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
