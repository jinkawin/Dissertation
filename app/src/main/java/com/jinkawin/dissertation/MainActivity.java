package com.jinkawin.dissertation;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.jcodec.api.android.AndroidSequenceEncoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Rational;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.annotation.Native;
import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";
    private static final int MEDIA_PICKER = 0;
    public static final int WIDTH = 480;

    public ArrayList<Result> results = new ArrayList<Result>();

    public ProcessorBroadcastReceiver receiver;

    public ImageProcessor imageProcessor;
    public TestImageProcessor testImageProcessor;

    public String saveVideoPath;
    public String weightPath;
    public String configPath;

    public Long start;

    public ModelType modelType = ModelType.SSD;

    /**
     * Callback when OpenCV libraries are loaded.
     */
    private BaseLoaderCallback blCallback = new BaseLoaderCallback() {
        @Override
        public void onManagerConnected(int status) {
            if(status == LoaderCallbackInterface.SUCCESS){
                Log.i(TAG, "onManagerConnected: Setup OpenCV is done");
            }else{
                super.onManagerConnected(status);
            }
        }
    };

    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Uri uploadfileuri = data.getData();
        File file = new File(uploadfileuri.getPath());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        NativeLib.checkNeon();
        setup(this.modelType);

//        Log.i(TAG, "onCreate: " + Core.getBuildInformation());

//        Log.i(TAG, "onCreate: Native-Lib: " + NativeLib.helloWorld());
//        this.processNativeImage(R.raw.picturte_test, "picture_test.jpg");
//        this.processNativeParallelVideo(R.raw.video_test, "video_test.mp4");
//        this.processNativeVideo(R.raw.video_test, "video_test.mp4");

//        Button btnBrowser = findViewById(R.id.btnBrowser);
//        btnBrowser.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//                intent.setType("*/*");
//                intent.addCategory(Intent.CATEGORY_OPENABLE);
//                startActivityForResult(intent, MEDIA_PICKER);
//            }
//        });

        // It seems thread are run in sequencial
        // The second thread will be started when the first thread is finished.
        // And the second thread is not consistency run, the second thread is blocked (is switched).

//        processSingleFrameTest(R.raw.video_test, "video_test.mp4", "Dnn 1");
//        processSingleFrameTest(R.raw.video_test, "video_test.mp4", "Dnn 2");
//        processSingleFrameTest(R.raw.video_test, "video_test.mp4", "Dnn 3");
//        processNativeParallelVideo(R.raw.video_test, "video_test.mp4");
//        processVideo(R.raw.video_test, "video_test.mp4");

//        this.processVideo(R.raw.video_test, "video_test.mp4");
//        this.processSingleFrame(R.raw.picturte_test, "picture_test.jpg");
//        this.processImage(R.raw.picturte_test, "picture_test.jpg");
        this.processParallelVideo(R.raw.video_test, "video_test.mp4");
    }

    public void processNativeImage(int rId, String name){
        // Initial
        ImageReader imageReader = new ImageReader(this);

        // Read Image
        Mat image = imageReader.readImage(rId, name);
        Log.i(TAG, "processNativeImage");

        Log.i(TAG, "processNativeImage: Mat dims: " + image.dims());
        Log.i(TAG, "processNativeImage: Mat size: " + image.size());
        Log.i(TAG, "processNativeImage: Mat channels: " + image.channels());

        // Process by using Native lib
        NativeLib.process(image.getNativeObjAddr(), this.weightPath, this.configPath);

        // Save Image
//        Bitmap savedImage = Bitmap.createBitmap(image.width(), image.height(), Bitmap.Config.ARGB_8888);
//        Utils.matToBitmap(image, savedImage);
//        MediaStore.Images.Media.insertImage(getContentResolver(), savedImage, "title", "description");
    }

    public void processNativeVideo(int rId, String name){
        // Initial
        VideoManager videoManager = new VideoManager(this);

        // Init for saving video
        File targetFolder = this.getExternalMediaDirs()[0];
        SeekableByteChannel out = null;
        AndroidSequenceEncoder encoder = null;
        try {
            /* TODO: Change save path to Gallery */
            out = NIOUtils.writableFileChannel(targetFolder.getAbsolutePath() + "/" + System.currentTimeMillis() + ".mp4");
            encoder = new AndroidSequenceEncoder(out, Rational.R(30, 1));
        } catch (FileNotFoundException fe){
            Log.e(TAG, "saveVideo: " + fe.getMessage());
        } catch (IOException ioe){
            Log.e(TAG, "saveVideo: " + ioe.getMessage());
        }

        // Read Video from RAW Folder
        ArrayList<Mat> mats = videoManager.readVideo(rId, name);

        // Convert ArrayList of Mat to Array of address
        long[] addrs = new long[mats.size()];
        int i = 0;
        for(Mat mat: mats) {
            addrs[i] = mats.get(i++).getNativeObjAddr();
        }

        Log.i(TAG, "processNativeParallelVideo: First element: " + addrs[0]);
        Log.i(TAG, "processNativeParallelVideo: Second element: " + addrs[1]);

        Long start = System.currentTimeMillis();
        NativeLib.videoProcess(addrs, this.weightPath, this.configPath);
        Long finish = System.currentTimeMillis();
        Log.i(TAG, "Process time: " + ((finish - start)/1000.0) + " seconds");

        for (Mat mat:mats) {
            //Save frame to video
            try {
                Bitmap bitmap = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(mat, bitmap);
                encoder.encodeImage(bitmap);
            } catch (IOException e){
                Log.e(TAG, "encode: " + e.getMessage());
            }
        }

        try {
            encoder.finish();
        } catch (IOException e){
            Log.e(TAG, e.getMessage());
        }
        NIOUtils.closeQuietly(out);
        Log.i(TAG, "Video is saved!");
    }

    public void processNativeParallelVideo(int rId, String name){
        // Initial
        VideoManager videoManager = new VideoManager(this);

        // Init for saving video
        File targetFolder = this.getExternalMediaDirs()[0];
        SeekableByteChannel out = null;
        AndroidSequenceEncoder encoder = null;
        try {
            /* TODO: Change save path to Gallery */
            out = NIOUtils.writableFileChannel(targetFolder.getAbsolutePath() + "/" + System.currentTimeMillis() + ".mp4");
            encoder = new AndroidSequenceEncoder(out, Rational.R(30, 1));
        } catch (FileNotFoundException fe){
            Log.e(TAG, "saveVideo: " + fe.getMessage());
        } catch (IOException ioe){
            Log.e(TAG, "saveVideo: " + ioe.getMessage());
        }

        // Read Video from RAW Folder
        ArrayList<Mat> mats = videoManager.readVideo(rId, name);

        // Convert ArrayList of Mat to Array of address
        long[] addrs = new long[mats.size()];
        int i = 0;
        for(Mat mat: mats) {
            addrs[i] = mats.get(i++).getNativeObjAddr();
        }

        Long start = System.currentTimeMillis();
        NativeLib.parallelProcess(addrs, this.weightPath, this.configPath);
        Long finish = System.currentTimeMillis();
        Log.i(TAG, "Process time: " + ((finish - start)/1000.0) + " seconds");

        for (Mat mat:mats) {
            //Save frame to video
            try {
                Bitmap bitmap = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(mat, bitmap);
                encoder.encodeImage(bitmap);
            } catch (IOException e){
                Log.e(TAG, "encode: " + e.getMessage());
            }
        }

        try {
            encoder.finish();
        } catch (IOException e){
            Log.e(TAG, e.getMessage());
        }
        NIOUtils.closeQuietly(out);
    }

    public void processSingleFrameTest(int rId, String name, String threadName){
        // Initial
        VideoManager videoManager = new VideoManager(this);

        // Read Video from RAW Folder
        ArrayList<Mat> mats = videoManager.readVideo(rId, name);

        // Calculate new size
        Size ogSize = mats.get(0).size();
        double ratio = ogSize.width/WIDTH;
        Size newSize = new Size(WIDTH, ogSize.height/ratio);

        Log.i(TAG, "ratio: " + ratio + ", new width: " + newSize.width + ", new height: " + ogSize.height);

        final Mat frame = mats.get(0);

        // Resize image
        Imgproc.resize(frame, frame, newSize);

        start = System.currentTimeMillis();
        Thread t1 = new Thread(new Runnable() {
            @Override public void run() {
                Long start = System.currentTimeMillis();
                testImageProcessor.process(frame, weightPath, configPath);
                Long finish = System.currentTimeMillis();
                Log.i(TAG, "Process time: " + ((finish - start)/1000.0) + " seconds");
            }
        });
        t1.setName(threadName);
        t1.start();

        Long finish = System.currentTimeMillis();
        Log.i(TAG, "processVideo: Total time: " + ((finish - start)/1000.0) + " seconds");
    }

    public void processSingleFrame(int rId, String name){
        // Initial
        VideoManager videoManager = new VideoManager(this);

        // Read Video from RAW Folder
        ArrayList<Mat> mats = videoManager.readVideo(rId, name);

        // Calculate new size
        Size ogSize = mats.get(0).size();
        double ratio = ogSize.width/WIDTH;
        Size newSize = new Size(WIDTH, ogSize.height/ratio);

        Log.i(TAG, "ratio: " + ratio + ", new width: " + newSize.width + ", new height: " + ogSize.height);

        Mat frame = mats.get(0);

        // Resize image
        Imgproc.resize(frame, frame, newSize);

        frame = imageProcessor.process(frame);

        Long finish = System.currentTimeMillis();
        Log.i(TAG, "processVideo: Total time: " + ((finish - start)/1000.0) + " seconds");
    }

    public void processParallelVideo(int rId, String name){
        // Initial
        VideoManager videoManager = new VideoManager(this);

        // Register receiver
        receiver = new ProcessorBroadcastReceiver();
        this.registerReceiver(this.receiver, new IntentFilter(ProcessorBroadcastReceiver.ACTION));

        // Init context for broadcasting and setup ImageProcessor
        ImageProcessorManager.setProcessor(this, this.weightPath, this.configPath);

        // Read Video from RAW Folder
        ArrayList<Mat> mats = videoManager.readVideo(rId, name);

        // Calculate new size
        Size ogSize = mats.get(0).size();
        double ratio = ogSize.width/WIDTH;
        Size newSize = new Size(WIDTH, ogSize.height/ratio);

//        Log.i(TAG, "ratio: " + ratio + ", new width: " + newSize.width + ", new height: " + ogSize.height);

        Mat frame = new Mat();

        /* TODO: Record time */
        start = Core.getTickCount();
        for(int i=0; i<mats.size();i++){
//            Log.i(TAG, "frame: " + i + "/" + mats.size());

//            if((i % 2) == 0) {
            ImageProcessorManager.process(mats.get(i), newSize, i, this.modelType);
//            }
        }

    }

    public void processVideo(int rId, String name){
        ArrayList<Mat> results = new ArrayList<>();

        // Initial
        VideoManager videoManager = new VideoManager(this);

        // Init for saving video
        File targetFolder = this.getExternalMediaDirs()[0];
        SeekableByteChannel out = null;
        AndroidSequenceEncoder encoder = null;
        try {
            /* TODO: Change save path to Gallery */
            out = NIOUtils.writableFileChannel(targetFolder.getAbsolutePath() + "/" + System.currentTimeMillis() + ".mp4");
            encoder = new AndroidSequenceEncoder(out, Rational.R(30, 1));
        } catch (FileNotFoundException fe){
            Log.e(TAG, "saveVideo: " + fe.getMessage());
        } catch (IOException ioe){
            Log.e(TAG, "saveVideo: " + ioe.getMessage());
        }

        // Read Video from RAW Folder
        ArrayList<Mat> mats = videoManager.readVideo(rId, name);

        // Calculate new size
        Size ogSize = mats.get(0).size();
        double ratio = ogSize.width/WIDTH;
        Size newSize = new Size(WIDTH, ogSize.height/ratio);

        Log.i(TAG, "ratio: " + ratio + ", new width: " + newSize.width + ", new height: " + ogSize.height);

        Mat frame = new Mat();

        /* TODO: Record time */
        Long start = Core.getTickCount();
        for(int i=0; i<mats.size();i++){
            Log.i("ImageProcessor", "frame: " + i + "/" + mats.size());

//            if((i % 2) == 0) {

                frame = mats.get(i);

                // Resize image
                Imgproc.resize(frame, frame, newSize);

                frame = this.imageProcessor.process(frame);
                results.add(frame);
//            }
        }

        Long finish = Core.getTickCount();
        Log.i(TAG, "processVideo: Total time: " + ((finish - start)/Core.getTickFrequency()) + " seconds");

//        for (Mat result:results) {
//            //Save frame to video
//            try {
//                Bitmap bitmap = Bitmap.createBitmap(result.width(), result.height(), Bitmap.Config.ARGB_8888);
//                Utils.matToBitmap(result, bitmap);
//                encoder.encodeImage(bitmap);
//            } catch (IOException e){
//                Log.e(TAG, "encode: " + e.getMessage());
//            }
//        }
//
//        try {
//            encoder.finish();
//        } catch (IOException e){
//            Log.e(TAG, e.getMessage());
//        }
//        NIOUtils.closeQuietly(out);
    }

    public void processImage(int rId, String name){

        // Initial
        ImageReader imageReader = new ImageReader(this);

        Mat image = imageReader.readImage(rId, name);

        // Calculate new size
//        Size ogSize = image.size();
//        double ratio = ogSize.width/WIDTH;
//        Size newSize = new Size(WIDTH, ogSize.height/ratio);
//        Imgproc.resize(image, image, newSize);
//        Log.i(TAG, "ratio: " + ratio + ", new width: " + newSize.width + ", new height: " + ogSize.height);

        Long start = System.currentTimeMillis();
        Mat mat = this.imageProcessor.process(image);
        Long finish = System.currentTimeMillis();
        Log.i(TAG, "processVideo: Total time: " + ((finish - start)/1000.0) + " seconds");

        Log.i(TAG, "processImage: Size: " + image.size().width + " x " + image.size().height);

        Bitmap savedImage = Bitmap.createBitmap(imageReader.bitmap);
        Utils.matToBitmap(mat, savedImage);
        MediaStore.Images.Media.insertImage(getContentResolver(), savedImage, "title", "description");
    }

    public class ProcessorBroadcastReceiver extends BroadcastReceiver {
        public static final String ACTION = "com.jinkawin.dissertation.SEND_PROCESS";

        @Override
        public void onReceive(Context context, Intent intent) {
            ArrayList<Result> results = ImageProcessorManager.getResults();
            Log.i(TAG, "onReceive: Result size: " + results.size());

            // Init for saving video
            SeekableByteChannel out = null;
            AndroidSequenceEncoder encoder = null;
            try {
                /* TODO: Change save path to Gallery */
                out = NIOUtils.writableFileChannel(saveVideoPath + "/" + System.currentTimeMillis() + ".mp4");
                encoder = new AndroidSequenceEncoder(out, Rational.R(30, 1));
            } catch (FileNotFoundException fe){
                Log.e(TAG, "saveVideo: " + fe.getMessage());
            } catch (IOException ioe){
                Log.e(TAG, "saveVideo: " + ioe.getMessage());
            }

            Long finish = Core.getTickCount();
            Log.i(TAG, "processVideo: Total time: " + ((finish - start)/Core.getTickFrequency()) + " seconds");

            // TODO: sort array
            Collections.sort(results, new ResultComparator());
            Log.i(TAG, "processParallelVideo: Results' size: " + results.size());

            // encode to video
            for (int i = 0; i < results.size(); i++) {
                try {
                    encoder.encodeImage(results.get(i).getBitmap());
                } catch (IOException e) {
                    Log.e(TAG, "processParallelVideo: " + e.getMessage());
                }
            }

            try {
                encoder.finish();
            } catch (IOException e){
                Log.e(TAG, e.getMessage());
            }
            NIOUtils.closeQuietly(out);
            Log.i(TAG, "onReceive: Finished");
        }
    }

    public void setup(ModelType type){
        // Load opencv4android lib
        loadOpenCV();

        this.saveVideoPath = this.getExternalMediaDirs()[0].getAbsolutePath();

        // Read and copy files to internal storage
        switch (type){
            case SSD:
                _readSSD();
                break;
            case YOLO:
                _readYOLO();
                break;
            default:
                type = ModelType.SSD;
                _readSSD();
        }

        // Initial
        this.imageProcessor = new ImageProcessor(this, this.weightPath, this.configPath, type);
        this.testImageProcessor = new TestImageProcessor();
    }

    private void _readYOLO(){
        // Read and copy files to internal storage
        FileUtility fileUtility = new FileUtility(this);
        this.weightPath = fileUtility.readAndCopyFile(R.raw.yolov3_weights, "yolov3_weights.weights");
        this.configPath = fileUtility.readAndCopyFile(R.raw.yolov3_cfg, "yolov3_cfg.cfg");

    }

    private void _readSSD(){
        // Read and copy files to internal storage
        FileUtility fileUtility = new FileUtility(this);
        this.weightPath = fileUtility.readAndCopyFile(R.raw.mobilenetssd_weight, "mobilenetssd_weight.caffemodel");
        this.configPath = fileUtility.readAndCopyFile(R.raw.mobilenetssd_config, "mobilenetssd_config.prototxt");
    }

    /**
     * Load OpenCV libraries (version 3.4.0)
     */
    public void loadOpenCV(){
        // If OpenCV's libraries are not loaded
        if(!OpenCVLoader.initDebug()){
//            boolean success = OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, blCallback);
            boolean success = OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, blCallback);
        }else{
            blCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }
}
