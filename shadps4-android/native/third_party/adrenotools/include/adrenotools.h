#pragma once

#ifdef __cplusplus
extern "C" {
#endif

#define ADRENOTOOLS_DRIVER_CUSTOM 0
#define ADRENOTOOLS_DRIVER_SYSTEM 1

/**
 * Open custom Vulkan driver (Turnip / freedreno) in a private linker namespace
 * with KGSL device node redirection.
 */
void* adrenotools_open_libvulkan(
    int feature_flags,
    const char* tmp_dir,
    const char* hook_dir,
    const char* custom_driver_dir,
    const char* custom_driver_name,
    const char* file_redirect_dir,
    void** out_handle
);

#ifdef __cplusplus
}
#endif
