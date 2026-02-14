package com.driveon;

public class TouchEvent {
    public float x;
    public float y;
    public int action;

    public TouchEvent(float x, float y, int action) {
        this.x = x;
        this.y = y;
        this.action = action;
    }
}