package com.jinkawin.dissertation;

import org.opencv.core.Mat;

public class ImageProcessorRunnable implements Runnable{
    private static final String TAG = "ImageProcessorRunnable";

    public TaskProcessorMethods taskProcessorMethods;

    public ImageProcessorRunnable(TaskProcessorMethods taskProcessorMethods){
        this.taskProcessorMethods = taskProcessorMethods;
    }
    
    
    @Override
    public void run() {
        taskProcessorMethods.setThread(Thread.currentThread());

        // Moves the current Thread into the background
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

        // Initial ImagePrcessor
        ImageProcessor imageProcessor = new ImageProcessor(
                taskProcessorMethods.getContext(),
                taskProcessorMethods.getWeightPath(),
                taskProcessorMethods.getConfigPath(),
                taskProcessorMethods.getModel(),
                taskProcessorMethods.isLiveStream());

        // Get target frame
        Mat frame = taskProcessorMethods.getFrame();

        frame = imageProcessor.process(frame);

        taskProcessorMethods.setFrame(frame);

        // Get list of detection
        taskProcessorMethods.setDetectionLogs(imageProcessor.getDetectionLog());

        // Send information back to the manager
        taskProcessorMethods.handleProcessState(ProcessStatus.SUCCESS);
    }
}
