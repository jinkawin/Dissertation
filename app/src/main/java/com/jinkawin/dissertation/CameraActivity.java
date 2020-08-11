package com.jinkawin.dissertation;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

public class CameraActivity extends org.opencv.android.CameraActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "CameraActivity";

    private static final int FPS = 10;
    private CameraBridgeViewBase cbvCamera;

    private PriorityQueue frameQueue;
    private long lastTime;
    private int frameCount;
    private int order = 0;

    public ModelType modelType = ModelType.SSD;

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
    protected void onDestroy() {
        super.onDestroy();
        reset();
    }

    @Override
    protected void onPause() {
        super.onPause();
        reset();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        // Back Button
        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
            }
        });

        // Setup Model
        ModelSetup.setup(this, this.modelType);

        // Setup ImageProcessor
        ImageProcessorManager.setProcessor(this, ModelSetup.weightPath, ModelSetup.configPath, true);

        cbvCamera = findViewById(R.id.cbvCamera);
        cbvCamera.setVisibility(CameraBridgeViewBase.VISIBLE);
        cbvCamera.setCvCameraViewListener(this);
        cbvCamera.setMaxFrameSize(400,400);
        cbvCamera.enableFpsMeter();
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

            processParallelVideo(mat);
//            mat = ModelSetup.imageProcessor.process(mat);

            Log.i(TAG, "onCameraFrame: Processing Image");
        }

        if(ImageProcessorManager.streamResult.size() != 0){
            // If there is processed frame in the buffer
            mat = ImageProcessorManager.streamResult.remove().getFrame();
        }else{

            // Draw Rectagle from previos frame
            for (DetectionLog log : ImageProcessorManager.detectionLogs) {
                Imgproc.rectangle(
                        mat,
                        log.getDetectedRect(),
                        log.getColour()
                );
            }
        }

        return mat;
    }

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(cbvCamera);
    }

    private void reset(){
        if (cbvCamera != null) {
            cbvCamera.disableView();
        }

        order = 0;

        // Clear buffer
        ImageProcessorManager.streamResult.clear();
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

    public void processParallelVideo(Mat frame){

        // Calculate new size
        Size ogSize = frame.size();
        double ratio = ogSize.width/ModelSetup.WIDTH;
        Size newSize = new Size(ModelSetup.WIDTH, ogSize.height/ratio);

        Log.i(TAG, "processParallelVideo: process order: " + order++);
        ImageProcessorManager.process(frame, newSize, order++, this.modelType);
    }
}
