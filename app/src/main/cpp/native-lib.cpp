#include <jni.h>
#include <string>
#include <iostream>
#include <android/log.h>

#include "Detector.hpp"
#include "Parallel.hpp"

#define APPNAME "NativeLib"
#define NUMBER_OF_THREADS 8

string jstring2string(JNIEnv *env, jstring jStr);
void arrayToVector(JNIEnv *env, jlongArray matAddrs, vector<Mat> &mats);
int64_t getTimeNsec();
void cvInfo();
void print_uint8 (uint8x16_t data);
void add3 (uint8x16_t *data);

extern "C" JNIEXPORT void JNICALL
Java_com_jinkawin_dissertation_NativeLib_neon(JNIEnv *env, jobject jObj){
    Detector detector;
    detector.determineDistanceNeon();
    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "Yes Neon");
}

extern "C" JNIEXPORT void JNICALL
Java_com_jinkawin_dissertation_NativeLib_process(JNIEnv *env, jobject jObj, jlong matAddr, jstring weightPath, jstring configPath){
    Detector detector;
    ModelConfig config;

    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "WeightPath: %s", env->GetStringUTFChars(weightPath, 0));
    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "ConfigPath: %s", env->GetStringUTFChars(configPath, 0));

    // Convert jString to std::string
    string stringWeightPath = jstring2string(env, weightPath);
    string stringConfigPath = jstring2string(env, configPath);

    // Setup Model
    config.model = MODEL::SSD;
    config.pathWeight = stringWeightPath;
    config.pathConfig = stringConfigPath;
    detector.setModelConfig(config);

    // Convert address to Mat and process
    Mat &frame = *(Mat *) matAddr;
    detector.process(frame);

    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "Mat Cols: %d", frame.cols);
    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "Mat Rows: %d", frame.rows);
}

extern "C" JNIEXPORT void JNICALL
Java_com_jinkawin_dissertation_NativeLib_videoProcess(JNIEnv *env, jobject jObj, jlongArray matAddrs, jstring weightPath, jstring configPath){
    Detector detector;
    ModelConfig config;

    // Convert jString to std::string
    string stringWeightPath = jstring2string(env, weightPath);
    string stringConfigPath = jstring2string(env, configPath);

    // Setup Model
    config.model = MODEL::SSD;
    config.pathWeight = stringWeightPath;
    config.pathConfig = stringConfigPath;
    detector.setModelConfig(config);

    // Get address from jArray
    jsize size = env->GetArrayLength(matAddrs);
    jlong *value = env->GetLongArrayElements(matAddrs, 0);

    int64 start = cv::getTickCount();

    // Convert each address to Mat
    for (int i = 0; i < size; i++) {
        Mat &frame = *(Mat *) value[i];
        detector.process(frame);
    }
    float diff = (cv::getTickCount() - start)/cv::getTickFrequency();
    __android_log_print(ANDROID_LOG_VERBOSE, "NativeLib", "Total: %lf: ", diff);
    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "Done");
}

extern "C" JNIEXPORT void JNICALL
Java_com_jinkawin_dissertation_NativeLib_parallelProcess(JNIEnv *env, jobject jObj, jlongArray matAddrs, jstring weightPath, jstring configPath){
    ModelConfig config;

    // Convert jString to std::string
    string stringWeightPath = jstring2string(env, weightPath);
    string stringConfigPath = jstring2string(env, configPath);

    // Setup Model
    config.model = MODEL::SSD;
    config.pathWeight = stringWeightPath;
    config.pathConfig = stringConfigPath;

    // Get address from jArray
    jsize size = env->GetArrayLength(matAddrs);
    jlong *value = env->GetLongArrayElements(matAddrs, 0);

    int64 start = cv::getTickCount();
    // Parallel Calculation
    cv::parallel_for_(cv::Range(0, NUMBER_OF_THREADS), Parallel_process(config, matAddrs, NUMBER_OF_THREADS, size, value));
    float diff = (cv::getTickCount() - start)/cv::getTickFrequency();
    __android_log_print(ANDROID_LOG_VERBOSE, "NativeLib", "Total: %lf: ", diff);

    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "Done");
}

extern "C" JNIEXPORT void JNICALL
Java_com_jinkawin_dissertation_NativeLib_getInfo(JNIEnv *env, jobject jObj){
    const string info = cv::getBuildInformation();

    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "CV Build Information: %s", info.c_str());
    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "CV Build Information: %s", CV_VERSION);
    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "CV Build Information: %s", info.c_str());

}

// Convert Array to Vector
void arrayToVector(JNIEnv *env, jlongArray matAddrs, vector<Mat> &mats){
    jsize size = env->GetArrayLength(matAddrs);
    jlong *value = env->GetLongArrayElements(matAddrs, 0);

    for (int i = 0; i < size; i++) {

        Mat &frame = *(Mat *) value[i];
        mats.push_back(frame);
    }
}


// Convert jString to String
string jstring2string(JNIEnv *env, jstring _jString) {

    // If jString is empty
    if (!_jString)
        return "";

    const jclass stringClass = env->GetObjectClass(_jString);
    const jmethodID methodId = env->GetMethodID(stringClass, "getBytes", "(Ljava/lang/String;)[B");
    const jbyteArray stringJbytes = (jbyteArray) env->CallObjectMethod(_jString, methodId, env->NewStringUTF("UTF-8"));

    size_t length = (size_t) env->GetArrayLength(stringJbytes);
    jbyte* pBytes = env->GetByteArrayElements(stringJbytes, NULL);

    std::string ret = std::string((char *)pBytes, length);
    env->ReleaseByteArrayElements(stringJbytes, pBytes, JNI_ABORT);

    env->DeleteLocalRef(stringJbytes);
    env->DeleteLocalRef(stringClass);
    return ret;
}