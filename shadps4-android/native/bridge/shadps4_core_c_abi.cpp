#include "shadps4_core_c_abi.h"
#include "../fex-frontend/fex_engine.h"
#include "../driver-loader/custom_driver_loader.h"
#include "../vulkan-surface/android_vulkan_surface.h"
#include <android/log.h>
#include <string>

#define LOG_TAG "ShadPS4_Bridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static std::string g_game_path;
static std::string g_title_id;
static std::string g_custom_driver_path;
static EmuConfig g_active_config;
static bool g_is_running = false;

extern "C" {

int32_t shadps4_core_init(const EmuConfig* config) {
    if (!config || !config->game_path) {
        LOGE("Invalid EmuConfig provided");
        return SHADPS4_ERR_INIT_FAILED;
    }

    // Persist C-strings internally so pointers remain valid after Rust caller frees memory
    g_game_path = config->game_path ? config->game_path : "";
    g_title_id = config->title_id ? config->title_id : "UNKNOWN";
    g_custom_driver_path = config->custom_driver_path ? config->custom_driver_path : "";

    g_active_config = *config;
    g_active_config.game_path = g_game_path.c_str();
    g_active_config.title_id = g_title_id.c_str();
    g_active_config.custom_driver_path = g_custom_driver_path.empty() ? nullptr : g_custom_driver_path.c_str();

    LOGI("Initializing shadPS4 Native Core for game: %s (Title: %s)", 
         g_active_config.game_path, g_active_config.title_id);

    // 1. Initialize Vulkan Driver (Custom Turnip or System)
    auto& driver_loader = ShadPS4::Android::Graphics::CustomDriverLoader::Instance();
    std::string driver_dir = config->custom_driver_path ? config->custom_driver_path : "";
    driver_loader.LoadDriver(driver_dir, "libvulkan_freedreno.so", "/data/local/tmp");

    // 2. Setup Native Window Surface if supplied
    if (config->native_window) {
        ShadPS4::Android::Graphics::AndroidVulkanSurface::Instance().SetNativeWindow(
            static_cast<ANativeWindow*>(config->native_window)
        );
    }

    // 3. Initialize FEXCore JIT engine
    auto& fex = ShadPS4::Android::CPU::FexEngine::Instance();
    if (!fex.Initialize(config->enable_tso, config->enable_vector_tso)) {
        LOGE("Failed to initialize FEXCore Dynarec");
        if (config->state_cb) config->state_cb(EMU_STATE_CRASHED, "FEXCore Init Failed");
        return SHADPS4_ERR_INIT_FAILED;
    }

    // 4. Initialize Low-latency AAudio Engine
    ShadPS4::Android::Audio::AndroidAudioSink::Instance().Start(48000, 2);

    if (config->state_cb) {
        config->state_cb(EMU_STATE_STARTING, "Core initialized successfully");
    }

    return SHADPS4_OK;
}

int32_t shadps4_core_run(void) {
    LOGI("shadps4_core_run: Starting execution loop");
    g_is_running = true;

    if (g_active_config.state_cb) {
        g_active_config.state_cb(EMU_STATE_RUNNING, "Emulation running");
    }

    auto& fex = ShadPS4::Android::CPU::FexEngine::Instance();
    fex.RunThread();

    g_is_running = false;
    if (g_active_config.state_cb) {
        g_active_config.state_cb(EMU_STATE_STOPPED, "Emulation ended");
    }

    return SHADPS4_OK;
}

int32_t shadps4_core_pause(void) {
    LOGI("shadps4_core_pause");
    if (g_active_config.state_cb) {
        g_active_config.state_cb(EMU_STATE_PAUSED, "Emulation paused");
    }
    return SHADPS4_OK;
}

int32_t shadps4_core_resume(void) {
    LOGI("shadps4_core_resume");
    if (g_active_config.state_cb) {
        g_active_config.state_cb(EMU_STATE_RUNNING, "Emulation resumed");
    }
    return SHADPS4_OK;
}

int32_t shadps4_core_stop(void) {
    LOGI("shadps4_core_stop");
    g_is_running = false;

    // 1. Stop audio playback to prevent audio thread hang
    ShadPS4::Android::Audio::AndroidAudioSink::Instance().Stop();

    // 2. Release native window & surface resources
    ShadPS4::Android::Graphics::AndroidVulkanSurface::Instance().DestroySurface();

    // 3. Shutdown CPU recompiler
    auto& fex = ShadPS4::Android::CPU::FexEngine::Instance();
    fex.Shutdown();

    if (g_active_config.state_cb) {
        g_active_config.state_cb(EMU_STATE_STOPPED, "Emulation stopped cleanly");
    }

    return SHADPS4_OK;
}

int32_t shadps4_core_update_surface(void* native_window, int32_t width, int32_t height) {
    LOGI("shadps4_core_update_surface: window=%p, %dx%d", native_window, width, height);
    g_active_config.native_window = native_window;
    // Notify Vulkan swapchain reconstruction with VkSwapchainKHR.oldSwapchain
    return SHADPS4_OK;
}

} // extern "C"
