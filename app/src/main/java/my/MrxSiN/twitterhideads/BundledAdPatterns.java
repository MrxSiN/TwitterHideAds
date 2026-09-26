package my.MrxSiN.twitterhideads;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Host side of promoted-post classification.
 *
 * This class only gathers facts: the render model's text fields and whether
 * one of its object fields is a promoted-metadata model. The decision is made
 * by {@code brainfuck/src/post.bf} (OP_CLASSIFY_POST, specified in
 * docs/policy/post.md):
 *
 *   direct promoted entry ID       -> PROMOTED
 *   direct promoted metadata       -> PROMOTED
 *   confident organic entry ID     -> ORGANIC
 *   no reliable direct signal      -> action-graph fallback, else UNKNOWN
 *
 * The entry grammar itself (segment boundaries, nested module prefixes) is
 * Brainfuck too. The reflective {@link PromotedActionScanner} walk is reserved
 * for models the policy cannot decide directly, and its result is cached per
 * model instance because Compose recomposes the same model repeatedly. When a
 * policy request fails for any reason the model is kept.
 */
final class BundledAdPatterns {
    static final String SCHEMA_VERSION = "9-bf" + BfAbi.ABI_MAJOR;

    /** {@link #classifyPacked} result bits. */
    static final int PROMOTED = 1;
    static final int IDENTIFIES_POST = 2;

    private static final Set<String> NO_SIGNALS = Collections.emptySet();

    private static final ConcurrentHashMap<Class<?>, DirectAccess> DIRECT_ACCESS =
            new ConcurrentHashMap<>();
    private static final IdentityCache<Classification> FALLBACK_RESULTS =
            new IdentityCache<>(256);

    private BundledAdPatterns() {
    }

    /**
     * Allocation-free classification for the render hot path: {@link #PROMOTED}
     * and {@link #IDENTIFIES_POST} bits.
     */
    static int classifyPacked(Object model, String expectedRenderModel, boolean allowActionFallback) {
        if (model == null) {
            return 0;
        }
        DirectAccess access = DIRECT_ACCESS.computeIfAbsent(model.getClass(), DirectAccess::resolve);
        int verdict;
        String entryId = null;
        boolean entry;
        PolicyFrame frame = PolicyFrame.begin(BfAbi.OP_CLASSIFY_POST);
        try {
            if (!access.request(frame, model, expectedRenderModel, allowActionFallback)) {
                return 0;
            }
            verdict = frame.out(0);
            int index = frame.out(1);
            entry = index != BfAbi.NO_INDEX;
            if (verdict == BfAbi.V_NEED_FALLBACK && entry) {
                entryId = frame.refs[index].toString();
            }
        } finally {
            frame.release();
        }
        if (verdict == BfAbi.V_PROMOTED) {
            return PROMOTED | IDENTIFIES_POST;
        }
        if (verdict != BfAbi.V_NEED_FALLBACK) {
            return entry ? IDENTIFIES_POST : 0;
        }
        Classification fallback = fallback(model, entryId);
        return (fallback.promoted() ? PROMOTED : 0) | (fallback.identifiesPost() ? IDENTIFIES_POST : 0);
    }

    /** Full classification with entry ID and signals, for logging and tests. */
    static Classification classifyTimelinePost(
            Object model,
            String expectedRenderModel,
            boolean allowActionFallback
    ) {
        if (model == null) {
            return Classification.UNKNOWN;
        }
        DirectAccess access = DIRECT_ACCESS.computeIfAbsent(model.getClass(), DirectAccess::resolve);
        PolicyFrame frame = PolicyFrame.begin(BfAbi.OP_CLASSIFY_POST);
        int verdict;
        int signals;
        String entryId;
        try {
            if (!access.request(frame, model, expectedRenderModel, allowActionFallback)) {
                return Classification.UNKNOWN;
            }
            verdict = frame.out(0);
            int index = frame.out(1);
            signals = frame.out(2);
            entryId = index < frame.refCount ? frame.refs[index].toString() : null;
        } finally {
            frame.release();
        }

        switch (verdict) {
            case BfAbi.V_PROMOTED: {
                LinkedHashSet<String> names = new LinkedHashSet<>();
                if ((signals & BfAbi.SIG_ENTRY) != 0) {
                    names.add("entryId:promoted");
                }
                if ((signals & BfAbi.SIG_METADATA) != 0) {
                    names.add("PromotedMetadata");
                }
                return new Classification(Verdict.PROMOTED, entryId, names, false);
            }
            case BfAbi.V_ORGANIC:
                return new Classification(Verdict.ORGANIC, entryId, NO_SIGNALS, false);
            case BfAbi.V_NEED_FALLBACK:
                return fallback(model, entryId);
            default:
                return new Classification(Verdict.UNKNOWN, entryId, NO_SIGNALS, false);
        }
    }

    private static Classification fallback(Object model, String entryId) {
        Classification cached = FALLBACK_RESULTS.get(model);
        if (cached != null) {
            return cached;
        }
        PolicyStats.fallback();
        PromotedActionScanner.Match actionMatch = PromotedActionScanner.inspect(model);
        Classification result = new Classification(
                actionMatch.promoted ? Verdict.PROMOTED : Verdict.UNKNOWN,
                entryId,
                actionMatch.promoted ? actionMatch.actions : NO_SIGNALS,
                actionMatch.used
        );
        FALLBACK_RESULTS.put(model, result);
        return result;
    }

    enum Verdict {
        PROMOTED,
        ORGANIC,
        UNKNOWN
    }

    static final class Classification {
        static final Classification UNKNOWN = new Classification(
                Verdict.UNKNOWN,
                null,
                NO_SIGNALS,
                false
        );

        final Verdict verdict;
        final String entryId;
        final Set<String> signals;
        final boolean actionFallbackUsed;

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

        boolean promoted() {
            return verdict == Verdict.PROMOTED;
        }

        /** True when the model looked like a timeline post at all. */
        boolean identifiesPost() {
            return entryId != null || verdict == Verdict.PROMOTED;
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

    /** Cached per render-model class: which fields carry the facts. */
    private static final class DirectAccess {
        final Field[] stringFields;
        final Field[] objectFields;

        DirectAccess(List<Field> stringFields, List<Field> objectFields) {
            this.stringFields = stringFields.toArray(new Field[0]);
            this.objectFields = objectFields.toArray(new Field[0]);
        }

        /** Encodes OP_CLASSIFY_POST for {@code model} and runs it. */
        boolean request(PolicyFrame frame, Object model, String expectedRenderModel, boolean allowFallback) {
            boolean metadata = false;
            for (Field field : objectFields) {
                if (PromotedMetadata.isInstance(Reflect.read(field, model))) {
                    metadata = true;
                    break;
                }
            }
            frame.bool(expectedRenderModel != null && expectedRenderModel.equals(model.getClass().getName()));
            frame.bool(allowFallback);
            frame.bool(metadata);
            int countAt = frame.position();
            frame.u8(0);
            int count = 0;
            for (Field field : stringFields) {
                Object value = Reflect.read(field, model);
                if (!(value instanceof CharSequence)) {
                    continue;
                }
                if (count == BfAbi.MAX_FIELDS || !frame.ref(value)) {
                    return false;
                }
                frame.entryText((CharSequence) value);
                count++;
            }
            frame.patch(countAt, count);
            return frame.send(BfAbi.PROG_POST, 3) && frame.out(0) <= BfAbi.V_NEED_FALLBACK
                    && (frame.out(1) < count || frame.out(1) == BfAbi.NO_INDEX);
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
                if (CharSequence.class.isAssignableFrom(declared)) {
                    strings.add(field);
                } else if (!declared.isPrimitive()) {
                    objects.add(field);
                }
            }

            objects.sort((left, right) -> objectFieldPriority(left) - objectFieldPriority(right));
            return new DirectAccess(strings, objects);
        }

        private static int objectFieldPriority(Field field) {
            return PromotedMetadata.isType(field.getType().getName()) ? 0 : 1;
        }
    }
}
