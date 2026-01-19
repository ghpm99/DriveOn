package com.driveon;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

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
                byte[] frame = network.receiveFrame();

                if (frame == null) {
                    continue;
                }

                // 🖼️ Decodifica JPEG → Bitmap
                Bitmap bitmap = BitmapFactory.decodeByteArray(frame, 0, frame.length);

                if (bitmap != null) {
                    surfaceView.updateFrame(bitmap);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
