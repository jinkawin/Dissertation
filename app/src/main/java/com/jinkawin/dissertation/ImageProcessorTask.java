package com.jinkawin.dissertation;

import android.content.Context;

import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Size;

import java.util.ArrayList;
import java.util.List;

public class ImageProcessorTask implements TaskProcessorMethods {
    private static final String TAG = "ImageProcessorTask";

    // For singleton pattern
    private static ImageProcessorManager instance;

    private Thread currentThread;
    private ImageProcessorRunnable imageProcessorRunnable;

    private static Context context;
    private Mat frame;
    private int index = -1;
    private Size size;
    private String weightPath;
    private String configPath;
    private ModelType model;
    private boolean isLiveStream;

    private ArrayList<DetectionLog> detectionLogs;

    public ImageProcessorTask(){
        instance = ImageProcessorManager.getInstance();
        imageProcessorRunnable = new ImageProcessorRunnable(this);
    }

    public void initTask(Context context, Mat frame, Size newSize, int index, String weightPath, String configPath, ModelType model){
        this.context = context;
        this.frame = frame;
        this.size = newSize;
        this.index = index;
        this.model = model;
        this.isLiveStream = false;

        this.weightPath = weightPath;
        this.configPath = configPath;

        detectionLogs = new ArrayList<>();
    }

    public void initTask(Context context, Mat frame, Size newSize, int index, String weightPath, String configPath, ModelType model, boolean isLiveStream){
        this.context = context;
        this.frame = frame;
        this.size = newSize;
        this.index = index;
        this.model = model;
        this.isLiveStream = isLiveStream;

        this.weightPath = weightPath;
        this.configPath = configPath;

        detectionLogs = new ArrayList<>();
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

    void recycle() {

        this.frame = null;
        this.size = null;
        this.index = -1;

        this.weightPath = null;
        this.configPath = null;

        this.detectionLogs.clear();
    }

    @Override
    public void setThread(Thread thread) {
        synchronized (instance){
            this.currentThread = thread;
        }
    }

    @Override
    // Send the message back to the object in the thread pool
    public void handleProcessState(ProcessStatus status) {
        instance.handleState(this, status);
    }

    @Override
    public void setFrame(Mat frame) {
        this.frame = frame;
    }

    @Override
    public boolean isLiveStream() {
        return this.isLiveStream;
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

    @Override
    public String getWeightPath() {
        return this.weightPath;
    }

    @Override
    public String getConfigPath() {
        return this.configPath;
    }

    @Override
    public Context getContext() {
        return context;
    }

    @Override
    public ModelType getModel() {
        return this.model;
    }

    @Override
    public ArrayList<DetectionLog> getDetectionLogs() {
        return this.detectionLogs;
    }

    @Override
    public void setDetectionLogs(ArrayList<DetectionLog> detectionLogs) {
        this.detectionLogs = detectionLogs;
    }

}
