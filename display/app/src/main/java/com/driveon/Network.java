package com.driveon;

import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;

public class Network implements OnTouchEventListener {

    private final String serverAddress = "192.168.18.7";
    private SensorDTO sensorDTO;
    private int serverPort = 9000;
    private Socket socket;


    public Network(SensorDTO sensorDTO) {
        this.sensorDTO = sensorDTO;

    }

    private Socket getSocket() throws SocketException {
        if (this.socket == null || !this.socket.isConnected()) {
            throw new SocketException("Socket nao conectado");
        }
        return this.socket;
    }

    public synchronized void connect() {
        if(this.socket != null && this.socket.isConnected()){
            Log.d("Network", "Já conectado");
            return;
        }
        new Thread(() -> {
            Log.d("Network", "Tentando conectar");
            this.socket = new Socket();
            SocketAddress socketAddress = new InetSocketAddress(serverAddress, serverPort);
            try {
                socket.connect(socketAddress, 5000); // 5 seconds timeout
                Log.d("Network", "Connected to server: " + serverAddress + ":" + serverPort);
                sendAck();
            } catch (IOException e) {
                Log.e("Network", "Connection failed: " + e.getMessage());
            }
        }).start();
    }

    public void disconnect() {
        try {
            getSocket().close();
            Log.d("Network", "Disconnected from server.");

        } catch (IOException e) {
            Log.e("Network", "Disconnection failed: " + e.getMessage());
        }
    }

    public void sendSensorData() {
        String dataMsg = "{Type: 'SENSOR', AccelX: " + sensorDTO.getGx() + ", AccelY: " + sensorDTO.getGy() + "}";
        sendString(dataMsg);
    }

    private void sendString(String message) {
        try {
            OutputStream outputStream = getSocket().getOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(outputStream);

            dataOutputStream.write((message + "\n").getBytes("UTF-8"));
            dataOutputStream.flush();
            Log.d("Network", "Message sent: " + message);
        } catch (IOException e) {
            Log.e("Network", "Failed to send message: " + e.getMessage());
        }
    }

    public void sendAck(){
        sendString("ACK");
    }

    public void onTouch(float x, float y, int action) {
        String msg = "TOUCH " + x + " " + y + " " + action;
        sendString(msg);
    }

    public FrameDTO receiveFrame() {
        try {
            FrameDTO frame = new FrameDTO();
            DataInputStream in = new DataInputStream(getSocket().getInputStream());
            Log.d("Network", "Waiting for frame...");
            Log.d("Network", "Recebendo width");
            frame.setWidth(in.readInt());
            Log.d("Network", "Recebendo height");
            frame.setHeight(in.readInt());
            Log.d("Network", "Recebendo frameSize");
            frame.setFrameSize(in.readInt());
            Log.d("Network", "Recebendo data");
            byte[] data = new byte[frame.getFrameSize()];
            in.readFully(data);
            frame.setData(data);
            Log.d("Network", "Frame received");
            return frame;
        }catch (IOException e){
            Log.d("Network", e.getMessage());
            return null;
        }
    }
}