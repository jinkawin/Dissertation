package com.jinkawin.dissertation;

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

    public int getX(){ return this.x; }
    public int getY(){ return this.y; }
    public int getCentreX() { return centreX; }
    public int getCentreY() { return centreY; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public Rect2d getRect2d(){
        return new Rect2d(this.x, this.y, this.width, this.height);
    }

    public void setCentreX(double centreX) {
        this.centreX = (int)(centreX * frame.getWidth());
        this.x = (int)(this.centreX - (this.width / 2));
    }

    public void setCentreY(double centreY) {
        this.centreY = (int)(centreY * frame.getHeight());
        this.y = (int)(this.centreY - (this.height / 2));
    }

    public void setWidth(double width) {
        this.width = (int)(width * frame.getWidth());
    }

    public void setHeight(double height) {
        this.height = (int)(height * frame.getHeight());
    }
}
