package com.jinkawin.dissertation;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;

public class ImageReader {

    public static final String TAG = "VideoReader";
    private static final String PATH_RAW = "android.resource://com.jinkawin.dissertation/raw/";

    private Context context;

    public Bitmap bitmap;

    public ImageReader(Context context){
        this.context = context;
    }

    /**
     * Read video from raw folder
     * @param resId   R.raw.{id}
     * @return ArrayList<Picture>
     */
    public Mat readImage(int resId, String filename){
        FileUtility fileUtility = new FileUtility(this.context);

        String filePath = fileUtility.readAndCopyFile(resId, filename);
        File file = new File(filePath);

        return imageToMat(file);
    }

    public Mat imageToMat(File file){
        Mat mat = new Mat();
        this.bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        Utils.bitmapToMat(bitmap, mat);

        return mat;
    }

    public Mat pathToMat(String path){
        Mat mat = new Mat();
        this.bitmap = BitmapFactory.decodeFile(path);
        Utils.bitmapToMat(bitmap, mat);

        return mat;
    }

    public Mat bitmapToMat(Bitmap bitmap){
        Mat mat = new Mat();
        this.bitmap = bitmap;
        Utils.bitmapToMat(bitmap, mat);

        return mat;
    }

}
