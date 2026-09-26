/*
 * One request in, one response out. The tape lives on the caller's stack, so
 * concurrent calls from different threads share nothing but the immutable
 * program table. See docs/BRAINFUCK_ARCHITECTURE.md.
 */
#ifndef TWITTERHIDEADS_BF_ENGINE_H
#define TWITTERHIDEADS_BF_ENGINE_H

#include <stddef.h>
#include <stdint.h>

#include "bf_runtime.h"
#include "generated/bf_abi.h"

#ifdef __cplusplus
extern "C" {
#endif

#define BF_TAPE_MAX BF_RUNTIME_TAPE_MAX
#define BF_IN_CAP BF_RUNTIME_IN_CAP
#define BF_OUT_CAP BF_RUNTIME_OUT_CAP

/*
 * Runs `program` over the request `in`. On BF_OK the response is in `out`
 * and *out_len holds its length; otherwise *out_len is 0. *ticks (optional)
 * receives the loop iterations used.
 */
int bf_run(int program, const uint8_t *in, size_t in_len, uint8_t *out, size_t out_cap,
           size_t *out_len, uint32_t *ticks);

#ifdef __cplusplus
}
#endif

#endif
