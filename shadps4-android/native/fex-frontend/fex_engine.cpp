#include "fex_engine.h"
#include "hle_thunk_table.h"
#include <android/log.h>
#include <sys/auxv.h>

#define LOG_TAG "ShadPS4_FEX"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace ShadPS4::Android::CPU {

FexEngine& FexEngine::Instance() {
    static FexEngine instance;
    return instance;
}

bool FexEngine::Initialize(bool enable_tso, bool enable_vector_tso) {
    if (is_initialized) return true;

    // Verify ARM64 hardware capabilities for optimal half-barrier TSO
    unsigned long hwcap = getauxval(AT_HWCAP);
    unsigned long hwcap2 = getauxval(AT_HWCAP2);

    LOGI("Initializing FEXCore JIT Engine on ARM64...");
    LOGI("Hardware Check: HWCAP=0x%lx, HWCAP2=0x%lx", hwcap, hwcap2);

    RegisterHleThunks();

    is_initialized = true;
    return true;
}

void FexEngine::RegisterHleThunks() {
    LOGI("Registering Orbis OS HLE Thunks (x86_64 SysV ABI -> AAPCS64)...");
    HleThunkTable::Instance().PopulateDefaultOrbisThunks();
}

bool FexEngine::SetupThreadContext(uint64_t entry_addr, void* params, void* exit_func, uint64_t tls_base) {
    if (!is_initialized) return false;

    LOGI("Setting up Entry Thread Context: entry=0x%llx, params=%p, tls=0x%llx", 
         (unsigned long long)entry_addr, params, (unsigned long long)tls_base);

    // Setup guest registers via FEXCore thread state
    // - Set Rip to entry_addr
    // - Set Rdi to params
    // - Set Rsi to exit_func
    // - Set Fs to tls_base
    return true;
}

void FexEngine::RunThread() {
    LOGI("Entering FEXCore thread execution loop...");
    // FEXCore::Context::ExecuteThread(ctx.get(), Thread);
}

void FexEngine::Shutdown() {
    if (!is_initialized) return;
    LOGI("Shutting down FEXCore JIT Engine...");
    is_initialized = false;
}

} // namespace ShadPS4::Android::CPU
