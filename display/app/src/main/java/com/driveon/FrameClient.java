package com.driveon;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.nio.ByteBuffer;

public class FrameClient implements Runnable {

    private final Network network;
    private final FrameSurfaceView surfaceView;
    private Bitmap reusableBitmap;
    private ByteBuffer pixelBuffer;

    public FrameClient(Network network, FrameSurfaceView view) {
        this.network = network;
        this.surfaceView = view;
    }

    @Override
    public void run() {

        try {
            while (true) {
                if(!network.isConnected()){
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                }
                FrameDTO frame = network.receiveFrame();

                if (frame == null || !frame.isValid()) {
                    Log.d("Network", "Invalid frame received");
                    continue;
                }

                if(reusableBitmap == null){
                    reusableBitmap = Bitmap.createBitmap(frame.getWidth(), frame.getHeight(), Bitmap.Config.RGB_565);
                    pixelBuffer = ByteBuffer.allocate(frame.getFrameSize());

                }

                if(pixelBuffer.capacity() != frame.getFrameSize()){
                    pixelBuffer = ByteBuffer.allocate(frame.getFrameSize());
                }

                pixelBuffer.clear();
                pixelBuffer.put(frame.getData());
                pixelBuffer.rewind();
                reusableBitmap.copyPixelsFromBuffer(pixelBuffer);

                surfaceView.updateFrame(reusableBitmap);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
