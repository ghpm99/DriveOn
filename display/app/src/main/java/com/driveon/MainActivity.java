package com.driveon;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;

public class MainActivity extends Activity {

    private FrameSurfaceView view;
    private TelemetryManager telemetry;
    private NetworkWorker worker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Inicializa Sensores e Tela
        telemetry = new TelemetryManager(this);
        view = new FrameSurfaceView(this);
        setContentView(view);

        // 2. Mantém tela ligada
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // 3. Inicia Worker de Rede
        worker = new NetworkWorker(view, telemetry);
        worker.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        telemetry.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        telemetry.stop(); // Importante para salvar bateria se o app for para segundo plano
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        worker.shutdown();
    }
}