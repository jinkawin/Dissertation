package com.jinkawin.dissertation;

import org.opencv.core.Point;
import org.opencv.core.Rect2d;

public class Box {

    private Frame frame;

    private int x;
    private int y;
    private int centreX;
    private int centreY;
    private int width;
    private int height;

    public Box(Frame frame){ this.frame = frame; }

    public int getX(){ return (int)(this.centreX - (this.width / 2.0)); }
    public int getY(){ return (int)(this.centreY - (this.height / 2.0)); }
    public int getCentreX() { return centreX; }
    public int getCentreY() { return centreY; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public Point getPoint(){ return new Point(this.centreX, this.centreY); }

    public Rect2d getRect2d(){
        return new Rect2d(this.x, this.y, this.width, this.height);
    }

    public void setCentreX(double centreX) {
        this.centreX = (int)(centreX * frame.getWidth());
    }

    public void setCentreY(double centreY) {
        this.centreY = (int)(centreY * frame.getHeight());
    }

    public void setWidth(double width) {
        this.width = (int)(width * frame.getWidth());
    }

    public void setHeight(double height) {
        this.height = (int)(height * frame.getHeight());
    }
}
