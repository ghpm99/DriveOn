package com.driveon;

import android.graphics.Bitmap;
import android.util.Log;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.LinkedBlockingQueue;

public class ConnectionManager {

    // Fila agora recebe nosso objeto otimizado
    public final LinkedBlockingQueue<TouchEvent> touchQueue = new LinkedBlockingQueue<TouchEvent>();

    private final FrameSurfaceView view;
    private final TelemetryManager telemetry;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private boolean running = false;

    public ConnectionManager(FrameSurfaceView view, TelemetryManager telemetry) {
        this.view = view;
        this.telemetry = telemetry;
    }

    public void start() {
        running = true;
        new Thread(new ConnectionListener()).start();
    }

    public void stop() {
        running = false;
        try { if (serverSocket != null) serverSocket.close(); } catch (Exception e) {}
    }

    private class ConnectionListener implements Runnable {
        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(9000);
                Log.d("DriveOn", "Aguardando conexão...");

                while (running) {
                    clientSocket = serverSocket.accept();
                    clientSocket.setTcpNoDelay(true);
                    clientSocket.setSoTimeout(0);

                    Log.d("DriveOn", "Conectado! Iniciando Threads IO.");

                    Thread receiver = new Thread(new VideoReceiver(clientSocket));
                    Thread transmitter = new Thread(new DataTransmitter(clientSocket));

                    receiver.start();
                    transmitter.start();

                    receiver.join();

                    transmitter.interrupt();
                    clientSocket.close();
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    // RECEPTOR DE VÍDEO + CÁLCULO DE FPS
    private class VideoReceiver implements Runnable {
        private final Socket socket;
        private byte[] pixelData;
        private ByteBuffer pixelBuffer;
        private Bitmap reusableBitmap;

        public VideoReceiver(Socket s) { this.socket = s; }

        @Override
        public void run() {
            try {
                DataInputStream in = new DataInputStream(socket.getInputStream());

                long lastFpsTime = System.currentTimeMillis();
                int framesRendered = 0;

                while (running && !socket.isClosed()) {
                    int magic = in.readInt();
                    if (magic != 0xDEADBEEF) break;

                    int w = in.readInt();
                    int h = in.readInt();
                    int size = in.readInt();

                    if (pixelData == null || pixelData.length != size) {
                        pixelData = new byte[size];
                        pixelBuffer = ByteBuffer.wrap(pixelData);
                        reusableBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
                    }

                    in.readFully(pixelData);
                    pixelBuffer.rewind();
                    reusableBitmap.copyPixelsFromBuffer(pixelBuffer);

                    // Renderiza
                    view.updateFrame(reusableBitmap);
                    framesRendered++;

                    // --- CALCULA FPS A CADA 1 SEGUNDO ---
                    long now = System.currentTimeMillis();
                    if (now - lastFpsTime >= 1000) {
                        telemetry.currentFps = framesRendered; // Salva para o transmissor enviar
                        Log.d("DriveOn", "FPS Consumido: " + framesRendered);
                        framesRendered = 0;
                        lastFpsTime = now;
                    }
                }
            } catch (IOException e) { Log.e("VideoRx", "Erro: " + e.getMessage()); }
        }
    }

    // TRANSMISSOR DE DADOS (Protocolo Binário Modular)
    private class DataTransmitter implements Runnable {
        private final Socket socket;

        // Aloca os Buffers exatamente com o tamanho dos nossos "Pacotes"
        // 13 bytes: 1(Tipo) + 4(X) + 4(Y) + 4(Action)
        private final ByteBuffer touchBuffer = ByteBuffer.allocate(13);

        // 25 bytes: 1(Tipo) + 12(Acc) + 12(Mag)
        private final ByteBuffer fastBuffer = ByteBuffer.allocate(25);

        // 33 bytes: 1(Tipo) + 4(Light) + 4(Bat) + 8(Lat) + 8(Lon) + 4(Speed) + 4(FPS)
        private final ByteBuffer slowBuffer = ByteBuffer.allocate(33);

        public DataTransmitter(Socket s) {
            this.socket = s;
            // Configura todos para Little Endian (Padrão Go/C/x86)
            touchBuffer.order(ByteOrder.LITTLE_ENDIAN);
            fastBuffer.order(ByteOrder.LITTLE_ENDIAN);
            slowBuffer.order(ByteOrder.LITTLE_ENDIAN);
        }

        @Override
        public void run() {
            try {
                // Usando OutputStream direto (mais controle sobre envios binários imediatos)
                OutputStream out = socket.getOutputStream();

                long lastFastTime = 0;
                long lastSlowTime = 0;

                while (running && !socket.isClosed()) {
                    boolean sentData = false;
                    long now = System.currentTimeMillis();

                    // 1. EVENTO: Touch (Alta Prioridade)
                    while (!touchQueue.isEmpty()) {
                        TouchEvent t = touchQueue.poll();
                        if (t != null) {
                            touchBuffer.clear();
                            touchBuffer.put((byte) 0x01); // Header 0x01
                            touchBuffer.putFloat(t.x);
                            touchBuffer.putFloat(t.y);
                            touchBuffer.putInt(t.action);
                            out.write(touchBuffer.array(), 0, touchBuffer.position());
                            sentData = true;
                        }
                    }

                    // 2. POLLING RÁPIDO: Sensores Físicos (ex: 30ms = ~33Hz)
                    if (now - lastFastTime >= 30) {
                        lastFastTime = now;
                        fastBuffer.clear();
                        fastBuffer.put((byte) 0x02); // Header 0x02
                        fastBuffer.putFloat(telemetry.accX);
                        fastBuffer.putFloat(telemetry.accY);
                        fastBuffer.putFloat(telemetry.accZ);
                        fastBuffer.putFloat(telemetry.magX);
                        fastBuffer.putFloat(telemetry.magY);
                        fastBuffer.putFloat(telemetry.magZ);
                        out.write(fastBuffer.array(), 0, fastBuffer.position());
                        sentData = true;
                    }

                    // 3. POLLING LENTO: Sistema e GPS (ex: 1000ms = 1Hz)
                    if (now - lastSlowTime >= 1000) {
                        lastSlowTime = now;
                        slowBuffer.clear();
                        slowBuffer.put((byte) 0x03); // Header 0x03
                        slowBuffer.putFloat(telemetry.light);
                        slowBuffer.putInt(telemetry.batteryLevel);
                        slowBuffer.putDouble(telemetry.lat);
                        slowBuffer.putDouble(telemetry.lon);
                        slowBuffer.putFloat(telemetry.speed);
                        slowBuffer.putInt(telemetry.currentFps); // Envia o FPS pro Go
                        out.write(slowBuffer.array(), 0, slowBuffer.position());
                        sentData = true;
                    }

                    if (sentData) {
                        out.flush();
                    } else {
                        try { Thread.sleep(5); } catch (InterruptedException e) { break; }
                    }
                }
            } catch (IOException e) { Log.e("DataTx", "Erro: " + e.getMessage()); }
        }
    }
}