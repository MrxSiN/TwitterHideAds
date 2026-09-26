package my.MrxSiN.twitterhideads.legacy;

// Frozen copy of the v2.1.0 Java policy: the behavioural oracle for the
// Brainfuck parity tests. Never edit it to match new behaviour.

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Grammar of URT timeline entry identifiers.
 *
 * An entry is a lowercase, "-" separated identifier whose post segment is
 * either {@code tweet-<id>...} or {@code promoted-<type>-<id>...}. Surfaces
 * that nest a post inside a module prefix the module's own entry, which always
 * ends in that module's numeric id:
 *
 *   tweet-<id>
 *   promoted-tweet-<id>-<hash>
 *   conversationthread-<id>-promoted-tweet-<id>-<hash>
 *   search-conversation-<id>-promoted-tweet-<id>-<hash>
 *
 * The whole value has to match, so free text, "not-promoted-x",
 * "unpromoted-1" or a bare "promoted-deal" never classify as an entry.
 */
public final class LegacyEntryIds {
    public enum Kind {
        PROMOTED,
        ORGANIC,
        /** Not an entry identifier this grammar recognises. */
        NONE
    }

    /** Zero or more nesting modules, each ending in its numeric id. */
    private static final String MODULE_PREFIX = "(?:[a-z]+(?:-[a-z]+)*-\\d+-)*";
    private static final String TRAILER = "(?:-[a-z0-9_]+)*";

    private static final Pattern PROMOTED = Pattern.compile(
            MODULE_PREFIX + "promoted-[a-z]+(?:-[a-z]+)*-\\d+" + TRAILER
    );
    private static final Pattern ORGANIC = Pattern.compile(
            MODULE_PREFIX + "tweet-\\d+" + TRAILER
    );

    private static final int MAX_LENGTH = 256;

    private LegacyEntryIds() {
    }

    public static Kind kind(CharSequence value) {
        if (value == null || value.length() == 0 || value.length() > MAX_LENGTH) {
            return Kind.NONE;
        }
        String lower = value.toString().toLowerCase(Locale.ROOT);
        if (PROMOTED.matcher(lower).matches()) {
            return Kind.PROMOTED;
        }
        if (ORGANIC.matcher(lower).matches()) {
            return Kind.ORGANIC;
        }
        return Kind.NONE;
    }

    /**
     * True for text that mentions the promoted token without matching the
     * grammar. Such a model is not confidently organic.
     */
    public static boolean mentionsPromoted(CharSequence value) {
        return value != null
                && value.toString().toLowerCase(Locale.ROOT).contains("promoted");
    }

    /**
     * Log-safe form of an entry identifier: the grammar is kept, every segment
     * that carries a digit is replaced, so no post or module id is written.
     */
    public static String redact(String entryId) {
        if (entryId == null || entryId.isEmpty()) {
            return "unknown";
        }
        String[] segments = entryId.split("-", -1);
        StringBuilder out = new StringBuilder();
        for (int index = 0; index < segments.length && out.length() < 96; index++) {
            if (index > 0) {
                out.append('-');
            }
            String segment = segments[index];
            out.append(containsDigit(segment) ? "<n>" : segment);
        }
        return out.toString();
    }

    private static boolean containsDigit(String value) {
        for (int index = 0; index < value.length(); index++) {
            if (Character.isDigit(value.charAt(index))) {
                return true;
            }
        }
        return false;
    }
}
