#pragma once

#include <vector>
#include <cstdint>

namespace ShadPS4::Android::Graphics {

/**
 * Mobile-specific shader optimizations for Adreno / Mali GPUs.
 */
class MobileShaderOptimizer {
public:
    static MobileShaderOptimizer& Instance();

    /**
     * Inspects and patches SPIR-V binary words:
     * - Rewrites 64-bit float instructions to 32-bit float emulation on GPUs lacking Float64.
     * - Injects bounds checking fallback for Custom Border Color if unsupported.
     * - Applies register pressure reduction heuristics.
     */
    bool OptimizeSpirV(std::vector<uint32_t>& spirv_words, bool supports_float64, bool supports_custom_border);
};

} // namespace ShadPS4::Android::Graphics
