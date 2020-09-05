#include <iostream>
#include <fstream>
#include <string>
#include <stdlib.h>
#include <typeinfo>

#include <opencv2/dnn.hpp>
#include <opencv2/core/core.hpp>
#include <opencv2/core/types.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/highgui/highgui.hpp>

#include "arm_neon.h"

#define TAG "Detector"

using namespace cv;
using namespace dnn;
using namespace std;

typedef enum {YOLO, SSD} MODEL;

struct ModelConfig{
    MODEL model;
    string pathWeight;
    string pathConfig;
};

struct Box{
    Rect rect;
    int centreX;
    int centreY;
};

class Detector{

public:
    const double CONFIDENCE_THRESHOLD_YOLO = 0.5;
    const double CONFIDENCE_THRESHOLD_SSD = 0.3;
    const double SCALE_FACTOR = 1/255.0;
    const double NMS_THRESHOLD = 0.3;

    const int WIDTH = 480;

    const int PERSON = 15;

    const Size BLOB_SIZE = Size(416.0, 416.0);
    const Scalar BLOB_MEAN = Scalar(127.5, 127.5, 127.5);

    const Scalar COLOUR_WHITE = Scalar(255, 255, 255);
    const Scalar COLOUR_RED = Scalar(255, 0, 0);
    const Scalar COLOUR_GREEN = Scalar(0, 255, 0);

private:
    const int COLOUR_YOLO = COLOR_RGBA2BGR;
    const int COLOUR_SSD = COLOR_RGBA2RGB;

    ModelConfig config;
    Net network;

public:
    Detector(){
        // Set default model
        config.model = MODEL::SSD;
    }

    string hello(){
        return "hello";
    }

    void process(Mat &frame){
        vector<Box> nmsBoxes;

        Point a = Point(377, 13);
        Point b = Point(246, 16);

        int rowNum = frame.rows;
        int colNum = frame.cols;

        // Resize Frame
//        this->resizeFrame(frame, this->WIDTH);

        // Convert Colour
        int colour = (this->config.model == YOLO)?this->COLOUR_YOLO:this->COLOUR_SSD;
        cvtColor(frame, frame, colour);

        // Preprocessing
        Mat blob = blobFromImage(frame, SCALE_FACTOR, BLOB_SIZE, BLOB_MEAN, false, false);

        // Set input to the network
        this->network.setInput(blob);

        // Forward Propagation
        int64_t start = cv::getTickCount();
        Mat detection = this->network.forward();
        float diff = (cv::getTickCount() - start)/cv::getTickFrequency();
        __android_log_print(ANDROID_LOG_VERBOSE, "NativeLib", "Dnn forward: %lf: ", diff);

        // Human Detection
        this->detectPersonSSD(nmsBoxes, detection, frame);

        // Calculate Distance Between People
        this->determineDistance(nmsBoxes, frame);
    }

    void setModelConfig(ModelConfig config){
        this->config = config;
        this->setNetwork();
    }

    void printConfig(){
        string modelName = (this->config.model == MODEL::YOLO)?"YOLO":"SSD";
        cout << "Model: " + modelName << ", Weight Path: " << this->config.pathWeight << ", Config Path: " << this->config.pathConfig << endl;
    }

    string getConfig(){
        string modelName = (this->config.model == MODEL::YOLO)?"YOLO":"SSD";
        return "Model: " + modelName + ", Weight Path: " + this->config.pathWeight + ", Config Path: " + this->config.pathConfig;
    }

    void determineDistanceNeon(){
        /* Create custom arbitrary data. */
        const uint8_t uint8_data[] = { 1, 2, 3, 4, 5, 6, 7, 8,
                                       9, 10, 11, 12, 13, 14, 15, 16 };

        /* Create the vector with our data. */
        uint8x16_t data;

        /* Load our custom data into the vector register. */
        data = vld1q_u8 (uint8_data);

        print_uint8 (data);

        /* Call of the add3 function. */
        add3(&data);

        print_uint8 (data);
    }

private:

    void detectPersonSSD(vector<Box> &nmsBoxes, Mat &detection, Mat &frame){
        vector<Rect> boxes;
        vector<float> confidences;
        vector<int> indices;

        detection = detection.reshape(1, detection.total()/7);

        for (int i = 0; i < detection.rows; i++){
            int classId = (int)(detection.at<float>(i, 1));
            float confidence = detection.at<float>(i, 2);

            if(confidence > CONFIDENCE_THRESHOLD_SSD && classId == PERSON){

                // Get detected object's location
                int left = (int)(detection.at<float>(i, 3) * frame.cols);
                int top = (int)(detection.at<float>(i, 4) * frame.rows);
                int right = (int)(detection.at<float>(i, 5) * frame.cols);
                int bottom = (int)(detection.at<float>(i, 6) * frame.rows);

                confidences.push_back(confidence);
                Rect _rect = Rect(Point(left, top), Point(right, bottom));
                boxes.push_back(_rect);
            }

        }

        // NMSBox
        NMSBoxes(boxes, confidences, (float)CONFIDENCE_THRESHOLD_SSD, NMS_THRESHOLD, indices);

        // Filter Boxes
        for (size_t i = 0; i < indices.size(); i++){
            Rect &_box = boxes[i];

            // Calculate centre of x and y
            Box _nmsBox;
            _nmsBox.rect = _box;
            _nmsBox.centreX = (int)(_box.x + (_box.width/2.0));
            _nmsBox.centreY = (int)(_box.y + (_box.height/2.0));

            // add Box into vector
            nmsBoxes.push_back(_nmsBox);

            // Draw white box
            rectangle(frame, _box, COLOUR_WHITE);
        }
    }

    void determineDistance(vector<Box> nmsBoxes, Mat frame){
        bool statuses[nmsBoxes.size()];

        // Compair
        for (size_t i=0; i<nmsBoxes.size(); i++){
            for (size_t j=i+1; j<nmsBoxes.size(); j++){
                bool status = this->checkDistance(Point(nmsBoxes[i].centreX, nmsBoxes[i].centreY), Point(nmsBoxes[j].centreX, nmsBoxes[j].centreY));
                statuses[i] |= status;
                statuses[j] |= status;
            }
        }

        for (size_t i = 0; i < nmsBoxes.size(); i++){
            Scalar colour = statuses[i]?COLOUR_RED:COLOUR_GREEN;
            rectangle(frame, nmsBoxes[i].rect, colour);
        }
    }

    void resizeFrame(Mat &frame, int newWidth){
        double ratio = frame.size().width/(double)newWidth;
        Size newSize = Size(newWidth, frame.size().height/ratio);

        resize(frame, frame, newSize);
    }

    void setNetwork(){
        switch (this->config.model){
            case MODEL::YOLO:
                this->network = readNetFromDarknet(this->config.pathConfig, this->config.pathWeight);
                break;
            case MODEL::SSD:
                this->network = readNetFromCaffe(this->config.pathConfig, this->config.pathWeight);
                break;

            default:
                break;
        }

//        this->network.setPreferableTarget(DNN_TARGET_OPENCL_FP16);
    }

    bool checkDistance(Point a, Point b){
        double calibration = (a.y + b.y) / 2.0;
        double dist = pow((pow((a.x - b.x), 2.0) + ((550 / (calibration)) * pow((a.y - b.y), 2.0))), 0.5);

        if(0 < dist && dist < (0.25 * calibration)){
            return true;
        }else{
            return false;
        }
    }

    int64_t getTimeNsec() {
        struct timespec now;
        clock_gettime(CLOCK_MONOTONIC, &now);
        return (int64_t) now.tv_sec*1000000000LL + now.tv_nsec;
    }

    void add3 (uint8x16_t *data) {
        /* Set each sixteen values of the vector to 3.
         *
         * Remark: a 'q' suffix to intrinsics indicates
         * the instruction run for 128 bits registers.
         */
        uint8x16_t three = vmovq_n_u8 (3);

        /* Add 3 to the value given in argument. */
        *data = vaddq_u8 (*data, three);
    }

    void print_uint8 (uint8x16_t data) {
        int i;
        static uint8_t p[16];
        std::string outStr = "";

        vst1q_u8 (p, data);

        for (i = 0; i < 16; i++) {
            outStr.append(to_string(p[i]));
        }

        __android_log_print(ANDROID_LOG_VERBOSE, TAG, "out: %s", outStr.c_str());
    }
};