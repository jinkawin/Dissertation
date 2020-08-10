# Copyright (C) 2009 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

OPENCVROOT := /Users/2divide3/Documents/Dissertation/Application/Dissertation/OpenCV-android-sdk
OPENCV_INSTALL_MODULES := on
OPENCV_LIB_TYPE := STATIC
include /Users/2divide3/Documents/Dissertation/Application/Dissertation/OpenCV-android-sdk/sdk/native/jni/OpenCV.mk

LOCAL_MODULE    := native-lib
LOCAL_SRC_FILES := native-lib.cpp
LOCAL_LDLIBS := -llog

LOCAL_LDLIBS := -lm -llog -ldl -lz
LOCAL_CPPFLAGS += -fexceptions -frtti -std=c++11
LOCAL_LDFLAGS += -ljnigraphics

include $(BUILD_SHARED_LIBRARY)
