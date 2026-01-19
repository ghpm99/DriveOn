package com.driveon;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.DataInputStream;
import java.net.Socket;

public class FrameClient implements Runnable {

    private final Network network;
    private final FrameSurfaceView surfaceView;

    public FrameClient(Network network, FrameSurfaceView view) {
        this.network = network;
        this.surfaceView = view;
    }

    @Override
    public void run() {
        try {

            while (true) {
                frame = network.receiveFrame();

                if (frame == null) {
                    continue;
                }

                // 🖼️ Decodifica JPEG → Bitmap
                Bitmap bitmap = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);

                if (bitmap != null) {
                    surfaceView.updateFrame(bitmap);
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
