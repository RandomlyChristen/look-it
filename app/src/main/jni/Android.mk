LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

#opencv library
OPENCVROOT:= $(LOCAL_PATH)/../../../../opencv-sdk
OPENCV_CAMERA_MODULES:=on
OPENCV_INSTALL_MODULES:=on
OPENCV_LIB_TYPE:=SHARED
include ${OPENCVROOT}/native/jni/OpenCV.mk

LOCAL_MODULE    := opencv-native
LOCAL_SRC_FILES := opencv-native.cpp
LOCAL_LDLIBS += -llog

include $(BUILD_SHARED_LIBRARY)
