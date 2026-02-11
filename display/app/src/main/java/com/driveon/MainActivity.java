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
    private FrameClient frameClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("MainActivity", "onCreate");

        sensorDTO = new SensorDTO();
        sensorListener = new SensorListener(this,sensorDTO);
        network = new Network(sensorDTO);
        network.startServer();

        view = new FrameSurfaceView(this, network, sensorDTO);
        setContentView(view);

        frameClient = new FrameClient(network, view);

        new Thread(
                frameClient
        ).start();


        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        network.disconnect();
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
