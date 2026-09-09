package my.MrxSiN.twitterhideads;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Advertisement-classification rules shipped inside the APK.
 *
 * Validated X 12.7.1 and X 12.8.0 render models expose two ad-specific properties:
 *  - an entry ID carrying the URT "promoted-" token;
 *  - a direct com.x.models.TimelinePromotedMetadata field.
 *
 * Only the Home timeline names a promoted entry "promoted-tweet-...". Every
 * surface that nests a post inside a module prefixes that module's own entry,
 * so the same advertisement arrives as
 * "conversationthread-<id>-promoted-tweet-<id>-<hash>" in a post detail and as
 * "search-conversation-<id>-promoted-tweet-<id>-<hash>" in search. The token is
 * therefore matched at any segment boundary rather than only at the start.
 *
 * The promoted PostActionType values discovered by the menu probes are retained
 * in {@link PromotedActionScanner} as a compatibility fallback for an unfamiliar
 * model shape. No user-specific advertiser or post IDs are required.
 */
final class BundledAdPatterns {
    static final String SCHEMA_VERSION = "7";
    static final String PROMOTED_METADATA_CLASS =
            "com.x.models.TimelinePromotedMetadata";

    /** URT names every promoted entry with this token, wherever it is nested. */
    private static final String PROMOTED_ENTRY_TOKEN = "promoted-";

    private static final String PROMOTED_METADATA_SUFFIX = "PromotedMetadata";

    private static final ConcurrentHashMap<Class<?>, DirectAccess> DIRECT_ACCESS =
            new ConcurrentHashMap<>();

    private BundledAdPatterns() {
    }

    static Classification classifyTimelinePost(
            Object model,
            String expectedRenderModel,
            boolean allowActionFallback
    ) {
        if (model == null) {
            return Classification.NORMAL;
        }

        DirectAccess access = DIRECT_ACCESS.computeIfAbsent(
                model.getClass(),
                DirectAccess::resolve
        );

        String entryId = null;
        boolean promotedEntry = false;
        boolean promotedMetadata = false;

        for (Field field : access.stringFields) {
            Object value = Reflect.read(field, model);
            if (!(value instanceof CharSequence)) {
                continue;
            }
            String text = value.toString();
            if (entryId == null && isLikelyEntryField(field, text)) {
                entryId = text;
            }
            if (isPromotedEntryId(text)) {
                promotedEntry = true;
                entryId = text;
                break;
            }
        }

        for (Field field : access.objectFields) {
            Object value = Reflect.read(field, model);
            if (isPromotedMetadata(value)) {
                promotedMetadata = true;
                break;
            }
        }

        if (promotedEntry || promotedMetadata) {
            LinkedHashSet<String> signals = new LinkedHashSet<>();
            if (promotedEntry) {
                signals.add("entryId:promoted-");
            }
            if (promotedMetadata) {
                signals.add("TimelinePromotedMetadata");
            }
            return new Classification(true, entryId, signals, false);
        }

        // Validated render models have definitive direct fields. Avoid a graph
        // walk for every normal timeline item on supported X versions.
        if (expectedRenderModel != null
                && expectedRenderModel.equals(model.getClass().getName())) {
            return new Classification(
                    false,
                    entryId,
                    Collections.<String>emptySet(),
                    false
            );
        }

        if (!allowActionFallback) {
            return new Classification(
                    false,
                    entryId,
                    Collections.<String>emptySet(),
                    false
            );
        }

        PromotedActionScanner.Match actionMatch = PromotedActionScanner.inspect(model);
        if (actionMatch.promoted) {
            return new Classification(
                    true,
                    entryId,
                    actionMatch.actions,
                    true
            );
        }

        return new Classification(
                false,
                entryId,
                Collections.<String>emptySet(),
                actionMatch.used
        );
    }

    /**
     * True when {@code value} carries the URT promoted token, either as the
     * whole entry ID or as one of its "-" separated segments.
     */
    private static boolean isPromotedEntryId(String value) {
        if (value == null) {
            return false;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.startsWith(PROMOTED_ENTRY_TOKEN)
                || lower.contains("-" + PROMOTED_ENTRY_TOKEN);
    }

    private static boolean isLikelyEntryField(Field field, String value) {
        if (value == null) {
            return false;
        }
        String lowerName = field.getName().toLowerCase(Locale.ROOT);
        String lowerValue = value.toLowerCase(Locale.ROOT);
        return "a".equals(field.getName())
                || lowerName.contains("entry")
                || lowerValue.startsWith("tweet-")
                || lowerValue.contains("-tweet-")
                || isPromotedEntryId(lowerValue);
    }

    /**
     * X obfuscates the promoted-metadata class away on recent releases, so the
     * validated name is only the fast path and any model class still named
     * after it counts as the same signal.
     */
    private static boolean isPromotedMetadata(Object value) {
        if (value == null) {
            return false;
        }
        String name = value.getClass().getName();
        return PROMOTED_METADATA_CLASS.equals(name)
                || name.endsWith(PROMOTED_METADATA_SUFFIX);
    }

    static final class Classification {
        static final Classification NORMAL = new Classification(
                false,
                null,
                Collections.<String>emptySet(),
                false
        );

        final boolean promoted;
        final String entryId;
        final Set<String> signals;
        final boolean actionFallbackUsed;

        Classification(
                boolean promoted,
                String entryId,
                Set<String> signals,
                boolean actionFallbackUsed
        ) {
            this.promoted = promoted;
            this.entryId = entryId;
            this.signals = Collections.unmodifiableSet(
                    new LinkedHashSet<>(signals)
            );
            this.actionFallbackUsed = actionFallbackUsed;
        }

        String stableLogKey(Object model) {
            if (entryId != null && !entryId.isEmpty()) {
                return entryId;
            }
            return model.getClass().getName()
                    + "@"
                    + Integer.toHexString(System.identityHashCode(model));
        }
    }

    private static final class DirectAccess {
        final List<Field> stringFields;
        final List<Field> objectFields;

        DirectAccess(
                List<Field> stringFields,
                List<Field> objectFields
        ) {
            this.stringFields = stringFields;
            this.objectFields = objectFields;
        }

        static DirectAccess resolve(Class<?> type) {
            ArrayList<Field> strings = new ArrayList<>();
            ArrayList<Field> objects = new ArrayList<>();

            for (Field field : Reflect.allFields(type)) {
                if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                } catch (Throwable ignored) {
                    // Field#get may still work inside the target process.
                }

                Class<?> declared = field.getType();
                if (CharSequence.class.isAssignableFrom(declared)
                        || declared == String.class) {
                    strings.add(field);
                }
                if (!declared.isPrimitive()) {
                    objects.add(field);
                }
            }

            strings.sort((left, right) -> fieldPriority(left) - fieldPriority(right));
            objects.sort((left, right) -> objectFieldPriority(left) - objectFieldPriority(right));
            return new DirectAccess(
                    Collections.unmodifiableList(strings),
                    Collections.unmodifiableList(objects)
            );
        }

        private static int fieldPriority(Field field) {
            if ("a".equals(field.getName())) {
                return 0;
            }
            if (field.getName().toLowerCase(Locale.ROOT).contains("entry")) {
                return 1;
            }
            return 2;
        }

        private static int objectFieldPriority(Field field) {
            String typeName = field.getType().getName();
            if (PROMOTED_METADATA_CLASS.equals(typeName)
                    || typeName.endsWith(PROMOTED_METADATA_SUFFIX)) {
                return 0;
            }
            if ("m".equals(field.getName())) {
                return 1;
            }
            return 2;
        }
    }
}
