package com.jinkawin.dissertation;

import android.content.Context;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileUtility {

    public static final String TAG = "FileUtility";

    private Context context;
    public FileUtility(Context context){this.context = context;}

    /**
     *
     * @param resId     R.raw.{id}
     * @param fileName  Name of the chosen file
     * @return String - absolute path of file
     */
    // https://docs.opencv.org/3.4/d0/d6c/tutorial_dnn_android.html
    public String readAndCopyFile(int resId, String fileName) {
        String filePath = "";
        BufferedInputStream bis;
        try {
            bis = new BufferedInputStream(this.context.getResources().openRawResource(resId));
            byte data[] = new byte[bis.available()];

            // Read file
            bis.read(data);
            bis.close();

            // Create copy to internal storage
            File file = new File(this.context.getFilesDir(), fileName);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(data);
            fos.close();

            filePath = file.getAbsolutePath();

        } catch (IOException e) {
            Log.e(TAG, "Fail to read file");
        }

        return filePath;
    }
}
