package com.driveon;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.nio.ByteBuffer;

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
                FrameDTO frame = network.receiveFrame();

                if (!frame.isValid()) {
                    Log.d("Network", "Invalid frame received");
                    continue;
                }

                // 🖼️ Decodifica JPEG → Bitmap
                Bitmap bitmap = Bitmap.createBitmap(frame.getWidth(), frame.getHeight(), Bitmap.Config.RGB_565);
                ByteBuffer buffer = ByteBuffer.wrap(frame.getData());
                bitmap.copyPixelsFromBuffer(buffer);

                surfaceView.updateFrame(bitmap);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
