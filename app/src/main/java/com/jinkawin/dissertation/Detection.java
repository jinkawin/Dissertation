package com.jinkawin.dissertation;

import org.opencv.core.MatOfInt;

import java.util.ArrayList;

public class Detection {

    private MatOfInt indices;
    private ArrayList<Box> boxes;

    public Detection(){
        this.indices = new MatOfInt();
        this.boxes = new ArrayList<>();
    }

    public Detection(MatOfInt indices, ArrayList<Box> boxes){
        this.indices = indices;
        this.boxes = boxes;
    }

    public MatOfInt getIndices() {
        return indices;
    }

    public void setIndices(MatOfInt indices) {
        this.indices = indices;
    }

    public ArrayList<Box> getBoxes() {
        return boxes;
    }

    public void setBoxes(ArrayList<Box> boxes) {
        this.boxes = boxes;
    }
}
