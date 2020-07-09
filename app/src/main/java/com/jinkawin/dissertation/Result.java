package com.jinkawin.dissertation;

import android.graphics.Bitmap;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

public class Result {

    private Mat frame;
    private int index;

    public Result(Mat frame, int index){
        this.frame = frame;
        this.index = index;
    }

    public Mat getFrame() {
        return frame;
    }

    public int getIndex() {
        return index;
    }

    public Bitmap getBitmap(){
        Bitmap bitmap = Bitmap.createBitmap(this.frame.width(), this.frame.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(frame, bitmap);

        return bitmap;
    }
}
