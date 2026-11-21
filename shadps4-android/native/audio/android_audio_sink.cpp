#include "android_audio_sink.h"
#include <cstring>

#define LOG_TAG "ShadPS4_Audio"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace ShadPS4::Android::Audio {

AndroidAudioSink& AndroidAudioSink::Instance() {
    static AndroidAudioSink instance;
    return instance;
}

bool AndroidAudioSink::Start(int32_t sample_rate, int32_t channel_count) {
    if (is_running) return true;

    AAudioStreamBuilder* builder = nullptr;
    aaudio_result_t result = AAudio_createStreamBuilder(&builder);
    if (result != AAUDIO_OK) {
        LOGE("Failed to create AAudio stream builder: %d", result);
        return false;
    }

    AAudioStreamBuilder_setSampleRate(builder, sample_rate);
    AAudioStreamBuilder_setChannelCount(builder, channel_count);
    AAudioStreamBuilder_setFormat(builder, AAUDIO_FORMAT_PCM_I16);
    AAudioStreamBuilder_setPerformanceMode(builder, AAUDIO_PERFORMANCE_MODE_LOW_LATENCY);
    AAudioStreamBuilder_setSharingMode(builder, AAUDIO_SHARING_MODE_SHARED);
    AAudioStreamBuilder_setDataCallback(builder, AudioCallback, this);

    result = AAudioStreamBuilder_openStream(builder, &stream);
    AAudioStreamBuilder_delete(builder);

    if (result != AAUDIO_OK) {
        LOGE("Failed to open AAudio stream: %d", result);
        return false;
    }

    ring_buffer.resize(48000 * 2); // 1-second stereo buffer
    read_idx = 0;
    write_idx = 0;

    result = AAudioStream_requestStart(stream);
    if (result != AAUDIO_OK) {
        LOGE("Failed to start AAudio stream: %d", result);
        AAudioStream_close(stream);
        stream = nullptr;
        return false;
    }

    is_running = true;
    LOGI("Low-latency AAudio output stream started (SampleRate: %d, Channels: %d)", sample_rate, channel_count);
    return true;
}

void AndroidAudioSink::Stop() {
    if (!is_running || !stream) return;
    AAudioStream_requestStop(stream);
    AAudioStream_close(stream);
    stream = nullptr;
    is_running = false;
    LOGI("AAudio stream stopped");
}

void AndroidAudioSink::WritePcm(const int16_t* pcm_data, size_t num_frames) {
    if (!is_running || !pcm_data) return;
    std::lock_guard<std::mutex> lock(audio_mutex);
    size_t samples = num_frames * 2;
    for (size_t i = 0; i < samples; ++i) {
        ring_buffer[write_idx] = pcm_data[i];
        write_idx = (write_idx + 1) % ring_buffer.size();
    }
}

aaudio_data_callback_result_t AndroidAudioSink::AudioCallback(
    AAudioStream* stream,
    void* userData,
    void* audioData,
    int32_t numFrames
) {
    auto* self = static_cast<AndroidAudioSink*>(userData);
    int16_t* out = static_cast<int16_t*>(audioData);
    size_t samples_needed = numFrames * 2;

    std::lock_guard<std::mutex> lock(self->audio_mutex);
    for (size_t i = 0; i < samples_needed; ++i) {
        if (self->read_idx != self->write_idx) {
            out[i] = self->ring_buffer[self->read_idx];
            self->read_idx = (self->read_idx + 1) % self->ring_buffer.size();
        } else {
            out[i] = 0; // Underrun: silence
        }
    }

    return AAUDIO_CALLBACK_RESULT_CONTINUE;
}

} // namespace ShadPS4::Android::Audio
