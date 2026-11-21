#pragma once

#include <string>
#include <vulkan/vulkan.h>

namespace ShadPS4::Android::Graphics {

class CustomDriverLoader {
public:
    static CustomDriverLoader& Instance();

    /**
     * Initializes Vulkan driver entry points:
     * - If custom_driver_path is provided, loads Turnip via libadrenotools into an isolated namespace.
     * - Otherwise falls back to system libvulkan.so.
     */
    bool LoadDriver(const std::string& custom_driver_dir, const std::string& driver_lib_name, const std::string& tmp_dir);

    PFN_vkGetInstanceProcAddr GetVkGetInstanceProcAddr() const {
        return vkGetInstanceProcAddr_fn;
    }

    bool IsCustomDriverLoaded() const {
        return is_custom;
    }

private:
    CustomDriverLoader() = default;
    ~CustomDriverLoader() = default;

    void* driver_handle{nullptr};
    PFN_vkGetInstanceProcAddr vkGetInstanceProcAddr_fn{nullptr};
    bool is_custom{false};
};

} // namespace ShadPS4::Android::Graphics
