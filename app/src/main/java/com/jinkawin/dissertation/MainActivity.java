package com.jinkawin.dissertation;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

import org.jcodec.common.model.Picture;
import org.opencv.core.Mat;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageProcessor imageProcessor = new ImageProcessor(this);
        VideoReader videoReader = new VideoReader(this);
        ArrayList<Mat> pictures = videoReader.readVideo(R.raw.video_6, "video_6.mp4");

        Log.i(TAG, "Pictures: " + pictures.size());
    }
}
