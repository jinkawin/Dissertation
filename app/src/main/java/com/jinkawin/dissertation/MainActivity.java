package com.jinkawin.dissertation;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;

import org.jcodec.api.android.AndroidSequenceEncoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Rational;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.processVideo();
//        this.processImage();

//        VideoManager videoManager = new VideoManager(this);
//        videoManager.addFrame();
    }

    public void processVideo(){
        // Initial
        ImageProcessor imageProcessor = new ImageProcessor(this);
        VideoManager videoManager = new VideoManager(this);

        // Init for saving video
        File targetFolder = this.getExternalMediaDirs()[0];
        SeekableByteChannel out = null;
        AndroidSequenceEncoder encoder = null;
        try {
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

        for(int i=0; i<mats.size();i++){
            Log.i("ImageProcessor", "frame: " + i + "/" + mats.size());
            if((i % 2) == 0) {
                Mat frame = mats.get(i);

                // Resize image
                Imgproc.resize(frame, frame, newSize);

                // Convert rgba to rgb
                Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGBA2RGB);
                frame = imageProcessor.process(frame);

                //Save frame to video
                try {
                    Bitmap bitmap = Bitmap.createBitmap(frame.width(), frame.height(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(frame, bitmap);
                    encoder.encodeImage(bitmap);
                } catch (IOException e){
                    Log.e(TAG, "encode: " + e.getMessage());
                }
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
        ImageProcessor imageProcessor = new ImageProcessor(this);
        ImageReader imageReader = new ImageReader(this);

        Mat image = imageReader.readImage(R.raw.picturte_test, "picture_test.jpg");

        Imgproc.cvtColor(image, image, Imgproc.COLOR_RGBA2RGB);

        Mat mat = imageProcessor.process(image);

        Bitmap savedImage = Bitmap.createBitmap(imageReader.bitmap);
        Utils.matToBitmap(mat, savedImage);
        MediaStore.Images.Media.insertImage(getContentResolver(), savedImage, "title", "description");
    }
}
