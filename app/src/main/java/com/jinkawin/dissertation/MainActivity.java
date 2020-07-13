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
import java.util.Collections;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";
    public static final int WIDTH = 480;

    public ArrayList<Result> results = new ArrayList<Result>();

    public ProcessorBroadcastReceiver receiver;

    public ImageProcessor imageProcessor;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setup(this.modelType);

        this.processParallelVideo();
//        this.processVideo();
//        this.processSingleFrame();
//        this.processImage();
    }

    public void processSingleFrame(){
        // Initial
        VideoManager videoManager = new VideoManager(this);

        // Read Video from RAW Folder
        ArrayList<Mat> mats = videoManager.readVideo(R.raw.video_test, "video_test.mp4");

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
        start = System.currentTimeMillis();
        for(int i=0; i<mats.size();i++){
            Log.i(TAG, "frame: " + i + "/" + mats.size());

//            if((i % 2) == 0) {
                ImageProcessorManager.process(mats.get(i), newSize, i, this.modelType);
//            }
        }

    }

    public void processVideo(){
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

                frame = this.imageProcessor.process(frame);
                results.add(frame);
            }
        }

        Long finish = System.currentTimeMillis();
        Log.i(TAG, "processVideo: Total time: " + ((finish - start)/1000.0) + " seconds");


        for (Mat result:results) {
            //Save frame to video
            try {
                Bitmap bitmap = Bitmap.createBitmap(result.width(), result.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(result, bitmap);
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

    public void processImage(){

        // Initial
        ImageReader imageReader = new ImageReader(this);

        Mat image = imageReader.readImage(R.raw.picturte_test, "picture_test.jpg");

        Long start = System.currentTimeMillis();
        Mat mat = this.imageProcessor.process(image);
        Long finish = System.currentTimeMillis();
        Log.i(TAG, "processVideo: Total time: " + ((finish - start)/1000.0) + " seconds");

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

            Long finish = System.currentTimeMillis();
            Log.i(TAG, "processVideo: Total time: " + ((finish - start)/1000.0) + " seconds");

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
            boolean success = OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, blCallback);
        }else{
            blCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }
}
