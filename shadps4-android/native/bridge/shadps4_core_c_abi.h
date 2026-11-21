#ifndef SHADPS4_CORE_C_ABI_H
#define SHADPS4_CORE_C_ABI_H

#include <stdint.h>
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

// Return codes
#define SHADPS4_OK 0
#define SHADPS4_ERR_GENERIC -1
#define SHADPS4_ERR_INVALID_PATH -2
#define SHADPS4_ERR_SURFACE_FAILED -3
#define SHADPS4_ERR_INIT_FAILED -4

typedef enum {
    EMU_STATE_STOPPED = 0,
    EMU_STATE_STARTING = 1,
    EMU_STATE_RUNNING = 2,
    EMU_STATE_PAUSED = 3,
    EMU_STATE_CRASHED = 4
} EmuState;

typedef void (*EmuStateCallback)(EmuState state, const char* message);

// Configuration passed from Rust Host to C++ Core
typedef struct {
    const char* game_path;
    const char* title_id;
    const char* custom_driver_path; // Path to libvulkan_freedreno.so via libadrenotools
    bool enable_tso;               // Hardware half-barrier TSO (LDAPUR/STLUR)
    bool enable_vector_tso;
    bool enable_gpl;               // VK_EXT_graphics_pipeline_library
    void* native_window;           // ANativeWindow* from SurfaceView
    EmuStateCallback state_cb;
} EmuConfig;

/**
 * Initialize emulation core, memory subsystem (<512GB VA layout), and Vulkan renderer.
 */
int32_t shadps4_core_init(const EmuConfig* config);

/**
 * Start execution of the guest PS4 main binary using embedded FEXCore.
 * Runs in the caller's emulation thread.
 */
int32_t shadps4_core_run(void);

/**
 * Pause execution of all guest threads.
 */
int32_t shadps4_core_pause(void);

/**
 * Resume execution of guest threads.
 */
int32_t shadps4_core_resume(void);

/**
 * Gracefully stop emulator, flush cache, and free memory mappings.
 */
int32_t shadps4_core_stop(void);

/**
 * Update ANativeWindow surface when Android lifecycle triggers surfaceChanged or surfaceDestroyed.
 */
int32_t shadps4_core_update_surface(void* native_window, int32_t width, int32_t height);

#ifdef __cplusplus
}
#endif

#endif // SHADPS4_CORE_C_ABI_H
