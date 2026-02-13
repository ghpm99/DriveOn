package com.driveon;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

// Classe "Singleton-ish" gerenciada pela MainActivity
// Foco: Armazenar o estado atual dos sensores sem criar lixo na memória
public class TelemetryManager implements SensorEventListener, LocationListener {

    private final SensorManager sensorManager;
    private final LocationManager locationManager;

    // Dados voláteis (lidos pela thread de rede)
    public volatile float accX, accY, accZ;
    public volatile float magX, magY, magZ;
    public volatile float light;
    public volatile double lat, lon, speed; // GPS
    public volatile boolean hasGpsFix = false;

    // Filtro Low-Pass (Simples e eficiente)
    private static final float ALPHA = 0.8f;
    private final float[] gravity = new float[3];

    public TelemetryManager(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    public void start() {
        // Usar SENSOR_DELAY_GAME (20ms) é ok, mas UI (60ms) economiza bateria no Tab 2
        Sensor accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor magnet = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        Sensor lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        if (accel != null) sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME);
        if (magnet != null) sensorManager.registerListener(this, magnet, SensorManager.SENSOR_DELAY_UI);
        if (lightSensor != null) sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_UI);

        // GPS: Solicita update a cada 1 segundo ou 10 metros para economizar bateria
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 10, this);
        } catch (SecurityException e) {
            e.printStackTrace(); // Tratar permissão no Android 6+ (O Tab 2 é 4.1, não precisa runtime permission)
        }
    }

    public void stop() {
        sensorManager.unregisterListener(this);
        locationManager.removeUpdates(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Switch case é mais rápido que if/else
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                // Filtro de gravidade básico
                gravity[0] = ALPHA * gravity[0] + (1 - ALPHA) * event.values[0];
                gravity[1] = ALPHA * gravity[1] + (1 - ALPHA) * event.values[1];
                gravity[2] = ALPHA * gravity[2] + (1 - ALPHA) * event.values[2];

                accX = event.values[0] - gravity[0];
                accY = event.values[1] - gravity[1];
                accZ = event.values[2] - gravity[2];
                break;

            case Sensor.TYPE_MAGNETIC_FIELD:
                magX = event.values[0];
                magY = event.values[1];
                magZ = event.values[2];
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
        speed = location.getSpeed() * 3.6f; // m/s para km/h
        hasGpsFix = true;
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
    @Override public void onProviderEnabled(String provider) {}
    @Override public void onProviderDisabled(String provider) {}
}