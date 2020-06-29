package com.jinkawin.dissertation;

import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;

import java.util.ArrayList;

public class Detection {

    private MatOfInt indices;
    private ArrayList<Box> boxes;
    private Mat frame;


    public Detection(){
        this.indices = new MatOfInt();
        this.boxes = new ArrayList<>();
        this.frame = new Mat();
    }

    public Detection(MatOfInt indices, ArrayList<Box> boxes, Mat frame){
        this.indices = indices;
        this.boxes = boxes;
        this.frame = frame;
    }

    public MatOfInt getIndices() {
        return indices;
    }
    public ArrayList<Box> getBoxes() {
        return boxes;
    }
    public Mat getFrame() { return this.frame; }

    public void setIndices(MatOfInt indices) { this.indices = indices; }
    public void setBoxes(ArrayList<Box> boxes) {
        this.boxes = boxes;
    }
    public void setFrame(Mat frame) { this.frame = frame; }
}
