package my.MrxSiN.twitterhideads;

/**
 * Loads libtwitterbf: on the JVM the host build (Gradle task buildHostCore: the same
 * AOT C, runtime and JNI glue the APK ships, compiled by the host compiler).
 */
public final class HostCore {
    private static boolean loaded;

    private HostCore() {
    }

    public static synchronized void ensure() {
        if (loaded) {
            return;
        }
        String path = System.getProperty("twitterbf.hostlib");
        if (path == null) {
            // On a device (connectedAndroidTest) the APK's own libtwitterbf is used.
            if (!NativePolicy.load()) {
                throw new IllegalStateException("libtwitterbf unavailable: " + NativePolicy.failure());
            }
            loaded = true;
            return;
        }
        System.load(path);
        if (!NativePolicy.verify()) {
            throw new IllegalStateException("host core rejected: " + NativePolicy.failure());
        }
        loaded = true;
    }

    public static int parityCases() {
        return Integer.getInteger("twitterbf.parityCases", 20000);
    }

    /** EntryIds.kind through post.bf: 0 none, 1 organic, 2 promoted. */
    static int kind(CharSequence value) {
        int[] result = classify(new CharSequence[]{value}, false, false, false);
        return result[0] == BfAbi.V_PROMOTED ? 2 : result[0] == BfAbi.V_ORGANIC ? 1 : 0;
    }

    /** EntryIds.mentionsPromoted, observed through the ambiguity rule. */
    static boolean mentionsPromoted(CharSequence value) {
        if (kind(value) != 0) {
            throw new IllegalArgumentException("only observable for non-identifiers");
        }
        int[] result = classify(new CharSequence[]{"tweet-1", value}, false, false, false);
        return result[0] != BfAbi.V_ORGANIC;
    }

    /** Raw OP_CLASSIFY_POST: {verdict, entry index, signals}, or {-1} on failure. */
    static int[] classify(CharSequence[] fields, boolean validated, boolean allowFallback, boolean metadata) {
        ensure();
        PolicyFrame frame = PolicyFrame.begin(BfAbi.OP_CLASSIFY_POST);
        try {
            frame.bool(validated);
            frame.bool(allowFallback);
            frame.bool(metadata);
            frame.u8(fields.length);
            for (CharSequence field : fields) {
                frame.entryText(field);
            }
            if (!frame.send(BfAbi.PROG_POST, 3)) {
                return new int[]{-1};
            }
            return new int[]{frame.out(0), frame.out(1), frame.out(2)};
        } finally {
            frame.release();
        }
    }
}
