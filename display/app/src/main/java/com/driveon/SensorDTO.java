package com.driveon;

public class SensorDTO {

    private float gX = 0f;
    private float gY = 0f;

    public static final float ALPHA = 0.9f;
    public static final float G_LIMIT = 1.0f;

    public float getGx() {
        return gX;
    }

    public float getGy() {
        return gY;
    }

    public void setGx(float gX) {
        this.gX = gX;
    }
    public void setGy(float gY) {
        this.gY = gY;
    }
}
