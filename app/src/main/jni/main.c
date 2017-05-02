#include "experiment_network_ipv4overipv6_MainActivity.h"

JNIEXPORT jstring JNICALL Java_experiment_network_ipv4overipv6_MainActivity_stringFromJNI(JNIEnv *env, jobject thi){
		return (*env)->NewStringUTF(env,"hello from JNI");
}