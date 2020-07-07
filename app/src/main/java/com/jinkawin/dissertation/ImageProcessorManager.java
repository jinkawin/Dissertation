package com.jinkawin.dissertation;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import org.opencv.core.Mat;
import org.opencv.core.Size;

import java.util.ArrayList;
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

    private static ArrayList<Result> results;
    private static Context context;
    private static int count;
    private static ImageProcessor imageProcessor;
    private static String weightPath;
    private static String configPath;

    // Message manager
    private Handler handler;

    // Static block will be executed only once
    static{
        // Get time unit
        KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;

        // Initial single instance
        instance = new ImageProcessorManager();

        results = new ArrayList<Result>();

        count = 0;
    }


    private ImageProcessorManager(){
        Log.i(TAG, "ImageProcessorManager: Constructor created");

        // Initial Queue
        this.processorQueue = new LinkedBlockingQueue<Runnable>();
        this.taskQueue = new LinkedBlockingQueue<ImageProcessorTask>();

        // Create threads pool
        this.processorThreadPool = new ThreadPoolExecutor(NUMBER_OF_CORES, MAXIMUM_CORES, KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT, this.processorQueue);

        handler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {
                Result _result = (Result) msg.obj;

                if(_result.getStatus() == ResponseType.DATA) {
                    Log.i(TAG, "handleMessage: " + msg.what + "| Result index: " + _result.getIndex());
                }

                switch (ProcessStatus.intToEnum(msg.what)){
                    case SUCCESS:
                        if(_result.getIndex() > -1) {
                            results.add(_result);
                            Log.i(TAG, "handleMessage: Result is added with size: " + results.size());
                        }
                    default:
                        Log.i(TAG, "handleMessage: default");
                }

                if(results.size() == count){
                    Log.i(TAG, "handleMessage: yes");
                    noticeMainActivity();
                }else{
                    Log.i(TAG, "handleMessage: no");
                }

            }
        };
    }

    public static ImageProcessorTask process(Mat frame, Size size, int index){
        Log.i(TAG, "process: Start process index:  " + index);
        count++;
        Log.i(TAG, "process: Count: " + count);

        // Try to get and deque the queue
        ImageProcessorTask processorTask = instance.taskQueue.poll();

        // If queue is empty, it will return null
        if(processorTask == null){
            processorTask = new ImageProcessorTask();
        }

        // Intial task
        processorTask.initTask(context, frame, size, index, weightPath, configPath);

        // Start processing image
        instance.processorThreadPool.execute(processorTask.getImageProcessorRunnable());

        // Send message that task are done
        instance.handleState(ProcessStatus.SUCCESS, new Result(ResponseType.FINISH));

        return processorTask;
    }

    // Send the message back to handler
    public void handleState(ProcessStatus status, Result result){
        handler.obtainMessage(status.getValue(), result).sendToTarget();
    }

    public static ImageProcessorManager getInstance(){
        return instance;
    }

    // Send results back to the main activity by broadcasting
    private void noticeMainActivity(){
        Log.i(TAG, "noticeMainActivity: Noticing...");
        Intent intent = new Intent();
        intent.setAction(MainActivity.ProcessorBroadcastReceiver.ACTION);
        intent.putExtra("data", results);
        context.sendBroadcast(intent);
    }

    public static void setProcessor(Context ct, String _weightPath, String _configPath){
        context = ct;
        weightPath = _weightPath;
        configPath = _configPath;
    }
}
