package com.driveon;

public class FrameDTO {

    private int width;
    private int height;
    private int frameSize;
    private byte[] data;

    public int getWidth() {
        return width;
    }

    public void setWidth(int width){
        this.width = width;
    }

    public int getHeight(){
        return  height;
    }

    public void setHeight(int height){
        this.height = height;
    }
    public int getFrameSize(){
        return frameSize;
    }
    public void setFrameSize(int frameSize){
        this.frameSize = frameSize;
    }

    public byte[] getData(){
        return data;
    }

    public void setData(byte[] data){
        this.data = data;
    }

    public boolean isValid(){
        return data.length == frameSize && width > 0 && height > 0;
    }


}
