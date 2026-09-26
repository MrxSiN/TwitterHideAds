/*
 * JNI surface of libtwitterbf: my.MrxSiN.twitterhideads.NativePolicy.
 *
 * One native call per policy decision. The request and response live in
 * direct ByteBuffers owned by the calling thread, so nothing is allocated
 * or copied across the boundary and no Java object ever reaches Brainfuck.
 */
#include <jni.h>

#include "bf_engine.h"

/* Returns the response length, or -status on failure. */
JNIEXPORT jint JNICALL
Java_my_MrxSiN_twitterhideads_NativePolicy_nativeRun(JNIEnv *env, jclass clazz, jint program,
                                                     jobject request, jint request_len,
                                                     jobject response)
{
    (void) clazz;
    if (request == NULL || response == NULL || request_len < 0) {
        return -BF_ERR_INPUT;
    }
    const uint8_t *req = (const uint8_t *) (*env)->GetDirectBufferAddress(env, request);
    uint8_t *out = (uint8_t *) (*env)->GetDirectBufferAddress(env, response);
    jlong req_cap = (*env)->GetDirectBufferCapacity(env, request);
    jlong out_cap = (*env)->GetDirectBufferCapacity(env, response);
    if (req == NULL || out == NULL || req_cap < request_len || out_cap <= 0) {
        return -BF_ERR_INPUT;
    }
    size_t out_len = 0;
    int status = bf_run(program, req, (size_t) request_len, out, (size_t) out_cap, &out_len, NULL);
    return status == BF_OK ? (jint) out_len : -status;
}

JNIEXPORT jint JNICALL
Java_my_MrxSiN_twitterhideads_NativePolicy_nativeAbi(JNIEnv *env, jclass clazz)
{
    (void) env;
    (void) clazz;
    return (BF_ABI_MAJOR << 8) | BF_ABI_MINOR;
}
