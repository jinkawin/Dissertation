package com.jinkawin.dissertation;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class ImageProcessorRunnable implements Runnable{
    private static final String TAG = "ImageProcessorRunnable";

    public static final int WIDTH = 480;

    public TaskProcessorMethods taskProcessorMethods;

    public ImageProcessorRunnable(TaskProcessorMethods taskProcessorMethods){
        this.taskProcessorMethods = taskProcessorMethods;
    }
    
    
    @Override
    public void run() {
//        taskProcessorMethods.setThread(Thread.currentThread());
//
//        // Moves the current Thread into the background
//        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
//
//        // Initial ImagePrcessor
//        ImageProcessor imageProcessor = new ImageProcessor(taskProcessorMethods.getContext(), taskProcessorMethods.getWeightPath(), taskProcessorMethods.getConfigPath());
//
//        // Get target frame
//        Mat frame = taskProcessorMethods.getFrame();
//
//        // Resize image
//        Imgproc.resize(frame, frame, taskProcessorMethods.getSize());
//
//        // Convert rgba to rgb
//        Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGBA2RGB);
//        frame = imageProcessor.process(frame);
//
//        taskProcessorMethods.setFrame(frame);
//
//        // Send information back to the manager
//        taskProcessorMethods.handleProcessState(ProcessStatus.SUCCESS);
//
//        // Free thread storage
////        taskProcessorMethods.setThread(null);
    }
}
