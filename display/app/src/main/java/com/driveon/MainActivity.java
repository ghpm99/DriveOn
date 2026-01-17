package com.driveon;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import java.util.List;

public class MainActivity extends Activity {

    private GForceView view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        listSensors();

        view = new GForceView(this);
        setContentView(view);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void listSensors() {
        SensorManager sm = (SensorManager) getSystemService(SENSOR_SERVICE);
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

    @Override
    protected void onResume() {
        super.onResume();
        view.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        view.stop();
    }
}
