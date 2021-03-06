package com.jinkawin.dissertation;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Size;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ImageProcessorManager {

    private static final String TAG = "ImageProcessorManager";

    // Get number of avialble CPU's cores
    private static int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
    private static int MAXIMUM_CORES = Runtime.getRuntime().availableProcessors();

    private static int STREAM_FPS = 30;

    // Sets the amount of time an idle thread will wait for a task before terminating
    private static final int KEEP_ALIVE_TIME = 1;

    // Sets the Time Unit to seconds
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT;

    // A queue of Runnables for the image processor
    private final BlockingQueue<Runnable> processorQueue;

    // A queue of Runnables for the task processor
    private final Queue<ImageProcessorTask> taskQueue;

    // Threads pool
    private final ThreadPoolExecutor processorThreadPool;

    // For singleton pattern
    private static ImageProcessorManager instance;

    private static Context context;
    private static String weightPath;
    private static String configPath;
    private static boolean isStream;

    public static int inputCount;
    public static ArrayList<Result> results;
    public static PriorityQueue<Result> streamResult;
    public static ArrayList<DetectionLog> detectionLogs;

    // Message manager
    private Handler handler;

    // Static block will be executed only once
    static{
        // Get time unit
        KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;

        // Initial single instance
        instance = new ImageProcessorManager();

        results = new ArrayList<Result>();
        streamResult = new PriorityQueue<Result>(STREAM_FPS, new ResultComparator());
        detectionLogs = new ArrayList<>();

        inputCount = 0;
    }

    /**
     * Constructor for pre-recorded video/image
     */
    private ImageProcessorManager(){
        isStream = false;

        // Initial Queue
        this.processorQueue = new LinkedBlockingQueue<Runnable>();
        this.taskQueue = new LinkedBlockingQueue<ImageProcessorTask>();

        // Create threads pool
        this.processorThreadPool = new ThreadPoolExecutor(NUMBER_OF_CORES, MAXIMUM_CORES, KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT, this.processorQueue);

        handler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {
                ImageProcessorTask processorTask = (ImageProcessorTask) msg.obj;

                switch (ProcessStatus.intToEnum(msg.what)){
                    case SUCCESS:
                        if(isStream){ // Live stream from camera
                            streamResult.add(new Result(processorTask.getFrame(), processorTask.getIndex()));
                            detectionLogs.clear();
                            detectionLogs.addAll(processorTask.getDetectionLogs());
                        }else { // pre-recorded video
                            results.add(new Result(processorTask.getFrame(), processorTask.getIndex()));
                            noticeUpdate();

                            if (results.size() == inputCount) {
                                noticeMainActivity();
                            }
                        }

                        break;
                    default:
                        Log.i(TAG, "handleMessage: default");
                        break;
                }

                // Recycle task for reusing
                recycleTask(processorTask);
            }
        };
    }

    public static ImageProcessorTask process(Mat frame, Size size, int index, ModelType model){
        if(!isStream) inputCount++;

        // Try to get and deque the queue
        ImageProcessorTask processorTask = instance.taskQueue.poll();

        // If queue is empty, it will return null
        if(processorTask == null){
            processorTask = new ImageProcessorTask();
        }

        // Intial task
        processorTask.initTask(context, frame, size, index, weightPath, configPath, model, isStream);

        // Start processing image
        instance.processorThreadPool.execute(processorTask.getImageProcessorRunnable());

        return processorTask;
    }

    // Send the message back to handler
    public void handleState(ImageProcessorTask processorTask ,ProcessStatus status){
        handler.obtainMessage(status.getValue(), processorTask).sendToTarget();
    }

    public static ImageProcessorManager getInstance(){
        return instance;
    }

    // Send results back to the main activity by broadcasting
    private void noticeMainActivity(){
        if(isStream){

        }else{
            Intent intent = new Intent();
            intent.setAction(MainActivity.ProcessorBroadcastReceiver.ACTION);
            intent.putExtra("data", ProcessStatus.FINISH);
            context.sendBroadcast(intent);
        }

    }

    private void noticeUpdate(){
        Intent intent = new Intent();
        intent.setAction(MainActivity.ProcessorBroadcastReceiver.ACTION);
        intent.putExtra("data", ProcessStatus.SUCCESS);
        context.sendBroadcast(intent);
    }

    public static void setProcessor(Context ct, String _weightPath, String _configPath){
        context = ct;
        weightPath = _weightPath;
        configPath = _configPath;

        isStream = false;
    }

    public static void setProcessor(Context ct, String _weightPath, String _configPath, boolean isStream){
        context = ct;
        weightPath = _weightPath;
        configPath = _configPath;
        ImageProcessorManager.isStream = isStream;
    }

    public static ArrayList<Result> getResults(){
        return results;
    }

    public void recycleTask(ImageProcessorTask task) {

        // Frees up memory in the task
        task.recycle();

        // Puts the task object back into the queue for re-use.
        taskQueue.offer(task);
    }
}
