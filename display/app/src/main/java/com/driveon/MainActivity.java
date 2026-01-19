package com.driveon;

import android.app.Activity;

import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import java.util.List;

public class MainActivity extends Activity {

    private FrameSurfaceView view;
    private SensorListener sensorListener;
    private SensorDTO sensorDTO;
    private Network network;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sensorDTO = new SensorDTO();
        sensorListener = new SensorListener(this,sensorDTO);
        network = new Network(sensorDTO);

        setContentView(R.layout.activity_main);

        view = findViewById(R.id.frameView);
        view.setSensorDTO(sensorDTO);
        view.setOnTouchEventListener(network);

        network.connect();


        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }



    @Override
    protected void onResume() {
        super.onResume();

        sensorListener.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorListener.stop();
    }
}
