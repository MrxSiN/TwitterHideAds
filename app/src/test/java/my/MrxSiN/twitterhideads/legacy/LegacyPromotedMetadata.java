package my.MrxSiN.twitterhideads.legacy;

// Frozen copy of the v2.1.0 Java policy: the behavioural oracle for the
// Brainfuck parity tests. Never edit it to match new behaviour.

/**
 * Recognises the promoted-metadata model by class name.
 *
 * X obfuscates the class away on recent releases, so the validated name is
 * only the fast path and any class whose simple name still ends in
 * {@code LegacyPromotedMetadata} counts as the same signal.
 */
public final class LegacyPromotedMetadata {
    public static final String VALIDATED_CLASS = "com.x.models.TimelinePromotedMetadata";

    private static final String SUFFIX = "PromotedMetadata";

    private LegacyPromotedMetadata() {
    }

    public static boolean isType(String className) {
        return className != null
                && (VALIDATED_CLASS.equals(className) || className.endsWith(SUFFIX));
    }

    public static boolean isInstance(Object value) {
        return value != null && isType(value.getClass().getName());
    }
}
