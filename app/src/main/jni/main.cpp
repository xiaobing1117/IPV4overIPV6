#include "experiment_network_ipv4overipv6_MainActivity.h"
#include <stdio.h>

#include <sys/types.h>

#include <sys/socket.h>

#include <netinet/in.h>

#include <arpa/inet.h>

#define  LOG_TAG    "hellojni"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)


JNIEXPORT jstring JNICALL Java_experiment_network_ipv4overipv6_MainActivity_stringFromJNI(JNIEnv *env, jobject thiz){


		return (*env).NewStringUTF("hello from JNI");
}
