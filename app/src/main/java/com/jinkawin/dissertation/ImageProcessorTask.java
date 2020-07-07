package com.jinkawin.dissertation;

import android.content.Context;

import org.opencv.core.Mat;
import org.opencv.core.Size;

public class ImageProcessorTask implements TaskProcessorMethods {

    // For singleton pattern
    private static ImageProcessorManager instance;

    private Thread currentThread;
    private ImageProcessorRunnable imageProcessorRunnable;

    private static Context context;
    private Mat frame;
    private int index = -1;
    private Size size;

    public ImageProcessorTask(){
        instance = ImageProcessorManager.getInstance();
        imageProcessorRunnable = new ImageProcessorRunnable(this);
    }

    public void initTask(Context context, Mat frame, Size newSize, int index){
        this.context = context;
        this.frame = frame;
        this.size = newSize;
        this.index = index;
    }

    /**
     * The reference of the thread can be changed by other processes (outside the app)
     * Thus, the method should lock on the static field, which is instance (ImageProcessorManager)
     *
     * @return Thread the current thread
     */
    public Thread getCurrentThread(){
        synchronized (instance){
            return this.currentThread;
        }
    }

    public ImageProcessorRunnable getImageProcessorRunnable() {
        return imageProcessorRunnable;
    }

    @Override
    public void setThread(Thread thread) {
        synchronized (instance){
            this.currentThread = thread;
        }
    }

    @Override
    // Send the message back to the object in the thread pool
    public void handleProcessState(ProcessStatus status, Result result) {
        instance.handleState(status, result);
    }

    @Override
    public Context getContext() {
        return this.context;
    }

    @Override
    public Mat getFrame() {
        return this.frame;
    }

    @Override
    public int getIndex() {
        return this.index;
    }

    @Override
    public Size getSize() {
        return this.size;
    }

}
