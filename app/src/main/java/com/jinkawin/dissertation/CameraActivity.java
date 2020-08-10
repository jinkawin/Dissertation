package com.jinkawin.dissertation;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

public class CameraActivity extends org.opencv.android.CameraActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "CameraActivity";

    private static final int FPS = 5;
    private CameraBridgeViewBase cbvCamera;

    private long lastTime;
    private int frameCount;
    private PriorityQueue frameQueue;

    private BaseLoaderCallback blCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if(status == LoaderCallbackInterface.SUCCESS) {
                Log.i(TAG, "onManagerConnected: Setup OpenCV is done");

                // Init the first time
                lastTime = Core.getCPUTickCount();

                cbvCamera.enableView();
            }else{
                super.onManagerConnected(status);
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        loadOpenCV();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        frameQueue = new PriorityQueue<Result>(FPS*2, new ResultComparator());

        cbvCamera = findViewById(R.id.cbvCamera);
        cbvCamera.setVisibility(CameraBridgeViewBase.VISIBLE);
        cbvCamera.setCvCameraViewListener(this);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat mat = inputFrame.rgba();

        if(isReachFPS()){
            // Set time of the lastest processed image
            lastTime = Core.getCPUTickCount();
            Log.i(TAG, "onCameraFrame: Processing Image");
        }

        return mat;
    }

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(cbvCamera);
    }

    public boolean isReachFPS(){
        long currentTime = Core.getCPUTickCount();

        // Find gap between the last time of processed image and current time
        double diffTime = (currentTime - lastTime)/Core.getTickFrequency();

        // return true it is able to process image within FPS
        return (diffTime > (1.0/FPS));
    }

    public void loadOpenCV(){
        // If OpenCV's libraries are not loaded
        if(!OpenCVLoader.initDebug()){
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, blCallback);
        }else{
            blCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public class ProcessorBroadcastReceiver extends BroadcastReceiver {
        public static final String ACTION = "com.jinkawin.dissertation.SEND_PROCESS";

        @Override
        public void onReceive(Context context, Intent intent) {
            ArrayList<Result> results = ImageProcessorManager.getResults();
            Log.i(TAG, "onReceive: Result size: " + results.size());


            // TODO: sort array
            Collections.sort(results, new ResultComparator());
            Log.i(TAG, "processParallelVideo: Results' size: " + results.size());

        }
    }
}
