package com.jinkawin.dissertation;

import android.content.Context;
import android.util.Log;

import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Picture;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

public class VideoReader {

    public static final String TAG = "VideoReader";
    public static final String PATH_RAW = "android.resource://com.jinkawin.dissertation/raw/";

    private Context context;

    public VideoReader(Context context){
        this.context = context;
    }

    /**
     * Read video from raw folder
     * @param resId   R.raw.{id}
     * @return ArrayList<Picture>
     */
    public ArrayList<Picture> readVideo(int resId){
        FileUtility fileUtility = new FileUtility(this.context);
        ArrayList<Picture> pictures = new ArrayList<>();
        Picture frame;

        String filePath = fileUtility.readAndCopyFile(R.raw.video_6, "video_6.mp4");
        File file = new File(filePath);

        try {
            // Read video from java.io.File by using FrameGrab
            FrameGrab frameGrab = FrameGrab.createFrameGrab(NIOUtils.readableChannel(file));

            // For each frame in video
            while(null != (frame = frameGrab.getNativeFrame())){
                pictures.add(frame);
                Log.i(TAG, "Height: " + frame.getHeight() + ", Width: " + frame.getWidth());
            }
        } catch (FileNotFoundException fe){
            Log.e(TAG, "File not found");
        } catch (IOException ioe){
            Log.e(TAG, "I/O Exception");
        } catch (JCodecException je){
            Log.e(TAG, "JCode Exception");
        }

        return pictures;
    }

}
