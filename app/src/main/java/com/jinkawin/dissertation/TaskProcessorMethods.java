package com.jinkawin.dissertation;

import android.content.Context;

import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Size;

import java.util.ArrayList;
import java.util.List;

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

    ModelType getModel();

    ArrayList<DetectionLog> getDetectionLogs();

    void setDetectionLogs(ArrayList<DetectionLog> detectRect);
}
