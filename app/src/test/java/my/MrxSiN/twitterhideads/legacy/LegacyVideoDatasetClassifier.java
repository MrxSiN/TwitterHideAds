package my.MrxSiN.twitterhideads.legacy;

// Frozen copy of the v2.1.0 Java policy: the behavioural oracle for the
// Brainfuck parity tests. Never edit it to match new behaviour.

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Direct, bounded classifier for items in the Video Tab paging batch. Entry
 * grammar and the metadata rule are shared with the timeline classifier
 * through {@link LegacyEntryIds} and {@link LegacyPromotedMetadata}.
 */
public final class LegacyVideoDatasetClassifier {
    private static final int MAX_ITEMS = 128;
    private static final int MAX_LOGGED_IDS = 12;
    private static final ConcurrentHashMap<Class<?>, Access> ACCESS_CACHE =
            new ConcurrentHashMap<>();

    private LegacyVideoDatasetClassifier() {
    }

    public static Batch inspect(Object container) {
        int size = sizeOf(container);
        if (size < 0) {
            return Batch.NONE;
        }

        Tally tally = new Tally();
        if (container instanceof Map<?, ?>) {
            int examined = 0;
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) container).entrySet()) {
                if (examined++ >= MAX_ITEMS) {
                    break;
                }
                tally.add(classify(entry.getValue(), identifier(entry.getKey())));
            }
        } else if (container instanceof Iterable<?>) {
            int examined = 0;
            for (Object item : (Iterable<?>) container) {
                if (examined++ >= MAX_ITEMS) {
                    break;
                }
                tally.add(classify(item, null));
            }
        } else if (container.getClass().isArray()) {
            int length = Math.min(Array.getLength(container), MAX_ITEMS);
            for (int index = 0; index < length; index++) {
                tally.add(classify(Array.get(container, index), null));
            }
        }
        return tally.toBatch(size);
    }

    public static boolean isPromoted(Object item) {
        return classify(item, null).promoted;
    }

    public static Classification classify(Object item, String mapKeyId) {
        if (item == null) {
            return Classification.NONE;
        }
        Access access = ACCESS_CACHE.computeIfAbsent(item.getClass(), Access::resolve);
        if (!access.possiblePostItem) {
            return Classification.NONE;
        }

        String entryId = mapKeyId;
        for (Field field : access.stringFields) {
            String candidate = identifier(LegacyReflect.read(field, item));
            if (candidate == null) {
                continue;
            }
            if (entryId == null || LegacyEntryIds.kind(candidate) == LegacyEntryIds.Kind.PROMOTED) {
                entryId = candidate;
            }
        }
        boolean metadata = false;
        for (Field field : access.objectFields) {
            if (LegacyPromotedMetadata.isInstance(LegacyReflect.read(field, item))) {
                metadata = true;
                break;
            }
        }

        LegacyEntryIds.Kind kind = LegacyEntryIds.kind(entryId);
        boolean promotedId = kind == LegacyEntryIds.Kind.PROMOTED;
        boolean normalId = kind == LegacyEntryIds.Kind.ORGANIC;
        if (!promotedId && !normalId && !metadata) {
            return Classification.NONE;
        }
        return new Classification(
                true,
                promotedId || metadata,
                normalId && !metadata,
                entryId,
                item.getClass().getName()
        );
    }

    public static List<Object> filteredElements(Object container) {
        ArrayList<Object> filtered = new ArrayList<>();
        if (container instanceof Iterable<?>) {
            for (Object item : (Iterable<?>) container) {
                if (!isPromoted(item)) {
                    filtered.add(item);
                }
            }
        } else if (container != null && container.getClass().isArray()) {
            int length = Array.getLength(container);
            for (int index = 0; index < length; index++) {
                Object item = Array.get(container, index);
                if (!isPromoted(item)) {
                    filtered.add(item);
                }
            }
        }
        return filtered;
    }

    private static int sizeOf(Object container) {
        if (container instanceof Collection<?>) {
            return ((Collection<?>) container).size();
        }
        if (container instanceof Map<?, ?>) {
            return ((Map<?, ?>) container).size();
        }
        if (container != null && container.getClass().isArray()) {
            return Array.getLength(container);
        }
        return -1;
    }

    /** The value itself when it is a recognised entry identifier. */
    private static String identifier(Object value) {
        if (!(value instanceof CharSequence)) {
            return null;
        }
        return LegacyEntryIds.kind((CharSequence) value) == LegacyEntryIds.Kind.NONE
                ? null
                : value.toString();
    }

    private static final class Tally {
        int items;
        int promoted;
        int normal;
        public final LinkedHashSet<String> promotedIds = new LinkedHashSet<>();
        public final LinkedHashSet<String> normalIds = new LinkedHashSet<>();

        void add(Classification classification) {
            if (!classification.postItem) {
                return;
            }
            items++;
            if (classification.promoted) {
                promoted++;
                addLimited(promotedIds, classification.displayId());
            } else if (classification.normal) {
                normal++;
                addLimited(normalIds, classification.displayId());
            }
        }

        Batch toBatch(int size) {
            return new Batch(size, items, promoted, normal, promotedIds, normalIds);
        }

        private static void addLimited(Set<String> target, String value) {
            if (value != null && target.size() < MAX_LOGGED_IDS) {
                target.add(value);
            }
        }
    }

    private static final class Access {
        public final boolean possiblePostItem;
        public final List<Field> stringFields;
        public final List<Field> objectFields;

        Access(boolean possiblePostItem, List<Field> stringFields, List<Field> objectFields) {
            this.possiblePostItem = possiblePostItem;
            this.stringFields = stringFields;
            this.objectFields = objectFields;
        }

        static Access resolve(Class<?> type) {
            ArrayList<Field> strings = new ArrayList<>();
            ArrayList<Field> objects = new ArrayList<>();
            boolean metadataField = false;
            for (Field field : LegacyReflect.allFields(type)) {
                if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                    continue;
                }
                if (CharSequence.class.isAssignableFrom(field.getType())) {
                    strings.add(field);
                } else {
                    objects.add(field);
                    if (LegacyPromotedMetadata.isType(field.getType().getName())) {
                        metadataField = true;
                    }
                }
            }
            String name = type.getName();
            boolean postClass = name.startsWith("com.x.models.timelines.items.")
                    || name.startsWith("com.x.urt.items.post.");
            return new Access(
                    postClass || metadataField,
                    Collections.unmodifiableList(strings),
                    Collections.unmodifiableList(objects)
            );
        }
    }

    public static final class Batch {
        static final Batch NONE = new Batch(
                -1, 0, 0, 0,
                Collections.<String>emptySet(),
                Collections.<String>emptySet()
        );

        public final int containerSize;
        public final int itemCount;
        public final int promotedCount;
        public final int normalCount;
        public final Set<String> promotedIds;
        public final Set<String> normalIds;

        Batch(
                int containerSize,
                int itemCount,
                int promotedCount,
                int normalCount,
                Set<String> promotedIds,
                Set<String> normalIds
        ) {
            this.containerSize = containerSize;
            this.itemCount = itemCount;
            this.promotedCount = promotedCount;
            this.normalCount = normalCount;
            this.promotedIds = Collections.unmodifiableSet(new LinkedHashSet<>(promotedIds));
            this.normalIds = Collections.unmodifiableSet(new LinkedHashSet<>(normalIds));
        }

        public boolean safeMixedVideoBatch() {
            return containerSize >= 3
                    && itemCount >= 2
                    && promotedCount > 0
                    && normalCount > 0;
        }
    }

    public static final class Classification {
        static final Classification NONE = new Classification(false, false, false, null, null);

        public final boolean postItem;
        public final boolean promoted;
        public final boolean normal;
        public final String entryId;
        public final String className;

        Classification(
                boolean postItem,
                boolean promoted,
                boolean normal,
                String entryId,
                String className
        ) {
            this.postItem = postItem;
            this.promoted = promoted;
            this.normal = normal;
            this.entryId = entryId;
            this.className = className;
        }

        public String displayId() {
            return entryId == null ? "metadata-only:" + className : entryId;
        }
    }
}
