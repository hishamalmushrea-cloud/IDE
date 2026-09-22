#include <jni.h>
#include <string>
#include <android/log.h>

#define TAG "NativeApp"

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_nativeapp_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++ with NDK & CMake in AIDE Next!";
    __android_log_print(ANDROID_LOG_INFO, TAG, "%s", hello.c_str());
    return env->NewStringUTF(hello.c_str());
}
