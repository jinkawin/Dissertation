package com.jinkawin.dissertation;

import android.content.Context;
import android.util.Log;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

public class ModelSetup {
    private static final String TAG = "ModelSetup";

    public static final int WIDTH = 480;
    public static Context context;

    public static ImageProcessor imageProcessor;

    public static String weightPath;
    public static String configPath;

    private static BaseLoaderCallback blCallback = new BaseLoaderCallback() {
        @Override
        public void onManagerConnected(int status) {
            if(status == LoaderCallbackInterface.SUCCESS){
                Log.i(TAG, "onManagerConnected: Setup OpenCV is done");
            }else{
                super.onManagerConnected(status);
            }
        }
    };

    public static void setup(Context context, ModelType type){
        ModelSetup.context = context;
        loadOpenCV();
        Log.i(TAG, "setup: Yes");

        // Read and copy files to internal storage
        switch (type){
            case SSD:
                _readSSD();
                break;
            case YOLO:
                _readYOLO();
                break;
            default:
                type = ModelType.SSD;
                _readSSD();
        }

        // Initial
        imageProcessor = new ImageProcessor(context, weightPath, configPath, type);
    }

    public ImageProcessor getImageProcessor() {
        return imageProcessor;
    }

    public String getWeightPath() {
        return weightPath;
    }

    public String getConfigPath() {
        return configPath;
    }

    public static void loadOpenCV(){
        Log.i(TAG, "loadOpenCV: Yes");
        // If OpenCV's libraries are not loaded
        if(!OpenCVLoader.initDebug()){
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, context, blCallback);
            Log.i(TAG, "OpenCV library is loaded!");
        }else{
            blCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    private static void _readYOLO(){
        // Read and copy files to internal storage
        FileUtility fileUtility = new FileUtility(context);
        weightPath = fileUtility.readAndCopyFile(R.raw.yolov3_weights, "yolov3_weights.weights");
        configPath = fileUtility.readAndCopyFile(R.raw.yolov3_cfg, "yolov3_cfg.cfg");

    }

    private static void _readSSD(){
        // Read and copy files to internal storage
        FileUtility fileUtility = new FileUtility(context);
        weightPath = fileUtility.readAndCopyFile(R.raw.mobilenetssd_weight, "mobilenetssd_weight.caffemodel");
        configPath = fileUtility.readAndCopyFile(R.raw.mobilenetssd_config, "mobilenetssd_config.prototxt");
    }
}
