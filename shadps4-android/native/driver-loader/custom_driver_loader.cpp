#include "custom_driver_loader.h"
#include <dlfcn.h>
#include <android/log.h>
#include "adrenotools.h"

#define LOG_TAG "ShadPS4_DriverLoader"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace ShadPS4::Android::Graphics {

CustomDriverLoader& CustomDriverLoader::Instance() {
    static CustomDriverLoader instance;
    return instance;
}

bool CustomDriverLoader::LoadDriver(const std::string& custom_driver_dir, 
                                    const std::string& driver_lib_name, 
                                    const std::string& tmp_dir) {
    if (!custom_driver_dir.empty() && !driver_lib_name.empty()) {
        LOGI("Attempting to load custom Turnip driver: %s/%s", 
             custom_driver_dir.c_str(), driver_lib_name.c_str());

        void* handle = nullptr;
        driver_handle = adrenotools_open_libvulkan(
            ADRENOTOOLS_DRIVER_CUSTOM,
            tmp_dir.c_str(),
            nullptr,
            custom_driver_dir.c_str(),
            driver_lib_name.c_str(),
            nullptr,
            &handle
        );

        if (driver_handle) {
            vkGetInstanceProcAddr_fn = reinterpret_cast<PFN_vkGetInstanceProcAddr>(
                dlsym(driver_handle, "vkGetInstanceProcAddr")
            );
            if (vkGetInstanceProcAddr_fn) {
                LOGI("Custom driver loaded successfully via libadrenotools namespace!");
                is_custom = true;
                return true;
            }
        }
        LOGE("Failed to load via libadrenotools, falling back to system driver...");
    }

    // Fallback: System Vulkan
    LOGI("Loading system libvulkan.so");
    driver_handle = dlopen("libvulkan.so", RTLD_NOW | RTLD_LOCAL);
    if (!driver_handle) {
        LOGE("Failed to dlopen system libvulkan.so: %s", dlerror());
        return false;
    }

    vkGetInstanceProcAddr_fn = reinterpret_cast<PFN_vkGetInstanceProcAddr>(
        dlsym(driver_handle, "vkGetInstanceProcAddr")
    );
    is_custom = false;
    return vkGetInstanceProcAddr_fn != nullptr;
}

} // namespace ShadPS4::Android::Graphics
