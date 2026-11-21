#include <jni.h>
#include <android/native_window_jni.h>
#include <android/log.h>
#include "../bridge/shadps4_core_c_abi.h"

#define LOG_TAG "ShadPS4_JNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C" {

JNIEXPORT void JNICALL
Java_net_shadps4_android_EmulatorActivity_nativeSetSurface(
    JNIEnv* env,
    jobject /* thiz */,
    jobject surface,
    jint width,
    jint height
) {
    if (!surface) {
        LOGI("nativeSetSurface: surface destroyed");
        shadps4_core_update_surface(nullptr, 0, 0);
        return;
    }

    ANativeWindow* window = ANativeWindow_fromSurface(env, surface);
    LOGI("nativeSetSurface: acquired ANativeWindow=%p (%dx%d)", window, width, height);
    shadps4_core_update_surface(window, width, height);
}

} // extern "C"
