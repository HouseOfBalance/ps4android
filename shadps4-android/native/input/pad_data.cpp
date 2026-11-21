#include "pad_data.h"
#include <mutex>
#include <cstring>
#include <android/log.h>

#define LOG_TAG "ShadPS4_Input"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

static std::mutex g_pad_mutex;
static NativePadData g_pad_state[4] = {};

extern "C" {

void shadps4_input_update_pad(int pad_index, const NativePadData* data) {
    if (pad_index < 0 || pad_index >= 4 || !data) return;
    std::lock_guard<std::mutex> lock(g_pad_mutex);
    g_pad_state[pad_index] = *data;
}

// Internal getter for Orbis libpad HLE
void shadps4_input_get_pad(int pad_index, NativePadData* out_data) {
    if (pad_index < 0 || pad_index >= 4 || !out_data) return;
    std::lock_guard<std::mutex> lock(g_pad_mutex);
    *out_data = g_pad_state[pad_index];
}

} // extern "C"
