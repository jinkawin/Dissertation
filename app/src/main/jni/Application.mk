APP_PLATFORM := android-29
APP_STL      := c++_shared
APP_ABI      := arm64-v8a
APP_OPTIM    := release
APP_SHORT_COMMANDS := true
NDK_APPLICATION_MK := native-lib
APP_CPPFLAGS += -fexceptions -frtti -std=c++11 -D__STDC_CONSTANT_MACROS