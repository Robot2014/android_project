LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES += \
    MediaPlayerDemo.cpp

LOCAL_C_INCLUDES += \
    $(TOP)/frameworks/av/include/

LOCAL_SHARED_LIBRARIES += \
    libmedia \
    libgui \
    libbinder \
    libutils

LOCAL_MODULE := mmtest

include $(BUILD_EXECUTABLE)


