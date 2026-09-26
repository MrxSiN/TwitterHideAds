package my.MrxSiN.twitterhideads.legacy;

// Frozen copy of the v2.1.0 Java policy: the behavioural oracle for the
// Brainfuck parity tests. Never edit it to match new behaviour.

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Advertisement-classification rules shipped inside the APK.
 *
 * A render model is classified from its direct fields first, in this order:
 *
 *   direct promoted entry ID       -> PROMOTED
 *   direct promoted metadata       -> PROMOTED
 *   confident organic entry ID     -> ORGANIC
 *   no reliable direct signal      -> action-graph fallback, else UNKNOWN
 *
 * The entry grammar lives in {@link LegacyEntryIds} and the metadata rule in
 * {@link LegacyPromotedMetadata}. The reflective {@link LegacyPromotedActionScanner} walk
 * is reserved for models that carry no recognisable entry ID, and its result
 * is cached per model instance because Compose recomposes the same model
 * repeatedly. No user-specific advertiser or post IDs are required.
 */
public final class LegacyBundledAdPatterns {
    public static final String SCHEMA_VERSION = "8";

    private static final Set<String> NO_SIGNALS = Collections.emptySet();

    private static final ConcurrentHashMap<Class<?>, DirectAccess> DIRECT_ACCESS =
            new ConcurrentHashMap<>();
    private static final LegacyIdentityCache<Classification> FALLBACK_RESULTS =
            new LegacyIdentityCache<>(256);

    private LegacyBundledAdPatterns() {
    }

    public static Classification classifyTimelinePost(
            Object model,
            String expectedRenderModel,
            boolean allowActionFallback
    ) {
        if (model == null) {
            return Classification.UNKNOWN;
        }

        DirectAccess access = DIRECT_ACCESS.computeIfAbsent(
                model.getClass(),
                DirectAccess::resolve
        );
        DirectSignals direct = access.read(model);

        if (direct.entryKind == LegacyEntryIds.Kind.PROMOTED || direct.promotedMetadata) {
            LinkedHashSet<String> signals = new LinkedHashSet<>();
            if (direct.entryKind == LegacyEntryIds.Kind.PROMOTED) {
                signals.add("entryId:promoted");
            }
            if (direct.promotedMetadata) {
                signals.add("PromotedMetadata");
            }
            return new Classification(Verdict.PROMOTED, direct.entryId, signals, false);
        }

        boolean validatedModel = expectedRenderModel != null
                && expectedRenderModel.equals(model.getClass().getName());
        if ((direct.entryKind == LegacyEntryIds.Kind.ORGANIC && !direct.ambiguousPromotedText)
                || validatedModel) {
            return new Classification(Verdict.ORGANIC, direct.entryId, NO_SIGNALS, false);
        }

        if (!allowActionFallback) {
            return new Classification(Verdict.UNKNOWN, direct.entryId, NO_SIGNALS, false);
        }

        Classification cached = FALLBACK_RESULTS.get(model);
        if (cached != null) {
            return cached;
        }
        LegacyPromotedActionScanner.Match actionMatch = LegacyPromotedActionScanner.inspect(model);
        Classification result = new Classification(
                actionMatch.promoted ? Verdict.PROMOTED : Verdict.UNKNOWN,
                direct.entryId,
                actionMatch.promoted ? actionMatch.actions : NO_SIGNALS,
                actionMatch.used
        );
        FALLBACK_RESULTS.put(model, result);
        return result;
    }

    public enum Verdict {
        PROMOTED,
        ORGANIC,
        UNKNOWN
    }

    public static final class Classification {
        static final Classification UNKNOWN = new Classification(
                Verdict.UNKNOWN,
                null,
                NO_SIGNALS,
                false
        );

        public final Verdict verdict;
        public final String entryId;
        public final Set<String> signals;
        public final boolean actionFallbackUsed;

        Classification(
                Verdict verdict,
                String entryId,
                Set<String> signals,
                boolean actionFallbackUsed
        ) {
            this.verdict = verdict;
            this.entryId = entryId;
            this.signals = signals.isEmpty()
                    ? NO_SIGNALS
                    : Collections.unmodifiableSet(new LinkedHashSet<>(signals));
            this.actionFallbackUsed = actionFallbackUsed;
        }

        public boolean promoted() {
            return verdict == Verdict.PROMOTED;
        }

        /** True when the model looked like a timeline post at all. */
        public boolean identifiesPost() {
            return entryId != null || verdict == Verdict.PROMOTED;
        }

        public String stableLogKey(Object model) {
            if (entryId != null && !entryId.isEmpty()) {
                return entryId;
            }
            return model.getClass().getName()
                    + "@"
                    + Integer.toHexString(System.identityHashCode(model));
        }
    }

    private static final class DirectSignals {
        public final String entryId;
        public final LegacyEntryIds.Kind entryKind;
        public final boolean promotedMetadata;
        public final boolean ambiguousPromotedText;

        DirectSignals(
                String entryId,
                LegacyEntryIds.Kind entryKind,
                boolean promotedMetadata,
                boolean ambiguousPromotedText
        ) {
            this.entryId = entryId;
            this.entryKind = entryKind;
            this.promotedMetadata = promotedMetadata;
            this.ambiguousPromotedText = ambiguousPromotedText;
        }
    }

    private static final class DirectAccess {
        public final List<Field> stringFields;
        public final List<Field> objectFields;

        DirectAccess(List<Field> stringFields, List<Field> objectFields) {
            this.stringFields = stringFields;
            this.objectFields = objectFields;
        }

        DirectSignals read(Object model) {
            String entryId = null;
            LegacyEntryIds.Kind entryKind = LegacyEntryIds.Kind.NONE;
            boolean ambiguous = false;
            for (Field field : stringFields) {
                Object value = LegacyReflect.read(field, model);
                if (!(value instanceof CharSequence)) {
                    continue;
                }
                LegacyEntryIds.Kind kind = LegacyEntryIds.kind((CharSequence) value);
                if (kind == LegacyEntryIds.Kind.PROMOTED) {
                    entryId = value.toString();
                    entryKind = kind;
                    break;
                }
                if (kind == LegacyEntryIds.Kind.ORGANIC) {
                    if (entryId == null) {
                        entryId = value.toString();
                        entryKind = kind;
                    }
                } else if (LegacyEntryIds.mentionsPromoted((CharSequence) value)) {
                    ambiguous = true;
                }
            }

            boolean metadata = false;
            if (entryKind != LegacyEntryIds.Kind.PROMOTED) {
                for (Field field : objectFields) {
                    if (LegacyPromotedMetadata.isInstance(LegacyReflect.read(field, model))) {
                        metadata = true;
                        break;
                    }
                }
            }
            return new DirectSignals(entryId, entryKind, metadata, ambiguous);
        }

        static DirectAccess resolve(Class<?> type) {
            ArrayList<Field> strings = new ArrayList<>();
            ArrayList<Field> objects = new ArrayList<>();

            for (Field field : LegacyReflect.allFields(type)) {
                if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                } catch (Throwable ignored) {
                    // Field#get may still work inside the target process.
                }

                Class<?> declared = field.getType();
                if (CharSequence.class.isAssignableFrom(declared)) {
                    strings.add(field);
                } else if (!declared.isPrimitive()) {
                    objects.add(field);
                }
            }

            objects.sort((left, right) -> objectFieldPriority(left) - objectFieldPriority(right));
            return new DirectAccess(
                    Collections.unmodifiableList(strings),
                    Collections.unmodifiableList(objects)
            );
        }

        private static int objectFieldPriority(Field field) {
            return LegacyPromotedMetadata.isType(field.getType().getName()) ? 0 : 1;
        }
    }
}
