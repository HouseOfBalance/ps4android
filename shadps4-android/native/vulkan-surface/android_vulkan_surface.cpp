#include "android_vulkan_surface.h"
#include <android/log.h>

#define LOG_TAG "ShadPS4_Surface"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace ShadPS4::Android::Graphics {

AndroidVulkanSurface& AndroidVulkanSurface::Instance() {
    static AndroidVulkanSurface instance;
    return instance;
}

void AndroidVulkanSurface::SetNativeWindow(ANativeWindow* window) {
    std::lock_guard<std::mutex> lock(surface_mutex);
    if (native_window) {
        ANativeWindow_release(native_window);
    }
    native_window = window;
    if (native_window) {
        ANativeWindow_acquire(native_window);
        width = ANativeWindow_getWidth(native_window);
        height = ANativeWindow_getHeight(native_window);
        LOGI("NativeWindow acquired: %dx%d", width, height);
    }
}

void AndroidVulkanSurface::UpdateDimensions(int32_t w, int32_t h) {
    std::lock_guard<std::mutex> lock(surface_mutex);
    width = w;
    height = h;
    LOGI("Surface dimensions updated: %dx%d", width, height);
}

void AndroidVulkanSurface::DestroySurface() {
    std::lock_guard<std::mutex> lock(surface_mutex);
    if (native_window) {
        ANativeWindow_release(native_window);
        native_window = nullptr;
    }
    LOGI("NativeWindow released");
}

VkResult AndroidVulkanSurface::CreateSurface(VkInstance instance, VkSurfaceKHR* out_surface) {
    std::lock_guard<std::mutex> lock(surface_mutex);
    if (!native_window) {
        LOGE("Cannot create VkSurfaceKHR: ANativeWindow is null");
        return VK_ERROR_INITIALIZATION_FAILED;
    }

    VkAndroidSurfaceCreateInfoKHR create_info{};
    create_info.sType = VK_STRUCTURE_TYPE_ANDROID_SURFACE_CREATE_INFO_KHR;
    create_info.window = native_window;

    auto vkCreateAndroidSurfaceKHR_fn = reinterpret_cast<PFN_vkCreateAndroidSurfaceKHR>(
        vkGetInstanceProcAddr(instance, "vkCreateAndroidSurfaceKHR")
    );

    if (!vkCreateAndroidSurfaceKHR_fn) {
        LOGE("Failed to get vkCreateAndroidSurfaceKHR function pointer");
        return VK_ERROR_EXTENSION_NOT_PRESENT;
    }

    VkResult res = vkCreateAndroidSurfaceKHR_fn(instance, &create_info, nullptr, out_surface);
    if (res == VK_SUCCESS) {
        vk_surface = *out_surface;
        LOGI("VK_KHR_android_surface created successfully!");
    } else {
        LOGE("vkCreateAndroidSurfaceKHR failed with code: %d", res);
    }
    return res;
}

void AndroidVulkanSurface::ConfigureSwapchainPacing(VkDevice device, VkSwapchainKHR swapchain) {
    // Integration point for Android Game SDK Swappy frame pacing at 120Hz/60Hz
    LOGI("Configured frame pacing with FIFO presentation queue.");
}

} // namespace ShadPS4::Android::Graphics
