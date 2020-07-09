package com.jinkawin.dissertation;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;

import org.jcodec.api.android.AndroidSequenceEncoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Rational;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";
    public static final int WIDTH = 480;

    public ArrayList<Result> results = new ArrayList<Result>();

    public ProcessorBroadcastReceiver receiver;

    public String saveVideoPath;

    public String weightPath;
    public String configPath;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setup();

        this.processParallelVideo();
//        this.processVideo();
//        this.processImage();
    }

    public void processParallelVideo(){
        // Initial
        VideoManager videoManager = new VideoManager(this);

        // Register receiver
        receiver = new ProcessorBroadcastReceiver();
        this.registerReceiver(this.receiver, new IntentFilter(ProcessorBroadcastReceiver.ACTION));

        // Init context for broadcasting and setup ImageProcessor
        ImageProcessorManager.setProcessor(this, this.weightPath, this.configPath);

        // Read Video from RAW Folder
        ArrayList<Mat> mats = videoManager.readVideo(R.raw.video_test, "video_test.mp4");

        // Calculate new size
        Size ogSize = mats.get(0).size();
        double ratio = ogSize.width/WIDTH;
        Size newSize = new Size(WIDTH, ogSize.height/ratio);

        Log.i(TAG, "ratio: " + ratio + ", new width: " + newSize.width + ", new height: " + ogSize.height);

        Mat frame = new Mat();

        /* TODO: Record time */
        Long start = System.currentTimeMillis();
        for(int i=0; i<mats.size();i++){
//        for(int i=0; i<2;i++){
            Log.i(TAG, "frame: " + i + "/" + mats.size());

//            if((i % 2) == 0) {
                ImageProcessorManager.process(mats.get(i), newSize, i);
//            }

            //Save frame to video
//            try {
//                Bitmap bitmap = Bitmap.createBitmap(frame.width(), frame.height(), Bitmap.Config.ARGB_8888);
//                Utils.matToBitmap(frame, bitmap);
//                encoder.encodeImage(bitmap);
//            } catch (IOException e){
//                Log.e(TAG, "encode: " + e.getMessage());
//            }
        }

        Long finish = System.currentTimeMillis();
        Log.i(TAG, "processVideo: Total time: " + ((finish - start)/1000.0) + " seconds");

    }

    public void processVideo(){
        // Initial
        ImageProcessor imageProcessor = new ImageProcessor(this, this.weightPath, this.configPath);
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
        ArrayList<Mat> mats = videoManager.readVideo(R.raw.video_test, "video_test.mp4");

        // Calculate new size
        Size ogSize = mats.get(0).size();
        double ratio = ogSize.width/WIDTH;
        Size newSize = new Size(WIDTH, ogSize.height/ratio);

        Log.i(TAG, "ratio: " + ratio + ", new width: " + newSize.width + ", new height: " + ogSize.height);

        Mat frame = new Mat();

        /* TODO: Record time */
        Long start = System.currentTimeMillis();
        for(int i=0; i<mats.size();i++){
            Log.i("ImageProcessor", "frame: " + i + "/" + mats.size());

            if((i % 2) == 0) {

                frame = mats.get(i);

                // Resize image
                Imgproc.resize(frame, frame, newSize);

                // Convert rgba to rgb
                Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGBA2RGB);
                frame = imageProcessor.process(frame);
            }

            //Save frame to video
            try {
                Bitmap bitmap = Bitmap.createBitmap(frame.width(), frame.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(frame, bitmap);
                encoder.encodeImage(bitmap);
            } catch (IOException e){
                Log.e(TAG, "encode: " + e.getMessage());
            }
        }

        Long finish = System.currentTimeMillis();
        Log.i(TAG, "processVideo: Total time: " + ((finish - start)/1000.0) + " seconds");

        try {
            encoder.finish();
        } catch (IOException e){
            Log.e(TAG, e.getMessage());
        }
        NIOUtils.closeQuietly(out);
    }

    public void processImage(){

        // Initial
        ImageProcessor imageProcessor = new ImageProcessor(this, this.weightPath, this.configPath);
        ImageReader imageReader = new ImageReader(this);

        Mat image = imageReader.readImage(R.raw.picturte_test, "picture_test.jpg");

        Imgproc.cvtColor(image, image, Imgproc.COLOR_RGBA2RGB);

        Mat mat = imageProcessor.process(image);

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

            // TODO: sort array
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
        }
    }

    public void setup(){
        loadOpenCV();

        this.saveVideoPath = this.getExternalMediaDirs()[0].getAbsolutePath();

        // Read and copy files to internal storage
        FileUtility fileUtility = new FileUtility(this);
        this.weightPath = fileUtility.readAndCopyFile(R.raw.yolov3_weights, "yolov3_weights.weights");
        this.configPath = fileUtility.readAndCopyFile(R.raw.yolov3_cfg, "yolov3_cfg.cfg");
    }

    /**
     * Load OpenCV libraries (version 3.4.0)
     */
    public void loadOpenCV(){
        // If OpenCV's libraries are not loaded
        if(!OpenCVLoader.initDebug()){
            boolean success = OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, blCallback);
        }else{
            blCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }
}
