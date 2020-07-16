#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
//jstring
Java_com_jinkawin_dissertation_MainActivity_helloWorld(JNIEnv *env, jobject thiz){
    std::string hello = "Hello";
    return env->NewStringUTF(hello.c_str());
}

//whatever(
//        JNIEnv *env,
//        jobject /* this */){
//    std::string hello = "Hello";
//    return env->NewStringUTF(hello.c_str());
//};