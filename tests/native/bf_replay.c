/*
 * Replays program invocations against the AOT-compiled programs through the
 * same entry point the JNI bridge uses (bf_run).
 *
 * Input file, repeated:  u32 program, u32 input_len, input bytes
 * Output file, repeated: u32 status, u32 ticks used, u32 output_len, output bytes
 * All integers little-endian.
 *
 * Adapted from ThreadsHideAds (tests/native/bf_replay.c at 358373d).
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "bf_engine.h"

static int read_u32(FILE *f, uint32_t *v)
{
    uint8_t b[4];
    if (fread(b, 1, 4, f) != 4) {
        return 0;
    }
    *v = (uint32_t) b[0] | ((uint32_t) b[1] << 8) | ((uint32_t) b[2] << 16) | ((uint32_t) b[3] << 24);
    return 1;
}

static void write_u32(FILE *f, uint32_t v)
{
    uint8_t b[4] = {(uint8_t) v, (uint8_t) (v >> 8), (uint8_t) (v >> 16), (uint8_t) (v >> 24)};
    fwrite(b, 1, 4, f);
}

int main(int argc, char **argv)
{
    if (argc != 3) {
        fprintf(stderr, "usage: bf_replay <cases> <results>\n");
        return 2;
    }
    FILE *in = fopen(argv[1], "rb");
    FILE *out = fopen(argv[2], "wb");
    if (in == NULL || out == NULL) {
        return 2;
    }
    static uint8_t output[BF_OUT_CAP];
    uint32_t program;
    while (read_u32(in, &program)) {
        uint32_t input_len;
        if (!read_u32(in, &input_len)) {
            return 3;
        }
        uint8_t *input = malloc(input_len ? input_len : 1);
        if (input == NULL || fread(input, 1, input_len, in) != input_len) {
            return 3;
        }
        size_t out_len = 0;
        uint32_t ticks = 0;
        int status = bf_run((int) program, input, input_len, output, sizeof output, &out_len, &ticks);
        write_u32(out, (uint32_t) status);
        write_u32(out, ticks);
        write_u32(out, (uint32_t) out_len);
        fwrite(output, 1, out_len, out);
        free(input);
    }
    fclose(out);
    fclose(in);
    return 0;
}
