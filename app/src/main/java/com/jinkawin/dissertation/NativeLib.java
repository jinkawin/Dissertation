package com.jinkawin.dissertation;

public class NativeLib {

    static {
        System.loadLibrary("native-lib");
    }

    public native static void process(long imageAddr, String weightPath, String configPath);
//    public native static String helloWorld();
}
