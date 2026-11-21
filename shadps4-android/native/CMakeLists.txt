cmake_minimum_required(VERSION 3.22)
project(shadps4_native_core LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(shadps4_native_bridge STATIC
    bridge/shadps4_core_c_abi.cpp
    bridge/jni_surface.cpp
    fex-frontend/fex_engine.cpp
    fex-frontend/hle_thunk_table.cpp
    driver-loader/custom_driver_loader.cpp
    vulkan-surface/android_vulkan_surface.cpp
    audio/android_audio_sink.cpp
    input/pad_data.cpp
    window/window_android.cpp
    shader-mobile/mobile_shader_optimizer.cpp
    pipeline-cache/android_pipeline_cache.cpp
)

target_include_directories(shadps4_native_bridge PUBLIC
    ${CMAKE_CURRENT_SOURCE_DIR}/bridge
    ${CMAKE_CURRENT_SOURCE_DIR}/fex-frontend
    ${CMAKE_CURRENT_SOURCE_DIR}/driver-loader
    ${CMAKE_CURRENT_SOURCE_DIR}/vulkan-surface
    ${CMAKE_CURRENT_SOURCE_DIR}/audio
    ${CMAKE_CURRENT_SOURCE_DIR}/input
    ${CMAKE_CURRENT_SOURCE_DIR}/window
    ${CMAKE_CURRENT_SOURCE_DIR}/shader-mobile
    ${CMAKE_CURRENT_SOURCE_DIR}/pipeline-cache
    ${CMAKE_CURRENT_SOURCE_DIR}/third_party/adrenotools/include
    ${CMAKE_CURRENT_SOURCE_DIR}/third_party/swappy/include
)

# Android log & native window & aaudio & dl libraries
find_library(log-lib log)
find_library(android-lib android)
find_library(aaudio-lib aaudio)
target_link_libraries(shadps4_native_bridge PRIVATE ${log-lib} ${android-lib} ${aaudio-lib} dl)
