package com.jinkawin.dissertation;

import android.graphics.Bitmap;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.Serializable;

public class Result implements Serializable {

    private Mat frame;
    private int index;

    private ResponseType status;

    public Result(ResponseType status){
        this.status = status;
    }

    public Result(Mat frame, int index){
        this.frame = frame;
        this.index = index;
        this.status = ResponseType.DATA;
    }

    public Mat getFrame() {
        return frame;
    }

    public int getIndex() {
        return index;
    }

    public ResponseType getStatus() {
        return status;
    }

    public Bitmap getBitmap(){
        Bitmap bitmap = Bitmap.createBitmap(this.frame.width(), this.frame.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(frame, bitmap);

        return bitmap;
    }
}
