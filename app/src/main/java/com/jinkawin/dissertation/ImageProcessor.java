package com.jinkawin.dissertation;

import android.content.Context;
import android.util.Log;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class ImageProcessor {

    public static final String TAG = "ImageProcessor";
    private static final String PATH = "android.resource://com.jinkawin.dissertation/raw/";

    private static final String PERSON = "person";

    private static final double WIDTH = 416.0;
    private static final double HEIGHT = 416.0;
    private static final double SCALE_FACTOR = 1.0/255.0;
    private static final double MEAN = 127.5;
    private static final double CONFIDENCE_LEVEL = 0.5;

    private static final Scalar COLOUR_BLACK = new Scalar(255, 255, 255);

    private Context context;

    private Net network;
    private List<String> layerNetwork;
    private List<String> labels;


    public ImageProcessor(Context context){
        this.context = context;
        this.labels = this._readLabels(R.raw.coco_names);

        this._loadOpenCV();
    }

    public void process(Mat frame){
        Log.i(TAG, "Processing...");

        Box box = new Box(new Frame(
                (int) frame.size().width,
                (int) frame.size().height
        ));

        // https://www.pyimagesearch.com/2017/11/06/deep-learning-opencvs-blobfromimage-works/
        Mat blob = Dnn.blobFromImage(frame, SCALE_FACTOR, new Size(WIDTH, HEIGHT), new Scalar(MEAN, MEAN, MEAN), true, false); // size = (1, 4, 416, 416)
//        Mat blob = Dnn.blobFromImage(frame, SCALE_FACTOR, new Size(WIDTH, HEIGHT));

        this.network.setInput(blob);
        Mat detection = this.network.forward(); // (8112, 85)

        for(int row=0; row<detection.rows(); row++){
            ArrayList<Double> scores = new ArrayList<>();

            // Get score[5:]
            for (int col=5; col<detection.cols(); col++){
                scores.add(detection.get(row, col)[0]);
            }

            // Get the highest score index -> index = class
            // Class = ["person", "bicycle", "car", ...]
            double confident = Collections.max(scores);
            int max_index = scores.indexOf(confident);

            if(this.labels.get(max_index) == PERSON && confident > CONFIDENCE_LEVEL){

                // Initial box over detected object
                box.setCentreX(detection.get(row, 0)[0]);
                box.setCentreY(detection.get(row, 1)[0]);
                box.setWidth(detection.get(row, 2)[0]);
                box.setHeight(detection.get(row, 3)[0]);

                // Draw box over detected object
                Imgproc.rectangle(
                        frame,
                        new Rect(box.getX(), box.getY(), box.getWidth(), box.getHeight()),
                        COLOUR_BLACK
                );

            }
        }

    }

    /**
     * Callback when OpenCV libraries are loaded.
     */
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

    /**
     * Load OpenCV libraries (version 3.4.0)
     */
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

    /**
     * Load config file and model file to the network
     */
    private void _setupNetwork(){
        FileUtility fileUtility = new FileUtility(this.context);

        // Read and copy files to internal storage
        String weightPath = fileUtility.readAndCopyFile(R.raw.yolov3_weights, "yolov3_weights.weights");
        String configUri = fileUtility.readAndCopyFile(R.raw.yolov3_cfg, "yolov3_cfg.cfg");

        Log.i(TAG, "_setupNetwork");

        // Initial network
        this.network = Dnn.readNetFromDarknet(configUri, weightPath);
        this.layerNetwork = this.network.getLayerNames();
    }

    /**
     * Read all label (classification) classes
     * @param resId R.raw.{id}
     * @return List<String> list of labels
     */
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
