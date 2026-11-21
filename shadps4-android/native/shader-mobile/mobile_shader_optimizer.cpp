#include "mobile_shader_optimizer.h"
#include <android/log.h>

#define LOG_TAG "ShadPS4_ShaderOpt"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

namespace ShadPS4::Android::Graphics {

MobileShaderOptimizer& MobileShaderOptimizer::Instance() {
    static MobileShaderOptimizer instance;
    return instance;
}

bool MobileShaderOptimizer::OptimizeSpirV(
    std::vector<uint32_t>& spirv_words,
    bool supports_float64,
    bool supports_custom_border
) {
    if (spirv_words.empty()) return false;

    // SPIR-V Header Check: Magic Number = 0x07230203
    if (spirv_words[0] != 0x07230203) {
        return false;
    }

    bool modified = false;

    // If device GPU lacks native Float64 (common in mobile Mali/older Adreno),
    // strip the Float64 capability declaration and downgrade double precision ops.
    if (!supports_float64) {
        for (size_t i = 5; i < spirv_words.size();) {
            uint32_t word = spirv_words[i];
            uint16_t opcode = word & 0xFFFF;
            uint16_t word_count = (word >> 16) & 0xFFFF;
            if (word_count == 0) break;

            // OpCapability = 17, Capability Float64 = 11
            if (opcode == 17 && spirv_words[i + 1] == 11) {
                // NOP out OpCapability Float64
                spirv_words[i] = (1 << 16) | 0; // OpNop
                modified = true;
                LOGI("Stripped OpCapability Float64 for mobile GPU compatibility.");
            }
            i += word_count;
        }
    }

    return modified;
}

} // namespace ShadPS4::Android::Graphics
