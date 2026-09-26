package my.MrxSiN.twitterhideads;

/**
 * Discovery and traversal bounds, owned by {@code brainfuck/src/discovery.bf}
 * (OP_LIMITS) and read once. If the request fails every bound is zero (the
 * witness window one), which disables hooks and scans: the module fails open.
 */
final class PolicyLimits {
    private static volatile int[] values;

    private PolicyLimits() {
    }

    private static int[] values() {
        int[] current = values;
        if (current != null) {
            return current;
        }
        int[] loaded = new int[BfAbi.LIMITS_SIZE];
        boolean ok;
        PolicyFrame frame = PolicyFrame.begin(BfAbi.OP_LIMITS);
        try {
            ok = frame.send(BfAbi.PROG_DISCOVERY, BfAbi.LIMITS_SIZE);
            for (int index = 0; ok && index < loaded.length; index++) {
                loaded[index] = frame.out(index);
            }
        } finally {
            frame.release();
        }
        if (!ok) {
            loaded = new int[BfAbi.LIMITS_SIZE];
            loaded[BfAbi.LIM_WITNESS_WINDOW] = 1;
            return loaded; // not cached: a later request may succeed
        }
        values = loaded;
        return loaded;
    }

    private static int u8(int offset) {
        return values()[offset];
    }

    private static int u16(int offset) {
        int[] v = values();
        return v[offset] | (v[offset + 1] << 8);
    }

    static int witnessWindow() {
        return Math.max(1, u8(BfAbi.LIM_WITNESS_WINDOW));
    }

    static int maxBoundaries() {
        return u8(BfAbi.LIM_MAX_BOUNDARIES);
    }

    static int maxCallers() {
        return u8(BfAbi.LIM_MAX_CALLERS);
    }

    static int scanDepth() {
        return u8(BfAbi.LIM_SCAN_DEPTH);
    }

    static int scanObjects() {
        return u8(BfAbi.LIM_SCAN_OBJECTS);
    }

    static int scanItems() {
        return u8(BfAbi.LIM_SCAN_ITEMS);
    }

    static int stackFrames() {
        return u8(BfAbi.LIM_STACK_FRAMES);
    }

    static int candidatesLogged() {
        return u8(BfAbi.LIM_CANDIDATE_LOG);
    }

    static int reflectionClasses() {
        return u16(BfAbi.LIM_REFLECTION_CLASSES);
    }

    static int videoClasses() {
        return u16(BfAbi.LIM_VIDEO_CLASSES);
    }
}
