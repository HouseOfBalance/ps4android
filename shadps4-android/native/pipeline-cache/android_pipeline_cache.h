#pragma once

#include <string>
#include <vector>
#include <vulkan/vulkan.h>

namespace ShadPS4::Android::Graphics {

/**
 * Manages on-disk persistent caching of VkPipelineCache to eliminate in-game stutter.
 */
class AndroidPipelineCacheManager {
public:
    static AndroidPipelineCacheManager& Instance();

    bool Initialize(VkDevice device, const std::string& cache_dir, const std::string& title_id);
    void SaveCache(VkDevice device);
    void Destroy(VkDevice device);

    VkPipelineCache GetVkPipelineCache() const { return pipeline_cache; }

private:
    AndroidPipelineCacheManager() = default;
    ~AndroidPipelineCacheManager() = default;

    VkPipelineCache pipeline_cache{VK_NULL_HANDLE};
    std::string cache_file_path;
};

} // namespace ShadPS4::Android::Graphics
