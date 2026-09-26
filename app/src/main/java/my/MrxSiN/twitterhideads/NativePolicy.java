package my.MrxSiN.twitterhideads;

import java.nio.ByteBuffer;

import dalvik.annotation.optimization.FastNative;

/**
 * libtwitterbf: the AOT-compiled Brainfuck policy programs.
 *
 * The library is loaded once. When it cannot be loaded, or reports another
 * ABI, every policy request fails and the module stays pass-through.
 */
final class NativePolicy {
    private static volatile boolean available;
    private static volatile String failure = "not loaded";

    private NativePolicy() {
    }

    /** Loads the library shipped in the module APK. Idempotent. */
    static synchronized boolean load() {
        if (available) {
            return true;
        }
        try {
            System.loadLibrary("twitterbf");
            return verify();
        } catch (Throwable throwable) {
            failure = Reflect.describeThrowable(throwable);
            return false;
        }
    }

    /** Host tests: the library was loaded with {@link System#load(String)}. */
    static synchronized boolean verify() {
        try {
            int abi = nativeAbi();
            int expected = (BfAbi.ABI_MAJOR << 8) | BfAbi.ABI_MINOR;
            if (abi != expected) {
                failure = "ABI mismatch: native=" + abi + ", java=" + expected;
                available = false;
                return false;
            }
            available = true;
            failure = null;
            return true;
        } catch (Throwable throwable) {
            failure = Reflect.describeThrowable(throwable);
            available = false;
            return false;
        }
    }

    static boolean isAvailable() {
        return available;
    }

    static String failure() {
        return failure;
    }

    /**
     * Returns the response length, or a negative runtime status.
     *
     * {@code @FastNative}: the call is short, bounded by the loop budget and
     * never blocks, so ART may skip the thread-state transition. The JNI
     * signature is unchanged, so a runtime that ignores the annotation (or
     * the host JVM running the tests) calls the same entry point.
     */
    @FastNative
    static native int nativeRun(int program, ByteBuffer request, int requestLength, ByteBuffer response);

    static native int nativeAbi();
}
