package com.jinkawin.dissertation;

import android.content.Context;
import android.util.Log;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;


public class ImageProcessor {

    public static final String TAG = "ImageProcessor";
    public static final String PATH = "android.resource://com.jinkawin.dissertation/raw/";

    private Context context;

    private Net network;
    private List<String> layerNetwork;
    private List<String> labels;

    private BaseLoaderCallback blCallback = new BaseLoaderCallback() {
        @Override
        public void onManagerConnected(int status) {
            if(status == LoaderCallbackInterface.SUCCESS){

                Log.i(TAG, "callback success");
                _setupNetwork();
            }else{
                Log.i(TAG, "callback else");
                super.onManagerConnected(status);
            }
        }
    };

    public ImageProcessor(Context context){
        this.context = context;
        this.labels = this._readLabels(R.raw.coco_names);

        this._loadOpenCV();
    }

    private void _loadOpenCV(){
        // If OpenCV's libraries are not loaded
        if(!OpenCVLoader.initDebug()){

            Log.i(TAG, "initDebug");
            boolean success = OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this.context, blCallback);
        }else{

            Log.i(TAG, "initDebug Success");
            blCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    private void _setupNetwork(){
        // Read and copy files to internal storage
        String weightPath = this._readAndCopyFile(R.raw.yolov3_weights, "yolov3_weights.weights");
        String configUri = this._readAndCopyFile(R.raw.yolov3_cfg, "yolov3_cfg.cfg");

        Log.i(TAG, "_setupNetwork");
        this.network = Dnn.readNetFromDarknet(configUri, weightPath);
        this.layerNetwork = this.network.getLayerNames();
    }

    // https://docs.opencv.org/3.4/d0/d6c/tutorial_dnn_android.html
    private String _readAndCopyFile(int resId, String fileName) {
        String filePath = "";
        BufferedInputStream bis;
        try {
            bis = new BufferedInputStream(this.context.getResources().openRawResource(resId));
            byte data[] = new byte[bis.available()];

            // Read file
            bis.read(data);
            bis.close();

            // Create copy to internal storage
            File file = new File(this.context.getFilesDir(), fileName);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(data);
            fos.close();

            filePath = file.getAbsolutePath();

        } catch (IOException e) {
            Log.e(TAG, "Fail to read file");
        }

        return filePath;
    }

    private List<String> _readLabels(int resId){
        BufferedReader br = new BufferedReader(new InputStreamReader(this.context.getResources().openRawResource(resId)));

        List<String> labels = new ArrayList<>();
        String line;

        try {
            while((line = br.readLine()) != null)
                labels.add(line);
            
        }catch (IOException e) {
            Log.i(TAG, "Cannot read classification file");
        }

        return labels;
    }


}
