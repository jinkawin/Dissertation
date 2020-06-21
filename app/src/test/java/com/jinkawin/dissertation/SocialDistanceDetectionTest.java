package com.jinkawin.dissertation;

import org.junit.Test;
import static org.junit.Assert.*;

public class SocialDistanceDetectionTest {
    private SocialDistanceDetection sdd = new SocialDistanceDetection();

//    Distance
    @Test
    public void calculateDistance_Case1(){
        Centre a = new Centre(337, 13);
        Centre b = new Centre(337, 13);
        assertEquals(sdd._calculateDistance(a, b), 0.0, 0.0);
    }

    @Test
    public void calculateDistance_Case2(){
        Centre a = new Centre(337, 13);
        Centre b = new Centre(246, 16);
        assertEquals(sdd._calculateDistance(a, b), 92.8567677142858, 0.0);
    }

    @Test
    public void calculateDistance_Case3(){
        Centre a = new Centre(337, 13);
        Centre b = new Centre(296, 6);
        assertEquals(sdd._calculateDistance(a, b), 67.21489496579727, 0.0);
    }

    @Test
    public void calculateDistance_Case4(){
        Centre a = new Centre(296, 6);
        Centre b = new Centre(71, 99);
        assertEquals(sdd._calculateDistance(a, b), 375.81055257745413, 0.0);
    }

    @Test
    public void calculateDistance_Case5(){
        Centre a = new Centre(296, 6);
        Centre b = new Centre(328, 13);
        assertEquals(sdd._calculateDistance(a, b), 62.1356749803457, 0.0);
    }

//    Calibration
    @Test
    public void calculateCalibration_Case1(){
        Centre a = new Centre(337, 13);
        Centre b = new Centre(337, 13);
        assertEquals(sdd._calculateCalibration(a, b), 13.0, 0.0);
    }

    @Test
    public void calculateCalibration_Case2(){
        Centre a = new Centre(337, 13);
        Centre b = new Centre(246, 16);
        assertEquals(sdd._calculateCalibration(a, b), 14.5, 0.0);
    }

    @Test
    public void calculateCalibration_Case3(){
        Centre a = new Centre(337, 13);
        Centre b = new Centre(296, 6);
        assertEquals(sdd._calculateCalibration(a, b), 9.5, 0.0);
    }

    @Test
    public void calculateCalibration_Case4(){
        Centre a = new Centre(296, 6);
        Centre b = new Centre(71, 99);
        assertEquals(sdd._calculateCalibration(a, b), 52.5, 0.0);
    }

    @Test
    public void calculateCalibration_Case5(){
        Centre a = new Centre(296, 6);
        Centre b = new Centre(328, 13);
        assertEquals(sdd._calculateCalibration(a, b), 9.5, 0.0);
    }

//    Check Condition
    @Test
    public void checkCondition_Case1(){
        double distance = 0.0;
        double calibration = 13.0;
        assertFalse(sdd._checkCondition(distance, calibration));
    }

    @Test
    public void checkCondition_Case2(){
        double distance = 270.0853686947494;
        double calibration = 94.0;
        assertFalse(sdd._checkCondition(distance, calibration));
    }

    @Test
    public void checkCondition_Case3(){
        double distance = 18.26586913641511;
        double calibration = 81.0;
        assertTrue(sdd._checkCondition(distance, calibration));
    }

    @Test
    public void checkCondition_Case4(){
        double distance = 31.04020507447977;
        double calibration = 220.5;
        assertTrue(sdd._checkCondition(distance, calibration));
    }

    @Test
    public void checkCondition_Case5(){
        double distance = 23.06920904142796;
        double calibration = 172.5;
        assertTrue(sdd._checkCondition(distance, calibration));
    }
}
