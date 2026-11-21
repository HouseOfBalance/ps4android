#pragma once

#include <cstdint>
#include <string>
#include <unordered_map>
#include <functional>

namespace ShadPS4::Android::CPU {

/**
 * Guest x86_64 CPU Context mapped to Host ARM64 AAPCS64 calling convention.
 */
struct GuestCPUState {
    uint64_t rax, rbx, rcx, rdx, rsi, rdi, rbp, rsp;
    uint64_t r8, r9, r10, r11, r12, r13, r14, r15;
    uint64_t rip;
    uint64_t rflags;
    uint64_t fs_base;
    uint64_t gs_base;
};

using ThunkHandler = std::function<uint64_t(GuestCPUState*)>;

class HleThunkTable {
public:
    static HleThunkTable& Instance();

    uint64_t RegisterThunk(const std::string& module_name, const std::string& function_name, uint32_t ordinal, ThunkHandler handler);
    bool DispatchThunk(uint32_t thunk_id, GuestCPUState* state);
    void PopulateDefaultOrbisThunks();

private:
    HleThunkTable() = default;
    ~HleThunkTable() = default;

    std::unordered_map<uint32_t, ThunkHandler> thunk_registry;
    std::unordered_map<std::string, uint32_t> symbol_to_id;
    uint32_t next_thunk_id{1};
};

} // namespace ShadPS4::Android::CPU
