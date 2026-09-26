package my.MrxSiN.twitterhideads;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * One policy request: encodes normalized facts into a frame, runs a
 * Brainfuck program over it and validates the response.
 *
 * Each thread owns one frame with direct request and response buffers, so
 * the render hot path allocates nothing. A request started while the
 * thread's frame is in use (a hook re-entered from app code during encoding)
 * gets a temporary frame. Anything that goes wrong leaves {@link #send}
 * returning {@code false}, and the caller keeps the content.
 */
final class PolicyFrame {
    static final int MAX_REFS = 1024;
    private static final int INITIAL_REQUEST = 4096;

    private static final ThreadLocal<PolicyFrame> CURRENT = new ThreadLocal<PolicyFrame>() {
        @Override
        protected PolicyFrame initialValue() {
            return new PolicyFrame();
        }
    };

    private static final byte[] ENTRY_CODES = new byte[128];
    private static final byte[] ACTION_CODES = new byte[128];
    private static final byte[] VERSION_CODES = new byte[128];

    static {
        // Entry identifiers: letters are case folded; only the letters of
        // "promoted" and "tweet" are told apart, every other letter is one code.
        for (int c = 'a'; c <= 'z'; c++) {
            ENTRY_CODES[c] = (byte) BfAbi.EC_LETTER;
            ENTRY_CODES[c - 32] = (byte) BfAbi.EC_LETTER;
        }
        entryLetter('p', BfAbi.EC_P);
        entryLetter('r', BfAbi.EC_R);
        entryLetter('o', BfAbi.EC_O);
        entryLetter('m', BfAbi.EC_M);
        entryLetter('t', BfAbi.EC_T);
        entryLetter('e', BfAbi.EC_E);
        entryLetter('d', BfAbi.EC_D);
        entryLetter('w', BfAbi.EC_W);
        for (int c = '0'; c <= '9'; c++) {
            ENTRY_CODES[c] = (byte) BfAbi.EC_DIGIT;
            VERSION_CODES[c] = (byte) (BfAbi.VC_DIGIT_0 + c - '0');
        }
        ENTRY_CODES['-'] = (byte) BfAbi.EC_HYPHEN;
        ENTRY_CODES['_'] = (byte) BfAbi.EC_UNDERSCORE;

        // Action names are case sensitive; only their own letters are told apart.
        String letters = "PromtedDisAInfRp";
        int[] codes = {
                BfAbi.AC_UP_P, BfAbi.AC_R, BfAbi.AC_O, BfAbi.AC_M, BfAbi.AC_T, BfAbi.AC_E,
                BfAbi.AC_D, BfAbi.AC_UP_D, BfAbi.AC_I, BfAbi.AC_S, BfAbi.AC_UP_A, BfAbi.AC_UP_I,
                BfAbi.AC_N, BfAbi.AC_F, BfAbi.AC_UP_R, BfAbi.AC_P
        };
        for (int index = 0; index < letters.length(); index++) {
            ACTION_CODES[letters.charAt(index)] = (byte) codes[index];
        }
        ACTION_CODES['#'] = (byte) BfAbi.AC_HASH;

        // Version names: String.trim() removes every char up to U+0020.
        for (int c = 0; c < ' '; c++) {
            VERSION_CODES[c] = (byte) BfAbi.VC_CONTROL;
        }
        VERSION_CODES[' '] = (byte) BfAbi.VC_SPACE;
        VERSION_CODES['.'] = (byte) BfAbi.VC_DOT;
        VERSION_CODES['-'] = (byte) BfAbi.VC_HYPHEN;
        VERSION_CODES['+'] = (byte) BfAbi.VC_PLUS;
    }

    private static void entryLetter(char lower, int code) {
        ENTRY_CODES[lower] = (byte) code;
        ENTRY_CODES[lower - 32] = (byte) code;
    }

    /** Values sent in the current request, so a response index can name one. */
    final Object[] refs = new Object[MAX_REFS];
    int refCount;

    /** Encoded on the Java heap (fastest to fill), copied once per send. */
    private byte[] buffer = new byte[INITIAL_REQUEST];
    private ByteBuffer request = direct(INITIAL_REQUEST);
    private final ByteBuffer response = direct(BfAbi.RUNTIME_OUT_CAP);
    private int position;
    private int opcode;
    private int requestId;
    private int responseLength;
    private boolean overflow;
    private boolean busy;

    private PolicyFrame() {
    }

    private static ByteBuffer direct(int capacity) {
        return ByteBuffer.allocateDirect(capacity).order(ByteOrder.LITTLE_ENDIAN);
    }

    /** The calling thread's frame, started for {@code op}. Pair with {@link #release()}. */
    static PolicyFrame begin(int op) {
        PolicyFrame frame = CURRENT.get();
        if (frame.busy) {
            frame = new PolicyFrame();
        }
        frame.busy = true;
        frame.restart(op);
        return frame;
    }

    /** Starts another request on the same frame (a follow-up batch). */
    void restart(int op) {
        opcode = op;
        requestId = (requestId + 1) & 0xFFFF;
        position = BfAbi.HEADER_SIZE;
        overflow = false;
        clearRefs();
    }

    void release() {
        clearRefs();
        busy = false;
    }

    private void clearRefs() {
        for (int index = 0; index < refCount; index++) {
            refs[index] = null;
        }
        refCount = 0;
    }

    // ------------------------------------------------------------ encoding

    private boolean ensure(int bytes) {
        int needed = position + bytes;
        if (needed <= buffer.length) {
            return true;
        }
        if (needed > BfAbi.RUNTIME_IN_CAP) {
            overflow = true;
            return false;
        }
        int capacity = buffer.length;
        while (capacity < needed) {
            capacity *= 2;
        }
        buffer = java.util.Arrays.copyOf(buffer, Math.min(capacity, BfAbi.RUNTIME_IN_CAP));
        return true;
    }

    int position() {
        return position;
    }

    void u8(int value) {
        if (position < buffer.length || ensure(1)) {
            buffer[position++] = (byte) value;
        }
    }

    void bool(boolean value) {
        u8(value ? 1 : 0);
    }

    void u16(int value) {
        u8(value);
        u8(value >>> 8);
    }

    /** Overwrites a byte written earlier (a count known only afterwards). */
    void patch(int at, int value) {
        if (at < position) {
            buffer[at] = (byte) value;
        }
    }

    /** Remembers {@code value} as the next indexed reference. */
    boolean ref(Object value) {
        if (refCount >= MAX_REFS) {
            overflow = true;
            return false;
        }
        refs[refCount++] = value;
        return true;
    }

    /** Length (u16, saturated) then chunked entry-alphabet codes. */
    void entryText(CharSequence value) {
        int length = value.length();
        u16(Math.min(length, 0xFFFF));
        chunks(value, ENTRY_CODES, true);
    }

    /** Chunked action-alphabet codes. */
    void actionText(CharSequence value) {
        chunks(value, ACTION_CODES, false);
    }

    /** Chunked version-alphabet codes. */
    void versionText(CharSequence value) {
        chunks(value, VERSION_CODES, false);
    }

    private void chunks(CharSequence value, byte[] table, boolean entry) {
        int length = value.length();
        if (!ensure(length + length / BfAbi.MAX_CHUNK + 2)) {
            return;
        }
        byte[] out = buffer;
        int at = position;
        int index = 0;
        while (index < length) {
            int chunk = Math.min(BfAbi.MAX_CHUNK, length - index);
            out[at++] = (byte) chunk;
            for (int end = index + chunk; index < end; index++) {
                char c = value.charAt(index);
                byte code;
                if (c < 128) {
                    code = table[c];
                } else if (entry && c == 'K') {
                    // KELVIN SIGN is the one non-ASCII char whose Locale.ROOT
                    // lower case is an ASCII letter (k).
                    code = (byte) BfAbi.EC_LETTER;
                } else {
                    code = 0;
                }
                out[at++] = code;
            }
        }
        out[at++] = 0;
        position = at;
    }

    // ------------------------------------------------------------ execution

    /**
     * Runs {@code program}; true when a well-formed response with exactly
     * {@code expectedLength} payload bytes came back.
     */
    boolean send(int program, int expectedLength) {
        responseLength = 0;
        if (overflow || !NativePolicy.isAvailable()) {
            return PolicyStats.failed(opcode, overflow ? PolicyStats.OVERFLOW : PolicyStats.UNAVAILABLE);
        }
        byte[] bytes = buffer;
        int payload = position - BfAbi.HEADER_SIZE;
        bytes[0] = (byte) BfAbi.ABI_MAJOR;
        bytes[1] = (byte) BfAbi.ABI_MINOR;
        bytes[2] = (byte) opcode;
        bytes[3] = 0;
        bytes[4] = (byte) payload;
        bytes[5] = (byte) (payload >>> 8);
        bytes[6] = (byte) requestId;
        bytes[7] = (byte) (requestId >>> 8);
        if (request.capacity() < position) {
            request = direct(Math.max(position, request.capacity() * 2));
        }
        ByteBuffer out = request;
        out.clear();
        out.put(bytes, 0, position);
        long started = PolicyStats.TIMING ? System.nanoTime() : 0L;
        int length;
        try {
            length = NativePolicy.nativeRun(program, out, position, response);
        } catch (Throwable throwable) {
            return PolicyStats.failed(opcode, PolicyStats.NATIVE);
        }
        if (PolicyStats.TIMING) {
            PolicyStats.timed(System.nanoTime() - started);
        }
        if (length < 0) {
            return PolicyStats.failed(opcode, PolicyStats.NATIVE);
        }
        ByteBuffer in = response;
        if (length != BfAbi.HEADER_SIZE + expectedLength
                || in.get(0) != BfAbi.ABI_MAJOR
                || (in.get(2) & 0xFF) != (opcode | BfAbi.RESPONSE_BIT)
                || in.get(3) != BfAbi.ST_OK
                || (in.getShort(4) & 0xFFFF) != expectedLength
                || (in.getShort(6) & 0xFFFF) != requestId) {
            return PolicyStats.failed(opcode, PolicyStats.MALFORMED);
        }
        responseLength = expectedLength;
        PolicyStats.ok();
        return true;
    }

    /** Payload byte {@code index} of the last valid response. */
    int out(int index) {
        return index < responseLength ? response.get(BfAbi.HEADER_SIZE + index) & 0xFF : 0;
    }

    int out16(int index) {
        return out(index) | (out(index + 1) << 8);
    }
}
