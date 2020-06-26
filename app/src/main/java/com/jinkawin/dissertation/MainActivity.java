package com.jinkawin.dissertation;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

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
    }

    public void processVideo(){

        // Initial
        ImageProcessor imageProcessor = new ImageProcessor(this);
        VideoReader videoReader = new VideoReader(this);

        // Read Video from RAW Folder
        ArrayList<Mat> mats = videoReader.readVideo(R.raw.video_6, "video_6.mp4");

        for (Mat frame : mats) {

            // Convert rgba to rgb
            Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGBA2RGB);

            imageProcessor.process(frame);
        }

    }
}
