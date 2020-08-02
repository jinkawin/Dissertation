#include <jni.h>
#include <string>
#include <iostream>
#include <android/log.h>

#include "Detector.hpp"
#include "Parallel.hpp"

#define APPNAME "NativeLib"
#define NUMBER_OF_THREADS 2

string jstring2string(JNIEnv *env, jstring jStr);
void arrayToVector(JNIEnv *env, jlongArray matAddrs, vector<Mat> &mats);

extern "C" JNIEXPORT void JNICALL
Java_com_jinkawin_dissertation_NativeLib_process(JNIEnv *env, jobject jObj, jlong matAddr, jstring weightPath, jstring configPath){
    Detector detector;
    ModelConfig config;

    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "Hello Log");

    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "WeightPath: %s", env->GetStringUTFChars(weightPath, 0));
    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "ConfigPath: %s", env->GetStringUTFChars(configPath, 0));

    // Convert jString to std::string
    string stringWeightPath = jstring2string(env, weightPath);
    string stringConfigPath = jstring2string(env, configPath);

    config.model = MODEL::SSD;
    config.pathWeight = stringWeightPath;
    config.pathConfig = stringConfigPath;
    detector.setModelConfig(config);

    Mat &frame = *(Mat *) matAddr;
    detector.process(frame);


    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "Mat Cols: %d", frame.cols);
    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "Mat Rows: %d", frame.rows);
}

extern "C" JNIEXPORT void JNICALL
Java_com_jinkawin_dissertation_NativeLib_parallelProcess(JNIEnv *env, jobject jObj, jlongArray matAddrs, jstring weightPath, jstring configPath){
    ModelConfig config;

    // Convert jString to std::string
    string stringWeightPath = jstring2string(env, weightPath);
    string stringConfigPath = jstring2string(env, configPath);

    config.model = MODEL::SSD;
    config.pathWeight = stringWeightPath;
    config.pathConfig = stringConfigPath;

    // Initial vector with size of jlongArray's size
    vector<Mat> mats;
    arrayToVector(env, matAddrs, mats);

    cv::parallel_for_(cv::Range(0, NUMBER_OF_THREADS), Parallel_process(config, mats, NUMBER_OF_THREADS));
    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "Done");
}


void arrayToVector(JNIEnv *env, jlongArray matAddrs, vector<Mat> &mats){
    jsize size = env->GetArrayLength(matAddrs);
    jlong *value = env->GetLongArrayElements(matAddrs, 0);

    for (int i = 0; i < size; i++) {

        Mat &frame = *(Mat *) value[i];
        mats.push_back(frame);
    }
}

// Credit: https://stackoverflow.com/a/41820336/3239784
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

//extern "C" JNIEXPORT jstring JNICALL
//Java_com_jinkawin_dissertation_NativeLib_helloWorld(JNIEnv *env, jobject jObj){
//    Detector detector;
//    return env->NewStringUTF(detector.helloWorld().c_str());
//}