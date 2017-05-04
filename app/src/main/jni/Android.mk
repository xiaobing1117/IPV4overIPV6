LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := setConnection
LOCAL_SRC_FILES := setConnection.cpp
LOCAL_LDLIBS := -llog

include $(BUILD_SHARED_LIBRARY)