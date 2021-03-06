package com.jinkawin.dissertation;

import org.opencv.core.Point;

public class SocialDistanceDetection {

    public double _calculateDistance(Point a, Point b){
        return Math.pow((Math.pow((a.x - b.x), 2.0) + ((550 / ((a.y + b.y) / 2.0)) * Math.pow((a.y - b.y), 2.0))), 0.5);
    }

    public double _calculateCalibration(Point a, Point b){
        return (a.y + b.y) / 2.0;
    }

    public boolean _checkCondition(Double distance, Double calibration){
        if(0 < distance && distance < (0.25 * calibration))
            return true;
        else
            return false;
    }

    public boolean checkDistance(Point a, Point b){
        double distance = this._calculateDistance(a, b);
        double calibration = this._calculateCalibration(a, b);

        return this._checkCondition(distance, calibration);
    }
}
