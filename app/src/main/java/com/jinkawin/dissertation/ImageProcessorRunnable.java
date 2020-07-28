package com.jinkawin.dissertation;

import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class ImageProcessorRunnable implements Runnable{
    private static final String TAG = "ImageProcessorRunnable";

    public TaskProcessorMethods taskProcessorMethods;

    public ImageProcessorRunnable(TaskProcessorMethods taskProcessorMethods){
        this.taskProcessorMethods = taskProcessorMethods;
    }
    
    
    @Override
    public void run() {
//        Log.i(TAG, "run: index " + taskProcessorMethods.getIndex() + " is started");
        taskProcessorMethods.setThread(Thread.currentThread());

        // Moves the current Thread into the background
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

        // Initial ImagePrcessor
        ImageProcessor imageProcessor = new ImageProcessor(
                taskProcessorMethods.getContext(),
                taskProcessorMethods.getWeightPath(),
                taskProcessorMethods.getConfigPath(),
                taskProcessorMethods.getModel());

        // Get target frame
        Mat frame = taskProcessorMethods.getFrame();

        // Resize image
        Imgproc.resize(frame, frame, taskProcessorMethods.getSize());

        frame = imageProcessor.process(frame);

        taskProcessorMethods.setFrame(frame);

        // Send information back to the manager
        taskProcessorMethods.handleProcessState(ProcessStatus.SUCCESS);

//        Log.i(TAG, "run: index " + taskProcessorMethods.getIndex() + " is finished");

        // Free thread storage
//        taskProcessorMethods.setThread(null);
    }
}
