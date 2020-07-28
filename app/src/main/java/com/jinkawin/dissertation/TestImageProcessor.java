package com.jinkawin.dissertation;

import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import static org.opencv.dnn.Dnn.DNN_BACKEND_OPENCV;
import static org.opencv.dnn.Dnn.DNN_TARGET_CPU;

public class TestImageProcessor {

    private static final String TAG = "TestImageProcessor";

    private static final double WIDTH = 416.0;
    private static final double HEIGHT = 416.0;
    private static final double SCALE_FACTOR = 1.0/255.0;

    private Net network;

    public void process(Mat frame, String weightPath, String configUri) {
        this.network = Dnn.readNetFromDarknet(configUri, weightPath);
        this.network.setPreferableBackend(DNN_BACKEND_OPENCV);
        this.network.setPreferableTarget(DNN_TARGET_CPU);

        Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGBA2RGB);
        Mat blob = Dnn.blobFromImage(frame, SCALE_FACTOR, new Size(WIDTH, HEIGHT), new Scalar(0), true, false); // size = (1, 4, 416, 416)

        this.network.setInput(blob);


        ArrayList<Mat> layerOutputs = new ArrayList<>();
        List<String> layersNames = this.network.getUnconnectedOutLayersNames();

        Long start = System.currentTimeMillis();
        this.network.forward(layerOutputs, layersNames);
        Long finish = System.currentTimeMillis();
        Log.i(TAG, "_processSSD: Total time: " + ((finish - start)/1000.0) + " seconds");
    }
}
