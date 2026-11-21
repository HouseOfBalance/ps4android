#pragma once

#include <cstdint>
#include <string>

namespace ShadPS4::Android::Window {

/**
 * Android Window abstraction for shadPS4 Video Core.
 * Replaces Desktop WindowSDL with native Android lifecycle callbacks.
 */
class WindowAndroid {
public:
    WindowAndroid(void* native_window, int32_t width, int32_t height);
    virtual ~WindowAndroid();

    void PollEvents();
    bool IsOpen() const { return is_open; }
    void Close() { is_open = false; }

    int32_t GetWidth() const { return width; }
    int32_t GetHeight() const { return height; }
    void* GetNativeWindow() const { return native_window; }

    void Resize(int32_t new_w, int32_t new_h);

private:
    void* native_window{nullptr};
    int32_t width{0};
    int32_t height{0};
    bool is_open{true};
};

} // namespace ShadPS4::Android::Window
