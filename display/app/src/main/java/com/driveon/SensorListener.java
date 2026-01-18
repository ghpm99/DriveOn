package com.driveon;

import android.content.Context;
import android.util.Log;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.hardware.SensorEventListener;

import java.util.List;

public class SensorListener implements SensorEventListener {

    private Context context;
    private SensorDTO sensorDTO;
    private final SensorManager sensorManager;
    private final Sensor accel;
    private final Sensor orientation;

    // Gravidade (low-pass)
    private float gravX = 0f;
    private float gravY = 0f;

    // Filtro


    public SensorListener(Context context, SensorDTO sensorDTO) {
        this.context = context;
        this.sensorDTO = sensorDTO;
        listSensors();
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        orientation = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
    }


    private void listSensors() {
        SensorManager sm = (SensorManager) context.getSystemService(context.SENSOR_SERVICE);
        List<Sensor> sensors = sm.getSensorList(Sensor.TYPE_ALL);

        for (Sensor s : sensors) {
            Log.d(
                    "Sensors",
                    "Type=" + s.getType()
                            + " | Name=" + s.getName()
                            + " | Vendor=" + s.getVendor()
                            + " | Version=" + s.getVersion()
            );
        }
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
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            // Low-pass para gravidade
            gravX = SensorDTO.ALPHA * gravX + (1 - SensorDTO.ALPHA) * event.values[0];
            gravY = SensorDTO.ALPHA * gravY + (1 - SensorDTO.ALPHA) * event.values[1];

            // Aceleração linear (força G)
            float gX = (event.values[0] - gravX) / SensorManager.GRAVITY_EARTH;
            float gY = (event.values[1] - gravY) / SensorManager.GRAVITY_EARTH;

            this.sensorDTO.setGx(gX);
            this.sensorDTO.setGy(gY);
        }
    }
}
