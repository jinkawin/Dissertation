package com.jinkawin.dissertation;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.media.Image;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.processVideo();
//        this.processImage();
    }

    public void processVideo(){

        // Initial
        ImageProcessor imageProcessor = new ImageProcessor(this);
        VideoReader videoReader = new VideoReader(this);

        // Read Video from RAW Folder
        ArrayList<Mat> mats = videoReader.readVideo(R.raw.video_6, "video_test.mp4");

        int i =0;
        for (Mat frame : mats) {
            Log.i("ImageProcessor", "frame: " + i++);

            /* TODO: Resize image */
            // resize...

            // Convert rgba to rgb
            Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGBA2RGB);

            imageProcessor.process(frame);
        }

    }

    public void processImage(){

        // Initial
//        ImageProcessor imageProcessor = new ImageProcessor(this);
//        ImageReader imageReader = new ImageReader(this);
//
//        Mat image = imageReader.readImage(R.raw.picturte_test, "picture_test.jpg");
//
//        Imgproc.cvtColor(image, image, Imgproc.COLOR_RGBA2RGB);
//
//        Mat mat = imageProcessor.process(image);
//
//
//        Bitmap savedImage = Bitmap.createBitmap(imageReader.bitmap);
//        Utils.matToBitmap(mat, savedImage);
//        MediaStore.Images.Media.insertImage(getContentResolver(), savedImage, "title", "description");
    }
}
