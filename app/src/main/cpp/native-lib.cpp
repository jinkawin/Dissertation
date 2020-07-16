#include <jni.h>
#include <string>

extern "C"

JNIEXPORT jstring JNICALL
Java_com_jinkawin_dissertation_MainActivity_helloWorld(JNIEnv *env, jobject jObj){
    std::string hello = "Hello";
    return env->NewStringUTF(hello.c_str());
}

JNIEXPORT void JNICALL
Java_com_jinkawin_dissertation_NativeLib_process(JNIEnv *env, jobject jObj){
}


