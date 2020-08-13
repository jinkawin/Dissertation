package com.jinkawin.dissertation;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.VideoView;

import org.jcodec.api.android.AndroidSequenceEncoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Rational;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";
    private static final int IMAGE_PICKER = 0;
    private static final int VIDEO_PICKER = 1;

    private static final int GET_PERMISSION_READ_EXTERNAL_STROAGE = 0;
    private static final int GET_PERMISSION_WRITE_EXTERNAL_STROAGE = 1;

    public ArrayList<Result> results = new ArrayList<Result>();

    public ProcessorBroadcastReceiver receiver;
    public ImageProcessor imageProcessor;

    public String saveVideoPath;
    public String weightPath;
    public String configPath;

    public Long start;

    public ModelType modelType = ModelType.SSD;

    public Button btnCamera;
    public Button btnBrowseImage;
    public Button btnBrowseVideo;
    public ImageView ivResult;
    public VideoView vvResult;
    public ProgressBar pbProgress;

    public boolean isParellel = true;


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null) {
            Uri contentURI = data.getData();

            if(requestCode == IMAGE_PICKER) {
                vvResult.setVisibility(View.INVISIBLE);
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), contentURI);
                    ivResult.setVisibility(View.VISIBLE);
                    ivResult.setImageBitmap(processImage(bitmap));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else if(requestCode == VIDEO_PICKER){
                ivResult.setVisibility(View.INVISIBLE);
                if(isParellel){
                    pbProgress.setVisibility(View.VISIBLE);
                    processParallelVideo(contentURI, "input_video.mp4");
                }else{
                    vvResult.setVisibility(View.VISIBLE);
                    String path = processVideo(contentURI, "input_video.mp4");
                    vvResult.setVideoPath(path);
                    vvResult.start();
                }

            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setup();
        initView();
        setListener();
        requestPermission();

        vvResult.setVisibility(View.INVISIBLE);
        pbProgress.setVisibility(View.INVISIBLE);

//        FileUtility fileUtility = new FileUtility(this);
//
//        MediaController mediaController = new MediaController(this);
//        mediaController.setAnchorView(vvResult);
//
//        vvResult.setVideoPath(fileUtility.readAndCopyFile(R.raw.video_test, "video_test.mp4"));
//        vvResult.setMediaController(mediaController);
//        vvResult.start();

//        this.processNativeImage(R.raw.picturte_test, "picture_test.jpg");
//        this.processNativeParallelVideo(R.raw.video_test, "video_test.mp4");
//        this.processNativeVideo(R.raw.video_test, "video_test.mp4");

//        processSingleFrameTest(R.raw.video_test, "video_test.mp4", "Dnn 1");
//        processSingleFrameTest(R.raw.video_test, "video_test.mp4", "Dnn 2");
//        processSingleFrameTest(R.raw.video_test, "video_test.mp4", "Dnn 3");
//        processNativeParallelVideo(R.raw.video_test, "video_test.mp4");
//        processVideo(R.raw.video_test, "video_test.mp4");

//        this.processVideo(R.raw.video_test, "video_test.mp4");
//        this.processSingleFrame(R.raw.picturte_test, "picture_test.jpg");
//        this.processImage(R.raw.picturte_test, "picture_test.jpg");
//        this.processParallelVideo(R.raw.video_test, "video_test.mp4");
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

    public void processParallelVideo(Uri uri, String name){
        // Initial
        VideoManager videoManager = new VideoManager(this);

        // Register receiver
        receiver = new ProcessorBroadcastReceiver();
        this.registerReceiver(this.receiver, new IntentFilter(ProcessorBroadcastReceiver.ACTION));

        // Init context for broadcasting and setup ImageProcessor
        ImageProcessorManager.setProcessor(this, this.weightPath, this.configPath);

        // Read Video from RAW Folder
        ArrayList<Mat> mats = videoManager.readVideo(uri, name);

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

    public String processVideo(Uri uri, String name){
        ArrayList<Mat> results = new ArrayList<>();

        // Initial
        VideoManager videoManager = new VideoManager(this);

        // Init for saving video{
        File targetFolder = this.getExternalMediaDirs()[0];
        String outPath = targetFolder.getAbsolutePath() + "/" + System.currentTimeMillis() + ".mp4";

        SeekableByteChannel out = null;
        AndroidSequenceEncoder encoder = null;
        try {
            /* TODO: Change save path to Gallery */
            out = NIOUtils.writableFileChannel(outPath);
            encoder = new AndroidSequenceEncoder(out, Rational.R(30, 1));
        } catch (FileNotFoundException fe){
            Log.e(TAG, "saveVideo: " + fe.getMessage());
        } catch (IOException ioe){
            Log.e(TAG, "saveVideo: " + ioe.getMessage());
        }

        // Read Video from RAW Folder
        ArrayList<Mat> mats = videoManager.readVideo(uri, name);
        Log.i(TAG, "processVideo: Mats size: " + mats.size());

        // Calculate new size
        Size ogSize = mats.get(0).size();
        double ratio = ogSize.width/ModelSetup.WIDTH;
        Size newSize = new Size(ModelSetup.WIDTH, ogSize.height/ratio);

        Log.i(TAG, "ratio: " + ratio + ", new width: " + newSize.width + ", new height: " + ogSize.height);

        Mat frame = new Mat();

        /* TODO: Record time */
        Long start = Core.getTickCount();
        for(int i=0; i<mats.size();i++){
             frame = mats.get(i);

             // Resize image
             Imgproc.resize(frame, frame, newSize);

             frame = this.imageProcessor.process(frame);
             results.add(frame);
        }

        Long finish = Core.getTickCount();
        Log.i(TAG, "processVideo: Total time: " + ((finish - start)/Core.getTickFrequency()) + " seconds");

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

        return outPath;
    }

    public Bitmap processImage(Bitmap bitmap){

        // Initial
        ImageReader imageReader = new ImageReader(this);

        Mat image = imageReader.bitmapToMat(bitmap);

        Long start = System.currentTimeMillis();
        Mat mat = this.imageProcessor.process(image);
        Long finish = System.currentTimeMillis();
        Log.i(TAG, "processVideo: Total time: " + ((finish - start)/1000.0) + " seconds");

        Log.i(TAG, "processImage: Size: " + image.size().width + " x " + image.size().height);

        Bitmap savedImage = Bitmap.createBitmap(imageReader.bitmap);
        Utils.matToBitmap(mat, savedImage);
        MediaStore.Images.Media.insertImage(getContentResolver(), savedImage, "title", "description");

        return savedImage;
    }

    public class ProcessorBroadcastReceiver extends BroadcastReceiver {
        public static final String ACTION = "com.jinkawin.dissertation.SEND_PROCESS";

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            extras.get("data");
            
            if(extras.get("data") == ProcessStatus.FINISH) {

                ArrayList<Result> results = ImageProcessorManager.getResults();
                Log.i(TAG, "onReceive: Result size: " + results.size());
                String path = saveVideoPath + "/" + System.currentTimeMillis() + ".mp4";

                // Init for saving video
                SeekableByteChannel out = null;
                AndroidSequenceEncoder encoder = null;
                try {
                    /* TODO: Change save path to Gallery */
                    out = NIOUtils.writableFileChannel(path);
                    encoder = new AndroidSequenceEncoder(out, Rational.R(30, 1));
                } catch (FileNotFoundException fe) {
                    Log.e(TAG, "saveVideo: " + fe.getMessage());
                } catch (IOException ioe) {
                    Log.e(TAG, "saveVideo: " + ioe.getMessage());
                }

                Long finish = Core.getTickCount();
                Log.i(TAG, "processVideo: Total time: " + ((finish - start) / Core.getTickFrequency()) + " seconds");

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
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                }
                NIOUtils.closeQuietly(out);
                Log.i(TAG, "onReceive: Finished");

                pbProgress.setVisibility(View.INVISIBLE);
                vvResult.setVisibility(View.VISIBLE);
                vvResult.setVideoPath(path);
                vvResult.start();
            }else{
                int progress = (int)((ImageProcessorManager.results.size()/(ImageProcessorManager.inputCount * 1.0))*100);
                Log.i(TAG, "onReceive: Update progress: size: " + ImageProcessorManager.results.size() + ", count: " + ImageProcessorManager.inputCount + ", percent: " + progress + "%");
                pbProgress.setProgress(progress);

            }
        }
    }

    public void setup(){
        this.saveVideoPath = this.getExternalMediaDirs()[0].getAbsolutePath();
        ModelSetup.setup(this, this.modelType);


        this.weightPath = ModelSetup.weightPath;
        this.configPath = ModelSetup.configPath;
        this.imageProcessor = ModelSetup.imageProcessor;
    }

    public void initView(){
        this.btnCamera = findViewById(R.id.btnCamera);
        this.btnBrowseImage = findViewById(R.id.btnBrowseImage);
        this.btnBrowseVideo = findViewById(R.id.btnBrowseVideo);
        this.ivResult = findViewById(R.id.ivResult);
        this.vvResult = findViewById(R.id.vvResult);
        this.pbProgress = findViewById(R.id.pbProgress);
    }

    public void setListener(){

        this.btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), CameraActivity.class);
                startActivity(intent);
            }
        });

        this.btnBrowseImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                startActivityForResult(intent, IMAGE_PICKER);
            }
        });

        this.btnBrowseVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("video/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, VIDEO_PICKER);
            }
        });
    }

    public void requestPermission(){
        Log.i(TAG, "getGROUPSTORAGEPremission: In");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    GET_PERMISSION_READ_EXTERNAL_STROAGE);
            Log.i(TAG, "getGROUPSTORAGEPremission: No READ_EXTERNAL_STORAGE");
        }else if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    GET_PERMISSION_WRITE_EXTERNAL_STROAGE);
            Log.i(TAG, "getGROUPSTORAGEPremission: No WRITE_EXTERNAL_STORAGE");
        } else {
            Log.i(TAG, "getGROUPSTORAGEPremission: Yes");
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == GET_PERMISSION_READ_EXTERNAL_STROAGE) {
            Log.d(TAG, "GET_PERMISSION_READ_EXTERNAL_STROAGE");
        }else if (requestCode == GET_PERMISSION_READ_EXTERNAL_STROAGE) {
            Log.d(TAG, "GET_PERMISSION_WRITE_EXTERNAL_STROAGE");
        } else {
            Log.d(TAG, "code: " + requestCode);
        }
    }
}
