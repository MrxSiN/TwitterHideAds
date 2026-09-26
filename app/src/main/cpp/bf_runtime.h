/*
 * Execution contract shared by every AOT-compiled Brainfuck program.
 *
 * A program is a C function over a fixed tape of unsigned 8-bit wrapping
 * cells. `,` reads the next request byte and yields 0 once the request is
 * exhausted; `.` appends to a bounded response buffer; every loop iteration
 * charges a per-request budget. See docs/BRAINFUCK_ARCHITECTURE.md.
 *
 * Adapted from ThreadsHideAds (app/src/main/cpp/bf_runtime.h at 358373d):
 * TwitterHideAds programs are stateless and never call back into the host,
 * so there is no refill, no capability and no persistent region.
 */
#ifndef TWITTERHIDEADS_BF_RUNTIME_H
#define TWITTERHIDEADS_BF_RUNTIME_H

#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

enum {
    BF_OK = 0,
    BF_ERR_TAPE = 1,
    BF_ERR_OUTPUT = 2,
    BF_ERR_LIMIT = 3,
    BF_ERR_PROGRAM = 4,
    BF_ERR_INPUT = 5
};

typedef struct {
    const uint8_t *in;
    size_t in_len;
    size_t in_pos;
    uint8_t *out;
    size_t out_len;
    size_t out_cap;
    uint32_t budget;          /* loop iterations left */
    int status;
} bf_io;

#define BF_IN(io) ((io)->in_pos < (io)->in_len ? (io)->in[(io)->in_pos++] : 0)

#define BF_OUT(io, v)                                        \
    do {                                                     \
        if ((io)->out_len < (io)->out_cap) {                 \
            (io)->out[(io)->out_len++] = (uint8_t) (v);      \
        } else {                                             \
            (io)->status = BF_ERR_OUTPUT;                    \
        }                                                    \
    } while (0)

/* Charged once per iteration of every `while` loop the generator emits. */
#define BF_TICK(io)                                          \
    do {                                                     \
        if ((io)->budget-- == 0) {                           \
            return BF_ERR_LIMIT;                             \
        }                                                    \
    } while (0)

#define BF_PROGRAM(name) int bf_prog_##name(uint8_t *restrict t, bf_io *restrict io)

typedef int (*bf_program_fn)(uint8_t *tape, bf_io *io);

typedef struct {
    const char *name;
    bf_program_fn run;
    uint16_t tape_size;
} bf_program_info;

/* Generated table, indexed by program id. */
extern const bf_program_info bf_programs[];
extern const int bf_program_count;

#ifdef __cplusplus
}
#endif

#endif
