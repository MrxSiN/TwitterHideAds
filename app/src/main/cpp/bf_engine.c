#include "bf_engine.h"

#include <string.h>

int bf_run(int program, const uint8_t *in, size_t in_len, uint8_t *out, size_t out_cap,
           size_t *out_len, uint32_t *ticks)
{
    *out_len = 0;
    if (ticks != NULL) {
        *ticks = 0;
    }
    if (program < 0 || program >= bf_program_count || in == NULL || out == NULL) {
        return BF_ERR_PROGRAM;
    }
    if (in_len > BF_IN_CAP) {
        return BF_ERR_INPUT;
    }
    const bf_program_info *info = &bf_programs[program];
    if (info->tape_size > BF_TAPE_MAX) {
        return BF_ERR_PROGRAM;
    }
    uint8_t tape[BF_TAPE_MAX];
    memset(tape, 0, info->tape_size);
    uint32_t budget = BF_BUDGET_BASE + (uint32_t) in_len * BF_BUDGET_PER_BYTE;
    bf_io io = {in, in_len, 0, out, 0, out_cap < BF_OUT_CAP ? out_cap : BF_OUT_CAP, budget, BF_OK};
    int status = info->run(tape, &io);
    if (ticks != NULL) {
        *ticks = budget - io.budget;
    }
    if (status == BF_OK) {
        status = io.status;
    }
    if (status == BF_OK) {
        *out_len = io.out_len;
    }
    return status;
}
