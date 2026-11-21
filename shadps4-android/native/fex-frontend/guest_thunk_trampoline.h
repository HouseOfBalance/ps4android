#pragma once

#include <cstdint>
#include <vector>
#include <mutex>
#include <sys/mman.h>
#include <android/log.h>

#define LOG_TAG "ShadPS4_ThunkStub"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

namespace ShadPS4::Android::CPU {

/**
 * Guest x86-64 Trampoline Allocator.
 * Generates valid x86 binary code sequences that FEXCore traps:
 *
 *   mov eax, <thunk_id>   (5 bytes: B8 [id_le])
 *   ud2                   (2 bytes: 0F 0B)
 *   ret                   (1 byte:  C3)
 *
 * When guest executes this stub, FEXCore catches UD2 signal,
 * reads EAX, and dispatches to the host AAPCS64 C++ function.
 */
class GuestThunkTrampoline {
public:
    static GuestThunkTrampoline& Instance() {
        static GuestThunkTrampoline instance;
        return instance;
    }

    uint64_t AllocateStub(uint32_t thunk_id) {
        std::lock_guard<std::mutex> lock(stub_mutex);
        if (!trampoline_page) {
            // Allocate RWX anonymous page for guest x86 stubs
            trampoline_page = static_cast<uint8_t*>(
                mmap(nullptr, 65536, PROT_READ | PROT_WRITE | PROT_EXEC, 
                     MAP_PRIVATE | MAP_ANONYMOUS, -1, 0)
            );
            current_offset = 0;
            LOGI("Allocated Guest x86 Trampoline Page at %p", trampoline_page);
        }

        if (current_offset + 8 > 65536) {
            return 0; // Page full
        }

        uint8_t* stub = trampoline_page + current_offset;
        // mov eax, thunk_id (B8 xx xx xx xx)
        stub[0] = 0xB8;
        stub[1] = static_cast<uint8_t>(thunk_id & 0xFF);
        stub[2] = static_cast<uint8_t>((thunk_id >> 8) & 0xFF);
        stub[3] = static_cast<uint8_t>((thunk_id >> 16) & 0xFF);
        stub[4] = static_cast<uint8_t>((thunk_id >> 24) & 0xFF);
        // ud2 (0F 0B) -> FEXCore Trap
        stub[5] = 0x0F;
        stub[6] = 0x0B;
        // ret (C3)
        stub[7] = 0xC3;

        current_offset += 8;
        return reinterpret_cast<uint64_t>(stub);
    }

private:
    GuestThunkTrampoline() = default;
    std::mutex stub_mutex;
    uint8_t* trampoline_page{nullptr};
    size_t current_offset{0};
};

} // namespace ShadPS4::Android::CPU
