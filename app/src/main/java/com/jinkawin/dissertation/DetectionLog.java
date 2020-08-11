package com.jinkawin.dissertation;

import org.opencv.core.Rect;
import org.opencv.core.Scalar;

public class DetectionLog {
    Rect detectedRect;
    Scalar colour;

    public DetectionLog(Rect detectedRect, Scalar colour){
        this.detectedRect = detectedRect;
        this.colour = colour;
    }

    public Rect getDetectedRect() {
        return detectedRect;
    }

    public Scalar getColour() {
        return colour;
    }
}
