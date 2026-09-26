package my.MrxSiN.twitterhideads;

/**
 * Keeps browsing-related identifiers out of framework logs, which users export
 * when reporting issues. Debug builds of the module log them verbatim. The
 * identifier grammar itself is policy and lives in brainfuck/src/post.bf.
 */
final class LogPrivacy {
    static final boolean VERBATIM_IDS = BuildConfig.DEBUG;

    private LogPrivacy() {
    }

    static String entryId(String entryId) {
        if (entryId == null || entryId.isEmpty()) {
            return "unknown";
        }
        if (VERBATIM_IDS) {
            return entryId.length() > 96 ? entryId.substring(0, 96) : entryId;
        }
        return redact(entryId);
    }

    /**
     * Log-safe form of an entry identifier: the grammar is kept, every segment
     * that carries a digit is replaced, so no post or module id is written.
     */
    static String redact(String entryId) {
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
            out.append(segment.chars().anyMatch(Character::isDigit) ? "<n>" : segment);
        }
        return out.toString();
    }
}
