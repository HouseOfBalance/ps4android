#include "window_android.h"
#include <android/log.h>

#define LOG_TAG "ShadPS4_Window"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

namespace ShadPS4::Android::Window {

WindowAndroid::WindowAndroid(void* window, int32_t w, int32_t h)
    : native_window(window), width(w), height(h), is_open(true) {
    LOGI("WindowAndroid initialized: %dx%d, handle=%p", width, height, native_window);
}

WindowAndroid::~WindowAndroid() {
    LOGI("WindowAndroid destroyed");
}

void WindowAndroid::PollEvents() {
    // Events are driven asynchronously from Android UI looper / Choreographer
}

void WindowAndroid::Resize(int32_t new_w, int32_t new_h) {
    width = new_w;
    height = new_h;
    LOGI("WindowAndroid resized: %dx%d", width, height);
}

} // namespace ShadPS4::Android::Window
