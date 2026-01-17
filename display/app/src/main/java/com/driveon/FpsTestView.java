package com.driveon;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

import java.util.Random;

public class FpsTestView extends View {

    private static final int BALL_COUNT = 50;

    private final float[] x = new float[BALL_COUNT];
    private final float[] y = new float[BALL_COUNT];
    private final float[] vx = new float[BALL_COUNT];
    private final float[] vy = new float[BALL_COUNT];

    private final Paint ballPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Random random = new Random();

    private boolean running = false;

    // FPS
    private long lastFpsTime = 0;
    private int frames = 0;
    private int fps = 0;

    // Loop
    private long lastFrameTime = 0;
    private static final long FRAME_DELAY_MS = 16; // ~60fps

    public FpsTestView(Context context) {
        super(context);

        ballPaint.setColor(Color.RED);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(32f);

        for (int i = 0; i < BALL_COUNT; i++) {
            x[i] = random.nextInt(800);
            y[i] = random.nextInt(480);
            vx[i] = random.nextFloat() * 8f - 4f;
            vy[i] = random.nextFloat() * 8f - 4f;
        }
    }

    public void start() {
        running = true;
        lastFrameTime = System.currentTimeMillis();
        lastFpsTime = lastFrameTime;
        invalidate();
    }

    public void stop() {
        running = false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!running) return;

        canvas.drawColor(Color.BLACK);

        long now = System.currentTimeMillis();
        update(now - lastFrameTime);
        lastFrameTime = now;

        drawBalls(canvas);
        drawFps(canvas);

        frames++;
        if (now - lastFpsTime >= 1000) {
            fps = frames;
            frames = 0;
            lastFpsTime = now;
        }

        postInvalidateDelayed(FRAME_DELAY_MS);
    }

    private void update(long deltaMs) {
        int w = getWidth();
        int h = getHeight();

        for (int i = 0; i < BALL_COUNT; i++) {
            x[i] += vx[i];
            y[i] += vy[i];

            if (x[i] < 0 || x[i] > w) vx[i] = -vx[i];
            if (y[i] < 0 || y[i] > h) vy[i] = -vy[i];
        }
    }

    private void drawBalls(Canvas canvas) {
        for (int i = 0; i < BALL_COUNT; i++) {
            canvas.drawCircle(x[i], y[i], 10f, ballPaint);
        }
    }

    private void drawFps(Canvas canvas) {
        canvas.drawText("FPS: " + fps, 20, 40, textPaint);
        canvas.drawText("Bolas: " + BALL_COUNT, 20, 80, textPaint);
    }
}
