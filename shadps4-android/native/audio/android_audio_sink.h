#pragma once

#include <aaudio/AAudio.h>
#include <vector>
#include <mutex>
#include <android/log.h>

namespace ShadPS4::Android::Audio {

class AndroidAudioSink {
public:
    static AndroidAudioSink& Instance();

    bool Start(int32_t sample_rate = 48000, int32_t channel_count = 2);
    void Stop();
    void WritePcm(const int16_t* pcm_data, size_t num_frames);

private:
    AndroidAudioSink() = default;
    ~AndroidAudioSink() = default;

    static aaudio_data_callback_result_t AudioCallback(
        AAudioStream* stream,
        void* userData,
        void* audioData,
        int32_t numFrames
    );

    AAudioStream* stream{nullptr};
    std::mutex audio_mutex;
    std::vector<int16_t> ring_buffer;
    size_t read_idx{0};
    size_t write_idx{0};
    bool is_running{false};
};

} // namespace ShadPS4::Android::Audio
