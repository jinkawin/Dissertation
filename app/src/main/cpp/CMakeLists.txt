cmake_minimum_required(VERSION 3.4.1)

#if(ANDROID_ABI STREQUAL armeabi-v7a)
#    set_target_properties(${TARGET} PROPERTIES COMPILE_FLAGS -mfpu=neon)
#endif()

set(CMAKE_VERBOSE_MAKEFILE on)
#set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++11")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++11 -DANDROID_ARM_NEON=TRUE -mfpu=neon")

include_directories(/Users/2divide3/Documents/Dissertation/Application/Dissertation/app/src/main/jniIncludes)
add_subdirectory(cpu_features)


add_library( # Sets the name of the library.
        native-lib

        # Sets the library as a shared library.
        SHARED
        # Provides a relative path to your source file(s).
        native-lib.cpp)

add_library(lib_opencv SHARED IMPORTED)

set_target_properties(lib_opencv PROPERTIES IMPORTED_LOCATION /Users/2divide3/Documents/Dissertation/Application/Dissertation/app/src/main/jniLibs/${ANDROID_ABI}/libopencv_java4.so)

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

target_link_libraries( # Specifies the target library.
        native-lib

        # Links the target library to the log library
        # included in the NDK.
        ${log-lib}

        lib_opencv)

target_link_libraries( # Specifies the target library.
        native-lib

        # Links the target library to the log library
        # included in the NDK.
        ${log-lib}

        cpu_features)