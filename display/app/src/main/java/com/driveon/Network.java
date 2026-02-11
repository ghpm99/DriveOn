package com.driveon;

import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;

public class Network implements OnTouchEventListener {

    private SensorDTO sensorDTO;
    private int serverPort = 9000;
    private Socket socket;
    private byte[] buffer;

    private DataOutputStream dataOutputStream;
    private DataInputStream dataInputStream;


    public Network(SensorDTO sensorDTO) {
        this.sensorDTO = sensorDTO;

    }

    private Socket getSocket() throws SocketException {
        if (this.socket == null || !this.socket.isConnected()) {
            throw new SocketException("Socket nao conectado");
        }
        return this.socket;
    }

    public synchronized void startServer(){
        new Thread(() -> {
            try {
                ServerSocket serverSocket = new ServerSocket(serverPort);
                Log.d("Network", "Server started on port " + serverPort);

                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    Log.d("Network", "Client connected: " + clientSocket.getInetAddress());

                    this.socket = clientSocket;
                    startDataStream();
                    handshake();

                }
        }catch(Exception e){
                e.printStackTrace();
        }
        });
    }

    private void startDataStream(){
        try {
            dataOutputStream = new DataOutputStream(getSocket().getOutputStream());
            dataInputStream = new DataInputStream(getSocket().getInputStream());

        } catch (Exception e){
            Log.e("Network", "Failed to start data stream: " + e.getMessage());
        }
    }

    private void handshake(){
        try {

            sendString("HELLO");
            String response = null;
            response = dataInputStream.readLine();

        Log.d("Network", "Handshake response: " + response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

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
            dataOutputStream.write((message + "\n").getBytes("UTF-8"));
            dataOutputStream.flush();
            Log.d("Network", "Message sent: " + message);
        } catch (IOException e) {
            Log.e("Network", "Failed to send message: " + e.getMessage());
        }
    }

    public void onTouch(float x, float y, int action) {
        String msg = "TOUCH " + x + " " + y + " " + action;
        sendString(msg);
    }

    public FrameDTO receiveFrame() {
        FrameDTO frame = new FrameDTO();
        try {
            frame.setWidth(dataInputStream.readInt());
            frame.setHeight(dataInputStream.readInt());
            frame.setFrameSize(dataInputStream.readInt());
            if(buffer == null || buffer.length != frame.getFrameSize()){
                buffer = new byte[frame.getFrameSize()];
            }
            dataInputStream.readFully(buffer);
            frame.setData(buffer);
            frame.setValid(true);
            return frame;
        }catch (IOException e){
            Log.d("Network", e.getMessage());
            frame.setValid(false);
            return frame;
        }
    }

    public boolean isConnected(){
        return this.socket != null && this.socket.isConnected();

    }
}