package com.jinkawin.dissertation;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.InstallCallbackInterface;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
        // Set up network
        String weightUri = Uri.parse(PATH + "yolov3_weights").getPath();
        String configUri = Uri.parse(PATH + "yolov3_cfg").getPath();

        Log.i(TAG, "_setupNetwork");
        this.network = Dnn.readNetFromDarknet(configUri, weightUri);
        this.layerNetwork = this.network.getLayerNames();
    }

    private void _readClassification() throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(this.context.getResources().openRawResource(R.raw.coco_names)));
        String line;
        while((line = br.readLine()) != null){
            System.out.println(line);
        }
    }


}
