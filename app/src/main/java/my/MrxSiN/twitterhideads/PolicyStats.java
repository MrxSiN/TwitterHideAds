package my.MrxSiN.twitterhideads;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Brainfuck policy counters. Failures are always counted (they are rare);
 * call timing is measured only in debug builds (or -PpolicyTiming builds), so
 * production pays for one counter increment per request.
 */
final class PolicyStats {
    static final boolean TIMING = BuildConfig.DEBUG || BuildConfig.POLICY_TIMING;

    static final int UNAVAILABLE = 0;
    static final int OVERFLOW = 1;
    static final int NATIVE = 2;
    static final int MALFORMED = 3;

    private static final AtomicLong CALLS = new AtomicLong();
    private static final AtomicLong[] FAILURES = {
            new AtomicLong(), new AtomicLong(), new AtomicLong(), new AtomicLong()
    };
    private static final AtomicLong TIMED_NANOS = new AtomicLong();
    private static final AtomicLong TIMED_CALLS = new AtomicLong();
    private static final AtomicLong MAX_NANOS = new AtomicLong();
    private static final AtomicLong FALLBACKS = new AtomicLong();
    private static final AtomicLong BLOCKED = new AtomicLong();

    private PolicyStats() {
    }

    static void ok() {
        CALLS.incrementAndGet();
    }

    /** Counts a failed request and returns false, so callers can fail open in one line. */
    static boolean failed(int opcode, int kind) {
        CALLS.incrementAndGet();
        long count = FAILURES[kind].incrementAndGet();
        if (count <= 3 || Long.bitCount(count) == 1) {
            ModuleRuntime.log("Brainfuck policy request failed open: op=0x" + Integer.toHexString(opcode)
                    + ", kind=" + kindName(kind) + ", count=" + count
                    + (kind == UNAVAILABLE ? ", reason=" + NativePolicy.failure() : ""));
        }
        return false;
    }

    static void timed(long nanos) {
        TIMED_NANOS.addAndGet(nanos);
        TIMED_CALLS.incrementAndGet();
        long max;
        while (nanos > (max = MAX_NANOS.get()) && !MAX_NANOS.compareAndSet(max, nanos)) {
            // retry
        }
    }

    /** Starts steady-state timing: discovery-time requests are left out. */
    static void resetTiming() {
        TIMED_NANOS.set(0);
        TIMED_CALLS.set(0);
        MAX_NANOS.set(0);
    }

    static void fallback() {
        FALLBACKS.incrementAndGet();
    }

    static void blocked() {
        BLOCKED.incrementAndGet();
    }

    static long calls() {
        return CALLS.get();
    }

    static long failures() {
        long total = 0;
        for (AtomicLong failure : FAILURES) {
            total += failure.get();
        }
        return total;
    }

    static String summary() {
        long timed = TIMED_CALLS.get();
        return "bfCalls=" + CALLS.get()
                + ", bfFailures=" + failures()
                + ", malformed=" + FAILURES[MALFORMED].get()
                + ", fallbackScans=" + FALLBACKS.get()
                + ", blocked=" + BLOCKED.get()
                + (timed > 0 ? ", avgNanos=" + TIMED_NANOS.get() / timed + ", maxNanos=" + MAX_NANOS.get() : "");
    }

    private static String kindName(int kind) {
        switch (kind) {
            case UNAVAILABLE:
                return "library-unavailable";
            case OVERFLOW:
                return "request-too-large";
            case NATIVE:
                return "native-error";
            default:
                return "malformed-response";
        }
    }
}
