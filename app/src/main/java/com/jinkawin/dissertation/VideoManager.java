package com.jinkawin.dissertation;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.api.android.AndroidSequenceEncoder;
import org.jcodec.common.AndroidUtil;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Rational;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

public class VideoManager {

    public static final String TAG = "VideoManager";
    private static final String PATH_RAW = "android.resource://com.jinkawin.dissertation/raw/";

    public static String CONTAINER = ".mp4";
    public static final int FPS = 30;

    private Context context;
    private ArrayList<Mat> frames;

    public VideoManager(Context context){
        this.context = context;
        this.frames = new ArrayList<>();
    }

    /**
     * Read video from raw folder
     * @param resId   R.raw.{id}
     * @return ArrayList<Picture>
     */
    public ArrayList<Mat> readVideo(int resId, String filename){
        FileUtility fileUtility = new FileUtility(this.context);
        ArrayList<Mat> mats = new ArrayList<>();
        Picture frame;

        String filePath = fileUtility.readAndCopyFile(resId, filename);
        File file = new File(filePath);

        try {
            // Read video from java.io.File by using FrameGrab
            FrameGrab frameGrab = FrameGrab.createFrameGrab(NIOUtils.readableChannel(file));

            // For each frame in video
            while(null != (frame = frameGrab.getNativeFrame())){

                // Convert Picture -> Bitmap -> Mat (Video container in OpenCV)
                Bitmap bitmap = AndroidUtil.toBitmap(frame);
                Mat mat = new Mat();
                Utils.bitmapToMat(bitmap, mat);

                mats.add(mat);
            }
        } catch (FileNotFoundException fe){
            Log.e(TAG, "File not found");
        } catch (IOException ioe){
            Log.e(TAG, "I/O Exception");
        } catch (JCodecException je){
            Log.e(TAG, "JCode Exception");
        }

        return mats;
    }


    public void saveVideo(ArrayList<Mat> frames, String name){
        File targetFolder = this.context.getExternalMediaDirs()[0];
        SeekableByteChannel out = null;

        try {
            out = NIOUtils.writableFileChannel(targetFolder.getAbsolutePath() + "/" + name + CONTAINER);
            AndroidSequenceEncoder encoder = new AndroidSequenceEncoder(out, Rational.R(FPS, 1));


            for (Mat frame:frames) {
                Bitmap bitmap = Bitmap.createBitmap(frame.width(), frame.height(), Bitmap.Config.RGBA_F16);
                Utils.matToBitmap(frame, bitmap);
                encoder.encodeImage(bitmap);
            }

            encoder.finish();
        } catch (FileNotFoundException fe){
            Log.e(TAG, "saveVideo: " + fe.getMessage());
        } catch (IOException ioe){
            Log.e(TAG, "saveVideo: " + ioe.getMessage());
        } finally {
            NIOUtils.closeQuietly(out);
        }
    }


}
