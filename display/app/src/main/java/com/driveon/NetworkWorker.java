package com.driveon;

import android.graphics.Bitmap;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;

public class NetworkWorker extends Thread {

    private final FrameSurfaceView view;
    private final TelemetryManager telemetry;
    private boolean running = true;

    // Buffers reutilizáveis (Zero Allocation Loop)
    private byte[] pixelData;
    private Bitmap reusableBitmap;
    private ByteBuffer pixelBuffer;
    private final StringBuilder sb = new StringBuilder(128); // Buffer para strings de sensores

    public NetworkWorker(FrameSurfaceView view, TelemetryManager telemetry) {
        this.view = view;
        this.telemetry = telemetry;
    }

    public void shutdown() {
        running = false;
        try {
            // Force socket close logic here if needed
        } catch (Exception e) {}
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(9000)) {
            Log.d("DriveOn", "Servidor TCP iniciado na porta 9000");

            while (running) {
                Socket socket = null;
                try {
                    socket = serverSocket.accept();
                    socket.setTcpNoDelay(true); // Vital para latência baixa
                    socket.setSoTimeout(5000);  // Timeout de leitura

                    handleConnection(socket);

                } catch (IOException e) {
                    Log.e("DriveOn", "Erro na conexão: " + e.getMessage());
                } finally {
                    if (socket != null && !socket.isClosed()) socket.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleConnection(Socket socket) throws IOException {
        DataInputStream in = new DataInputStream(socket.getInputStream());
        BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        // 1. Handshake
        String line = reader.readLine();
        if (!"HELLO".equals(line)) return;

        out.write("WELCOME\n".getBytes());
        out.flush();

        // Variável para controle de envio de sensores (ex: a cada 10 frames de vídeo)
        int sensorTick = 0;

        // 2. Loop Principal
        while (running && !socket.isClosed()) {
            // --- LEITURA DO VÍDEO ---
            int magic = in.readInt();
            if (magic != 0xDEADBEEF) break;

            int w = in.readInt();
            int h = in.readInt();
            int size = in.readInt();

            // Alocação Lazy (só aloca se mudar o tamanho)
            if (pixelData == null || pixelData.length != size) {
                pixelData = new byte[size];
                pixelBuffer = ByteBuffer.wrap(pixelData); // Wrapper leve
                // Bitmap RGB_565 é nativo do Android, consome metade da RAM do ARGB_8888
                reusableBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
            }

            // Lê o frame direto no array de bytes
            in.readFully(pixelData);

            // Copia bytes para o Bitmap (Rápido via JNI)
            pixelBuffer.rewind();
            reusableBitmap.copyPixelsFromBuffer(pixelBuffer);

            // Renderiza na tela
            view.updateFrame(reusableBitmap);

            // --- ENVIO DE SENSORES (Intercalado) ---
            // Verifica se há toque pendente na SurfaceView
            if (view.hasPendingTouch) {
                // Formato: TOUCH X Y ACTION
                sb.setLength(0); // Limpa o StringBuilder (sem alocar nova string)
                sb.append("TOUCH ");
                sb.append((int)view.touchX).append(" "); // Cast para int economiza bytes
                sb.append((int)view.touchY).append(" ");
                sb.append(view.touchAction).append("\n");

                out.write(sb.toString().getBytes()); // Aqui ainda gera byte[], mas é menos pior
                view.hasPendingTouch = false;
            }

            // Envia telemetria a cada X frames para não saturar o canal de upload
            // ou se o processador estiver livre
            if (sensorTick++ > 5) {
                sensorTick = 0;
                sendTelemetry(out);
            }

            // Importante: Flush para empurrar os dados pro Go
            out.flush();
        }
    }

    private void sendTelemetry(BufferedOutputStream out) throws IOException {
        // Formato CSV compactado: T,AccX,AccY,AccZ,MagX,MagY,MagZ,Light,GPSLat,GPSLon,Speed
        // T = Telemetry
        sb.setLength(0);
        sb.append("T,");
        sb.append(String.format("%.2f", telemetry.accX)).append(",");
        sb.append(String.format("%.2f", telemetry.accY)).append(",");
        sb.append(String.format("%.2f", telemetry.accZ)).append(",");
        sb.append(String.format("%.2f", telemetry.light)).append(",");

        if (telemetry.hasGpsFix) {
            sb.append(telemetry.lat).append(",");
            sb.append(telemetry.lon).append(",");
            sb.append((int)telemetry.speed);
        } else {
            sb.append("0,0,0");
        }
        sb.append("\n");

        out.write(sb.toString().getBytes());
    }
}