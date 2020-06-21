package com.jinkawin.dissertation;

public class SocialDistanceDetection {

    public double _calculateDistance(Centre a, Centre b){
        return Math.pow((Math.pow((a.getX() - b.getX()), 2.0) + ((550 / ((a.getY() + b.getY()) / 2.0)) * Math.pow((a.getY() - b.getY()), 2.0))), 0.5);
    }

    public double _calculateCalibration(Centre a, Centre b){
        return (a.getY() + b.getY()) / 2.0;
    }

    public boolean _checkCondition(Double distance, Double calibration){
        if(0 < distance && distance < (0.25 * calibration))
            return true;
        else
            return false;
    }

    public boolean checkDistance(Centre a, Centre b){
        double distance = this._calculateDistance(a, b);
        double calibration = this._calculateCalibration(a, b);

        return this._checkCondition(distance, calibration);
    }
}
