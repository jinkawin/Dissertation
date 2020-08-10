package com.jinkawin.dissertation;

import android.content.Context;
import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfRect2d;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Rect2d;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.opencv.dnn.Dnn.*;

public class ImageProcessor {

    public static final String TAG = "ImageProcessor";
    private static final String PATH = "android.resource://com.jinkawin.dissertation/raw/";

    private static final String PERSON = "person";

    private static final double WIDTH = 416.0;
    private static final double HEIGHT = 416.0;
    private static final double SCALE_FACTOR = 1.0/255.0;
    private static final double CONFIDENCE_THRESHOLD_YOLO = 0.5;
    private static final double CONFIDENCE_THRESHOLD_SSD = 0.3;
    private static final float SCORE_THRESHOLD_YOLO = 0.5f;
    private static final float SCORE_THRESHOLD_SSD = 0.3f;
    private static final float NMS_THRESHOLD = 0.3f;
    private static final double MEAN_VAL = 127.5;

    private static final Scalar COLOUR_WHITE = new Scalar(255, 255, 255);
    private static final Scalar COLOUR_GREEN = new Scalar(0, 255, 0);
    private static final Scalar COLOUR_RED = new Scalar(255, 0, 0);

    private Context context;

    private Net network;
    private List<String> labels;

    private ModelType model;

    /* --------- Setup --------- */
    public ImageProcessor(Context context, String weightPath, String configUri, ModelType model){
        this.context = context;
        this.model = model;

        // Initial network
        switch (model){
            case SSD:
                _setupCaffe_SSD(weightPath, configUri);
                break;
            case YOLO:
                _setupDarnet_Yolo3(weightPath, configUri);
                break;
            default:
                this.model = ModelType.SSD;
                _setupCaffe_SSD(weightPath, configUri);
        }
    }

    private void _setupCaffe_SSD(String weightPath, String configUri){
        this.labels = this._readLabels(R.raw.ssd_names);

        // https://docs.opencv.org/3.4/d6/d0f/group__dnn.html#ga186f7d9bfacac8b0ff2e26e2eab02625
        this.network = Dnn.readNetFromCaffe(configUri, weightPath);
//        this.network.setPreferableBackend(DNN_BACKEND_OPENCV);
//        this.network.setPreferableTarget(DNN_TARGET_OPENCL);
    }

    private void _setupDarnet_Yolo3(String weightPath, String configUri){
        this.labels = this._readLabels(R.raw.coco_names);

        // https://docs.opencv.org/3.4/d6/d0f/group__dnn.html#ga186f7d9bfacac8b0ff2e26e2eab02625
        this.network = Dnn.readNetFromDarknet(configUri, weightPath);
        this.network.setPreferableBackend(DNN_BACKEND_OPENCV);
        this.network.setPreferableTarget(DNN_TARGET_OPENCL_FP16);
    }


    /* --------- Process --------- */
    public Mat process(Mat frame){

        Detection detection = new Detection();

        switch (this.model){
            case SSD:
                // Convert rgba to bgr
                Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGBA2BGR);

                detection = this._processSSD(frame);
                break;
            case YOLO:
                // Convert rgba to rgb
                Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGBA2RGB);

                detection = this._processYOLO(frame);
                break;
        }

        detection = this.determineDistance(detection);

        return detection.getFrame();
    }

    public Detection determineDistance(Detection detection){
        SocialDistanceDetection sdd = new SocialDistanceDetection();
        ArrayList<Box> nmsBoxes = detection.getBoxes();
        Mat frame = detection.getFrame();

        // Set all statuses to false
        Boolean[] statuses = new Boolean[nmsBoxes.size()];
        Arrays.fill(statuses, false);

        int nmsBoxSize = nmsBoxes.size();
        // Check distance between coupled object
        for (int i=0; i<nmsBoxSize; i++){
            for (int j=i+1; j<nmsBoxSize; j++){
                Boolean status = sdd.checkDistance(nmsBoxes.get(i).getPoint(), nmsBoxes.get(j).getPoint());
                statuses[i] |= status;
                statuses[j] |= status;
            }
        }

        // Draw box over detected object
        int statusSize = statuses.length;
        for(int i=0; i<statusSize; i++){
            Scalar colour = statuses[i]?COLOUR_RED:COLOUR_GREEN;

            Imgproc.rectangle(
                    frame,
                    new Rect(nmsBoxes.get(i).getX(), nmsBoxes.get(i).getY(), nmsBoxes.get(i).getWidth(), nmsBoxes.get(i).getHeight()),
                    colour
            );

            /* TODO: Add line */
        }
        detection.setFrame(frame);

        return detection;

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
//            Log.i(TAG, "Cannot read classification file");
        }

        return labels;
    }


    /* --------- YOLO --------- */
    /**
     *
     * @param frame a picture in Mat format
     *
     * Read Dnn.blobFromImage: https://www.pyimagesearch.com/2017/11/06/deep-learning-opencvs-blobfromimage-works/
     */
    private Detection _processYOLO(Mat frame){

        // Initail variables
        ArrayList<Mat> layerOutputs = new ArrayList<>();
        List<String> layersNames = this.network.getUnconnectedOutLayersNames();

        // Process
        Mat blob = Dnn.blobFromImage(frame, SCALE_FACTOR, new Size(WIDTH, HEIGHT), new Scalar(0), true, false); // size = (1, 4, 416, 416)

        this.network.setInput(blob);

        Long start = System.currentTimeMillis();
        this.network.forward(layerOutputs, layersNames);

        Long finish = System.currentTimeMillis();
//        Log.i(TAG, "_processSSD: Total time: " + ((finish - start)/1000.0) + " seconds");

        return this._detectPerson_Yolo(layerOutputs, frame, false);
    }

    /**
     *
     * @param layerOutput               layer output of dnn
     * @param frame                     a picture in Mat format
     * @param isDrawEveryDetectedObj    draw white regtangle for every detected object (before nmsbox)
     *
     * YOLO3 Model Output:
     *      0       = Centre X
     *      1       = Centre Y
     *      2       = Width
     *      3       = Height
     *      4       = Confidence
     *      5-84    = Class Confidence
     *
     */
    private Detection _detectPerson_Yolo(ArrayList<Mat> layerOutput, Mat frame, boolean isDrawEveryDetectedObj) {
        ArrayList<Box> boxes = new ArrayList<>();
        ArrayList<Rect2d> outline = new ArrayList<>();
        ArrayList<Float> confidences = new ArrayList<>();

        MatOfRect2d matOfRect2d = new MatOfRect2d();
        MatOfFloat matOfFloat = new MatOfFloat();
        MatOfInt indices = new MatOfInt();

        // For each layer output
        for (int layer = 0; layer < layerOutput.size(); layer++) {
            Mat detection = layerOutput.get(layer);

            for (int row = 0; row < detection.rows(); row++) {
                ArrayList<Double> scores = new ArrayList<>();

                // Get score[5:] (Get confidence of all classes)
                for (int col=5; col<detection.cols(); col++){
                    scores.add(detection.get(row, col)[0]);
                }

                // Get the highest probability class
                double highestProb = Collections.max(scores);
                int mostProbIndex = scores.indexOf(highestProb);

                // If person is detected
                if(this.labels.get(mostProbIndex).equals(PERSON) && highestProb > CONFIDENCE_THRESHOLD_YOLO){

                    // Initial box over detected object
                    Box box = new Box(
                            (int)(detection.get(row, 0)[0]  * frame.size().width),
                            (int)(detection.get(row, 1)[0] * frame.size().height),
                            (int)(detection.get(row, 2)[0] * frame.size().width),
                            (int)(detection.get(row, 3)[0] * frame.size().height)
                    );

                    boxes.add(box);
                    outline.add(box.getRect2d());
                    confidences.add((float)highestProb);

                    if(isDrawEveryDetectedObj) {
                        // Draw box over detected object
                        Imgproc.rectangle(
                                frame,
                                new Rect(box.getX(), box.getY(), box.getWidth(), box.getHeight()),
                                COLOUR_WHITE
                        );
                    }
                }
            } // for row
        } // for layer

        // Convert Arraylist to Mat
        matOfRect2d.fromList(outline);
        matOfFloat.fromList(confidences);

        Dnn.NMSBoxes(matOfRect2d, matOfFloat, SCORE_THRESHOLD_YOLO, NMS_THRESHOLD, indices);

        ArrayList<Box> nmsBoxes = new ArrayList<>();

        // Filter only outline that is consisted in indices
        if(indices.size().height > 0) { // If there is at lease 1 index
            for (int index : indices.toList()) {
                nmsBoxes.add(boxes.get(index));
            }
        }

        return new Detection(indices, nmsBoxes, frame);
    }


    /* --------- SSD --------- */
    private Detection _processSSD(Mat frame){

        // Process
//        Mat blob = Dnn.blobFromImage(frame, SCALE_FACTOR, new Size(WIDTH, HEIGHT), new Scalar(MEAN_VAL, MEAN_VAL, MEAN_VAL), false, false); // size = (1, 4, 416, 416)
        Mat blob = Dnn.blobFromImage(frame, SCALE_FACTOR); // Faster

        this.network.setInput(blob);

        Long start = System.currentTimeMillis();
        Mat detection = this.network.forward();

        Long finish = System.currentTimeMillis();
        Log.i(TAG, "_processSSD: Total time: " + ((finish - start)/1000.0) + " seconds");

        return this._detectPerson_SSD(detection, frame);

    }

    private Detection _detectPerson_SSD(Mat detection, Mat frame){
        ArrayList<Box> boxes = new ArrayList<>();
        ArrayList<Rect2d> outline = new ArrayList<>();
        ArrayList<Float> confidences = new ArrayList<>();

        MatOfRect2d matOfRect2d = new MatOfRect2d();
        MatOfFloat matOfFloat = new MatOfFloat();
        MatOfInt indices = new MatOfInt();

        int cols = frame.cols();
        int rows = frame.rows();

        detection = detection.reshape(1, (int)detection.total() / 7);

        for (int i = 0; i < detection.rows(); i++) {
            double confidence = detection.get(i, 2)[0];
            int classId = (int)detection.get(i, 1)[0];

            // If person is detected
            if(this.labels.get(classId).equals(PERSON) && confidence > CONFIDENCE_THRESHOLD_SSD){
                int left   = (int)(detection.get(i, 3)[0] * cols);
                int top    = (int)(detection.get(i, 4)[0] * rows);
                int right  = (int)(detection.get(i, 5)[0] * cols);
                int bottom = (int)(detection.get(i, 6)[0] * rows);

                Rect _rect = new Rect(new Point(left, top), new Point(right, bottom));

                // Initial box over detected object
                Box box = new Box(
                        _rect.x + (_rect.width/2), // Centre X
                        _rect.y + (_rect.height/2), // Centre Y
                        _rect.width,
                        _rect.height
                );

                boxes.add(box);
                outline.add(box.getRect2d());
                confidences.add((float)confidence);
            }
        }

        // Convert Arraylist to Mat
        matOfRect2d.fromList(outline);
        matOfFloat.fromList(confidences);

        Dnn.NMSBoxes(matOfRect2d, matOfFloat, SCORE_THRESHOLD_SSD, NMS_THRESHOLD, indices);

        ArrayList<Box> nmsBoxes = new ArrayList<>();

        // Filter only outline that is consisted in indices
        if(indices.size().height > 0) { // If there is at lease 1 index
            for (int index : indices.toList()) {
                nmsBoxes.add(boxes.get(index));
            }
        }

        return new Detection(indices, nmsBoxes, frame);
    }
}
