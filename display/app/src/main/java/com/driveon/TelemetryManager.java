package com.driveon;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Bundle;

public class TelemetryManager implements SensorEventListener, LocationListener {

    private final Context context;
    private final SensorManager sensorManager;
    private final LocationManager locationManager;

    // Dados Voláteis (Lidos pela Thread de Rede)
    public volatile float accX, accY, accZ;
    public volatile float magX, magY, magZ;
    public volatile float light;
    public volatile double lat, lon;
    public volatile float speed; // Guardado como float para economizar bytes na rede
    public volatile boolean hasGpsFix = false;

    // Novos Dados do Sistema
    public volatile int batteryLevel = 0;
    public volatile int currentFps = 0; // Atualizado pelo VideoReceiver

    private static final float ALPHA = 0.8f;
    private final float[] gravity = new float[3];

    public TelemetryManager(Context context) {
        this.context = context;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    // Ouve as mudanças de bateria do Android (não gasta bateria extra fazer isso)
    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            if (level >= 0 && scale > 0) {
                batteryLevel = (int) ((level / (float) scale) * 100);
            }
        }
    };

    public void start() {
        Sensor accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor magnet = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        Sensor lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        if (accel != null) sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME);
        if (magnet != null) sensorManager.registerListener(this, magnet, SensorManager.SENSOR_DELAY_UI);
        if (lightSensor != null) sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_UI);

        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 10, this);
        } catch (SecurityException e) { e.printStackTrace(); }

        context.registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    public void stop() {
        sensorManager.unregisterListener(this);
        locationManager.removeUpdates(this);
        try {
            context.unregisterReceiver(batteryReceiver);
        } catch (Exception e) {}
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                gravity[0] = ALPHA * gravity[0] + (1 - ALPHA) * event.values[0];
                gravity[1] = ALPHA * gravity[1] + (1 - ALPHA) * event.values[1];
                gravity[2] = ALPHA * gravity[2] + (1 - ALPHA) * event.values[2];

                accX = event.values[0] - gravity[0];
                accY = event.values[1] - gravity[1];
                accZ = event.values[2] - gravity[2];
                break;

            case Sensor.TYPE_MAGNETIC_FIELD:
                magX = event.values[0]; magY = event.values[1]; magZ = event.values[2];
                break;

            case Sensor.TYPE_LIGHT:
                light = event.values[0];
                break;
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        lat = location.getLatitude();
        lon = location.getLongitude();
        speed = location.getSpeed() * 3.6f;
        hasGpsFix = true;
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
    @Override public void onProviderEnabled(String provider) {}
    @Override public void onProviderDisabled(String provider) {}
}