package com.driveon;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.View;

public class GForceView extends View implements SensorEventListener {

    private final SensorManager sensorManager;
    private final Sensor accel;
    private final Sensor orientation;

    // Aceleração linear estimada
    private float gX = 0f;
    private float gY = 0f;

    // Gravidade (low-pass)
    private float gravX = 0f;
    private float gravY = 0f;

    // Filtro
    private static final float ALPHA = 0.9f;
    private static final float G_LIMIT = 1.0f;

    // Paints
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public GForceView(Context context) {
        super(context);

        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        orientation = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);

        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(3f);
        gridPaint.setColor(Color.GRAY);

        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setColor(Color.RED);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(32f);
    }

    public void start() {
        sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME);
        if (orientation != null) {
            sensorManager.registerListener(this, orientation, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    public void stop() {
        sensorManager.unregisterListener(this);
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
        float px = cx + (gX / G_LIMIT) * radius;
        float py = cy - (gY / G_LIMIT) * radius;

        canvas.drawCircle(px, py, 12f, dotPaint);

        canvas.drawText(String.format("GX: %.2f", gX), 20, 40, textPaint);
        canvas.drawText(String.format("GY: %.2f", gY), 20, 80, textPaint);

        // Loop de render (controlado)
        postInvalidateOnAnimation();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            // Low-pass para gravidade
            gravX = ALPHA * gravX + (1 - ALPHA) * event.values[0];
            gravY = ALPHA * gravY + (1 - ALPHA) * event.values[1];

            // Aceleração linear (força G)
            gX = (event.values[0] - gravX) / SensorManager.GRAVITY_EARTH;
            gY = (event.values[1] - gravY) / SensorManager.GRAVITY_EARTH;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
