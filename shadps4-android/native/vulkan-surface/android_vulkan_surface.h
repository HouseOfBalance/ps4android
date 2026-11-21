#pragma once

#include <android/native_window.h>
#include <vulkan/vulkan.h>
#include <vulkan/vulkan_android.h>
#include <mutex>

namespace ShadPS4::Android::Graphics {

class AndroidVulkanSurface {
public:
    static AndroidVulkanSurface& Instance();

    void SetNativeWindow(ANativeWindow* window);
    void UpdateDimensions(int32_t width, int32_t height);
    void DestroySurface();

    VkResult CreateSurface(VkInstance instance, VkSurfaceKHR* out_surface);
    void ConfigureSwapchainPacing(VkDevice device, VkSwapchainKHR swapchain);

    ANativeWindow* GetWindow() const { return native_window; }
    int32_t GetWidth() const { return width; }
    int32_t GetHeight() const { return height; }

private:
    AndroidVulkanSurface() = default;
    ~AndroidVulkanSurface() = default;

    std::mutex surface_mutex;
    ANativeWindow* native_window{nullptr};
    int32_t width{0};
    int32_t height{0};
    VkSurfaceKHR vk_surface{VK_NULL_HANDLE};
};

} // namespace ShadPS4::Android::Graphics
