#ifndef SHADPS4_PAD_DATA_H
#define SHADPS4_PAD_DATA_H

#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef struct {
    uint32_t buttons;
    float left_stick_x;
    float left_stick_y;
    float right_stick_x;
    float right_stick_y;
    float l2_trigger;
    float r2_trigger;
} NativePadData;

void shadps4_input_update_pad(int pad_index, const NativePadData* data);

#ifdef __cplusplus
}
#endif

#endif // SHADPS4_PAD_DATA_H
