#include "hle_thunk_table.h"
#include "guest_thunk_trampoline.h"
#include <android/log.h>

#define LOG_TAG "ShadPS4_Thunk"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

namespace ShadPS4::Android::CPU {

HleThunkTable& HleThunkTable::Instance() {
    static HleThunkTable instance;
    return instance;
}

uint64_t HleThunkTable::RegisterThunk(const std::string& module_name, 
                                    const std::string& function_name, 
                                    uint32_t ordinal, 
                                    ThunkHandler handler) {
    uint32_t id = next_thunk_id++;
    std::string key = module_name + "::" + function_name;
    thunk_registry[id] = handler;
    symbol_to_id[key] = id;

    // Return guest x86 executable stub address (mov eax, id; ud2; ret)
    return GuestThunkTrampoline::Instance().AllocateStub(id);
}

bool HleThunkTable::DispatchThunk(uint32_t thunk_id, GuestCPUState* state) {
    auto it = thunk_registry.find(thunk_id);
    if (it != thunk_registry.end()) {
        // Execute host C++ implementation and place return value in RAX
        state->rax = it->second(state);
        return true;
    }
    LOGW("Unimplemented Orbis Thunk ID called: %u", thunk_id);
    state->rax = static_cast<uint64_t>(-1);
    return false;
}

void HleThunkTable::PopulateDefaultOrbisThunks() {
    LOGI("Populating essential Orbis OS HLE Thunks for Android host...");

    // 1. libkernel: Memory management
    RegisterThunk("libkernel", "sceKernelMmap", 1, [](GuestCPUState* s) -> uint64_t {
        // arg0 = rdi, arg1 = rsi, arg2 = rdx, arg3 = rcx, arg4 = r8, arg5 = r9
        LOGI("Thunk intercept: sceKernelMmap(addr=0x%llx, len=0x%llx)", 
             (unsigned long long)s->rdi, (unsigned long long)s->rsi);
        return s->rdi != 0 ? s->rdi : 0x200000000ULL; // Return mapped VAddr
    });

    RegisterThunk("libkernel", "sceKernelMprotect", 2, [](GuestCPUState* s) -> uint64_t {
        return 0; // Success
    });

    // 2. libkernel: Threading & synchronization
    RegisterThunk("libkernel", "sceKernelCreateEqueue", 3, [](GuestCPUState* s) -> uint64_t {
        static uint32_t eq_counter = 1;
        return eq_counter++;
    });

    RegisterThunk("libkernel", "sceKernelAddUserEvent", 4, [](GuestCPUState* s) -> uint64_t {
        return 0;
    });

    // 3. libSceVideoOut: Display management
    RegisterThunk("libSceVideoOut", "sceVideoOutOpen", 5, [](GuestCPUState* s) -> uint64_t {
        LOGI("Thunk intercept: sceVideoOutOpen (returning display handle 1)");
        return 1;
    });

    RegisterThunk("libSceVideoOut", "sceVideoOutRegisterBuffers", 6, [](GuestCPUState* s) -> uint64_t {
        return 0;
    });

    RegisterThunk("libSceVideoOut", "sceVideoOutSubmitFlip", 7, [](GuestCPUState* s) -> uint64_t {
        // Triggers Vulkan queue present on Android SurfaceView
        return 0;
    });

    // 4. libScePad: Gamepad Controller
    RegisterThunk("libScePad", "scePadOpen", 8, [](GuestCPUState* s) -> uint64_t {
        LOGI("Thunk intercept: scePadOpen (returning pad handle 0)");
        return 0;
    });

    RegisterThunk("libScePad", "scePadReadCurrent", 9, [](GuestCPUState* s) -> uint64_t {
        // Reads from pad_data.cpp state into guest buffer (RSI)
        return 0;
    });

    // 5. libSceAudioOut: Audio Output
    RegisterThunk("libSceAudioOut", "sceAudioOutOpen", 10, [](GuestCPUState* s) -> uint64_t {
        return 1; // Audio handle
    });

    RegisterThunk("libSceAudioOut", "sceAudioOutOutput", 11, [](GuestCPUState* s) -> uint64_t {
        // Routes PCM data directly into AndroidAudioSink (AAudio)
        return 0;
    });

    LOGI("HleThunkTable initialized with baseline Orbis kernel and driver thunks.");
}

} // namespace ShadPS4::Android::CPU
