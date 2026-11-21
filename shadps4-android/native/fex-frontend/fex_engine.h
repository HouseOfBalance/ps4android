#pragma once

#include <cstdint>
#include <memory>
#include <string>

namespace FEXCore::Context {
class Context;
}

namespace FEXCore::Core {
struct CPUState;
}

namespace ShadPS4::Android::CPU {

/**
 * Embedded FEXCore Execution Engine.
 * Replaces direct x86 'jmp *entry' with a high-performance JIT Dynarec on ARM64.
 */
class FexEngine {
public:
    static FexEngine& Instance();

    bool Initialize(bool enable_tso, bool enable_vector_tso);
    void Shutdown();

    /**
     * Prepares guest thread context with PS4 SYSV ABI registers:
     * - RDI: EntryParams pointer
     * - RSI: ProgramExitFunc
     * - RSP: Aligned 16-byte stack misaligned by 8 for Orbis libc
     * - FS_BASE: Static TLS base for guest module
     */
    bool SetupThreadContext(uint64_t entry_addr, void* params, void* exit_func, uint64_t tls_base);

    /**
     * Executes guest thread in JIT recompiler loop.
     */
    void RunThread();

    /**
     * Thunk Dispatcher: Maps guest HLE syscalls/libraries (libkernel, libSceVideoOut, etc.)
     * from x86 SysV ABI calling convention into native ARM64 host functions.
     */
    static void RegisterHleThunks();

private:
    FexEngine() = default;
    ~FexEngine() = default;

    std::unique_ptr<FEXCore::Context::Context> ctx;
    bool is_initialized{false};
};

} // namespace ShadPS4::Android::CPU
