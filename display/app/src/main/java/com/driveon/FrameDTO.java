package com.driveon;

public class FrameDTO {

    private int width;
    private int height;
    private int frameSize;
    private byte[] data;

    private boolean isValid;

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

    public void setValid(boolean isValid){
        this.isValid = isValid;
    }
    public boolean isValid(){
        return isValid && data.length == frameSize && width > 0 && height > 0;
    }


}
