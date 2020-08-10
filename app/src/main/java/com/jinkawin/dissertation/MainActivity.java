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
import org.opencv.android.Utils;
import org.opencv.core.Core;
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
    private static final int MEDIA_PICKER = 0;

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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Uri uploadfileuri = data.getData();
        File file = new File(uploadfileuri.getPath());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setup();

//        NativeLib.checkNeon();

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

        // Process by using Native lib
        NativeLib.process(image.getNativeObjAddr(), this.weightPath, this.configPath);

        // Save Image
        Bitmap savedImage = Bitmap.createBitmap(image.width(), image.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(image, savedImage);
        MediaStore.Images.Media.insertImage(getContentResolver(), savedImage, "title", "description");
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
        double ratio = ogSize.width/ModelSetup.WIDTH;
        Size newSize = new Size(ModelSetup.WIDTH, ogSize.height/ratio);

        /* TODO: Record time */
        start = Core.getTickCount();
        for(int i=0; i<mats.size();i++){
            ImageProcessorManager.process(mats.get(i), newSize, i, this.modelType);
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
        double ratio = ogSize.width/ModelSetup.WIDTH;
        Size newSize = new Size(ModelSetup.WIDTH, ogSize.height/ratio);

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

    public void setup(){
        this.saveVideoPath = this.getExternalMediaDirs()[0].getAbsolutePath();
        ModelSetup.setup(this, this.modelType);


        this.weightPath = ModelSetup.weightPath;
        this.configPath = ModelSetup.configPath;
        this.imageProcessor = ModelSetup.imageProcessor;
    }
}
