#include "android_pipeline_cache.h"
#include <fstream>
#include <android/log.h>

#define LOG_TAG "ShadPS4_PipeCache"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace ShadPS4::Android::Graphics {

AndroidPipelineCacheManager& AndroidPipelineCacheManager::Instance() {
    static AndroidPipelineCacheManager instance;
    return instance;
}

bool AndroidPipelineCacheManager::Initialize(
    VkDevice device, 
    const std::string& cache_dir, 
    const std::string& title_id
) {
    cache_file_path = cache_dir + "/" + title_id + "_vk_pipeline.bin";
    std::vector<char> initial_data;

    std::ifstream file(cache_file_path, std::ios::binary | std::ios::ate);
    if (file.is_open()) {
        std::streamsize size = file.tellg();
        file.seekg(0, std::ios::beg);
        initial_data.resize(size);
        if (file.read(initial_data.data(), size)) {
            LOGI("Loaded %zu bytes of cached pipelines from disk: %s", (size_t)size, cache_file_path.c_str());
        }
    }

    VkPipelineCacheCreateInfo create_info{};
    create_info.sType = VK_STRUCTURE_TYPE_PIPELINE_CACHE_CREATE_INFO;
    create_info.initialDataSize = initial_data.size();
    create_info.pInitialData = initial_data.empty() ? nullptr : initial_data.data();

    VkResult res = vkCreatePipelineCache(device, &create_info, nullptr, &pipeline_cache);
    if (res != VK_SUCCESS) {
        LOGE("Failed to create VkPipelineCache: %d", res);
        return false;
    }

    LOGI("VkPipelineCache successfully initialized.");
    return true;
}

void AndroidPipelineCacheManager::SaveCache(VkDevice device) {
    if (pipeline_cache == VK_NULL_HANDLE || cache_file_path.empty()) return;

    size_t data_size = 0;
    vkGetPipelineCacheData(device, pipeline_cache, &data_size, nullptr);

    if (data_size > 0) {
        std::vector<char> buffer(data_size);
        if (vkGetPipelineCacheData(device, pipeline_cache, &data_size, buffer.data()) == VK_SUCCESS) {
            std::ofstream file(cache_file_path, std::ios::binary);
            if (file.is_open()) {
                file.write(buffer.data(), data_size);
                LOGI("Saved %zu bytes of persistent VkPipelineCache to: %s", data_size, cache_file_path.c_str());
            }
        }
    }
}

void AndroidPipelineCacheManager::Destroy(VkDevice device) {
    if (pipeline_cache != VK_NULL_HANDLE) {
        SaveCache(device);
        vkDestroyPipelineCache(device, pipeline_cache, nullptr);
        pipeline_cache = VK_NULL_HANDLE;
    }
}

} // namespace ShadPS4::Android::Graphics
