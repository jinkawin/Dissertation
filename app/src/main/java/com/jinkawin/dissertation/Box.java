package com.jinkawin.dissertation;

import android.util.Log;

import androidx.annotation.NonNull;

import org.opencv.core.Point;
import org.opencv.core.Rect2d;

public class Box {

    private static final String TAG = "Box";

    private int x;
    private int y;
    private int centreX;
    private int centreY;
    private int width;
    private int height;

    public Box(){

    }

    public Box(int centreX, int centreY, int width, int height){
        this.centreX = centreX;
        this.centreY = centreY;
        this.width = width;
        this.height = height;

        this.x = (int)(this.centreX - (this.width / 2.0));
        this.y = (int)(this.centreY - (this.height / 2.0));
    }

    public int getX(){ return this.x; }
    public int getY(){ return this.y; }
    public int getWidth() { return this.width; }
    public int getHeight() { return this.height; }
    public Point getPoint(){ return new Point(this.centreX, this.centreY); }

    public Rect2d getRect2d(){
        return new Rect2d(this.centreX, this.centreY, this.width, this.height);
    }

    @NonNull
    @Override
    public String toString() {
        return "x: " + this.x + ", y: " + this.y + ", centre x: " + this.centreX + ", centre y: " + this.centreY + ", width: " + this.width + ", height: " + this.height;
    }
}
