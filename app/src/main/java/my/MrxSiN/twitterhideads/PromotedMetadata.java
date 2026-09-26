package my.MrxSiN.twitterhideads;

/**
 * Recognises the promoted-metadata model by class name.
 *
 * X obfuscates the class away on recent releases, so the validated name is
 * only the fast path and any class whose simple name still ends in
 * {@code PromotedMetadata} counts as the same signal.
 */
final class PromotedMetadata {
    static final String VALIDATED_CLASS = "com.x.models.TimelinePromotedMetadata";

    private static final String SUFFIX = "PromotedMetadata";

    private PromotedMetadata() {
    }

    static boolean isType(String className) {
        return className != null
                && (VALIDATED_CLASS.equals(className) || className.endsWith(SUFFIX));
    }

    static boolean isInstance(Object value) {
        return value != null && isType(value.getClass().getName());
    }
}
