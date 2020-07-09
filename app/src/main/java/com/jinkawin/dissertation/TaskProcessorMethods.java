package com.jinkawin.dissertation;

import android.content.Context;

import org.opencv.core.Mat;
import org.opencv.core.Size;

public interface TaskProcessorMethods {
    void setThread(Thread thread);

    void handleProcessState(ProcessStatus status);

    void setFrame(Mat frame);

    Mat getFrame();

    int getIndex();

    Size getSize();

    String getWeightPath();

    String getConfigPath();

    Context getContext();
}
