package com.driveon;

import android.graphics.Bitmap;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

public class ConnectionManager {

    // Fila Thread-Safe para o Touch (Garante ordem e não perde cliques)
    public final LinkedBlockingQueue<String> touchQueue = new LinkedBlockingQueue<String>();
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
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (Exception e) {
        }
    }

    // Thread 1: Aguarda conexão e inicia os trabalhos
    private class ConnectionListener implements Runnable {
        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(9000);
                Log.d("DriveOn", "Aguardando conexão...");

                while (running) {
                    clientSocket = serverSocket.accept();
                    clientSocket.setTcpNoDelay(true); // Vital para latência
                    clientSocket.setSoTimeout(0); // Leitura bloqueante infinita (vídeo)

                    Log.d("DriveOn", "Conectado! Iniciando Threads IO.");

                    // Inicia as duas vias separadas
                    Thread receiver = new Thread(new VideoReceiver(clientSocket));
                    Thread transmitter = new Thread(new DataTransmitter(clientSocket));

                    receiver.start();
                    transmitter.start();

                    // Aguarda uma delas morrer (monitoramento simples)
                    receiver.join();

                    // Cleanup
                    transmitter.interrupt();
                    clientSocket.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Thread 2: RECEPTOR DE VÍDEO (Alta prioridade, consome muita banda)
    private class VideoReceiver implements Runnable {
        private final Socket socket;
        private byte[] pixelData;
        private ByteBuffer pixelBuffer;
        private Bitmap reusableBitmap;

        public VideoReceiver(Socket s) {
            this.socket = s;
        }

        @Override
        public void run() {
            try {
                DataInputStream in = new DataInputStream(socket.getInputStream());

                // Handshake Entrada (Lê HELLO)
                // ... (Implementar leitura do HELLO se necessário, ou assumir conexao ok)
                String line = in.readLine();
                if (!"HELLO".equals(line)) throw new IOException("Invalid handshake");

                while (running && !socket.isClosed()) {
                    int magic = in.readInt(); // Bloqueia aqui esperando dados
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
                    view.updateFrame(reusableBitmap);
                }
            } catch (IOException e) {
                Log.e("VideoRx", "Erro: " + e.getMessage());
            }
        }
    }

    // Thread 3: TRANSMISSOR DE DADOS (Touch + Sensores)
    // Controla a taxa de envio (20Hz sensores, Touch imediato)
    private class DataTransmitter implements Runnable {
        private final Socket socket;
        private final StringBuilder sb = new StringBuilder(128);

        public DataTransmitter(Socket s) {
            this.socket = s;
        }

        @Override
        public void run() {
            try {
                BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());

                // Handshake Saída
                out.write("WELCOME\n".getBytes());
                out.flush();

                long lastSensorTime = 0;
                long SENSOR_INTERVAL_MS = 50; // 20Hz (1000ms / 20)

                while (running && !socket.isClosed()) {
                    boolean sentData = false;
                    long now = System.currentTimeMillis();

                    // 1. Prioridade: Esvaziar a fila de Touch
                    // Drena toda a fila para não acumular lag
                    while (!touchQueue.isEmpty()) {
                        String touchCmd = touchQueue.poll();
                        if (touchCmd != null) {
                            out.write(touchCmd.getBytes());
                            sentData = true;
                        }
                    }

                    // 2. Verifica se é hora de enviar Sensores (20Hz)
                    if (now - lastSensorTime >= SENSOR_INTERVAL_MS) {
                        lastSensorTime = now;

                        // Monta pacote de Sensores (T,accX,accY...)
                        sb.setLength(0);
                        sb.append("T,");
                        sb.append(format(telemetry.accX)).append(",");
                        sb.append(format(telemetry.accY)).append(","); // Y invertido se necessário
                        // ... outros sensores
                        sb.append("\n");

                        out.write(sb.toString().getBytes());
                        sentData = true;
                    }

                    // 3. Flush e Sleep Inteligente
                    if (sentData) {
                        out.flush();
                    } else {
                        // Se não tem nada pra fazer, dorme um pouco para economizar CPU
                        // Dorme 5ms (aprox 200Hz de polling rate no touch)
                        try {
                            Thread.sleep(5);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
            } catch (IOException e) {
                Log.e("DataTx", "Erro: " + e.getMessage());
            }
        }

        // Formatação rápida (evita String.format lento)
        private String format(float val) {
            return Float.toString(val); // Pode otimizar depois se precisar
        }
    }
}