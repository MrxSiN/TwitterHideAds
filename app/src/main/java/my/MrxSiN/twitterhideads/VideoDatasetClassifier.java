package my.MrxSiN.twitterhideads;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Direct, bounded classifier for items in the Video Tab paging batch. */
final class VideoDatasetClassifier {
    private static final int MAX_ITEMS = 128;
    private static final ConcurrentHashMap<Class<?>, Access> ACCESS_CACHE =
            new ConcurrentHashMap<>();

    private VideoDatasetClassifier() {
    }

    static Batch inspect(Object container) {
        int size = sizeOf(container);
        if (size < 0) {
            return Batch.NONE;
        }

        int items = 0;
        int promoted = 0;
        int normal = 0;
        LinkedHashSet<String> promotedIds = new LinkedHashSet<>();
        LinkedHashSet<String> normalIds = new LinkedHashSet<>();

        if (container instanceof Map<?, ?>) {
            int examined = 0;
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) container).entrySet()) {
                if (examined++ >= MAX_ITEMS) {
                    break;
                }
                Classification classification = classify(entry.getValue(), identifier(entry.getKey()));
                if (!classification.postItem) {
                    continue;
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
        } else if (container instanceof Iterable<?>) {
            int examined = 0;
            for (Object item : (Iterable<?>) container) {
                if (examined++ >= MAX_ITEMS) {
                    break;
                }
                Classification classification = classify(item, null);
                if (!classification.postItem) {
                    continue;
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
        } else if (container != null && container.getClass().isArray()) {
            int length = Math.min(Array.getLength(container), MAX_ITEMS);
            for (int index = 0; index < length; index++) {
                Classification classification = classify(Array.get(container, index), null);
                if (!classification.postItem) {
                    continue;
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
        }

        return new Batch(
                size,
                items,
                promoted,
                normal,
                promotedIds,
                normalIds
        );
    }

    static boolean isPromoted(Object item) {
        return classify(item, null).promoted;
    }

    static Classification classify(Object item, String mapKeyId) {
        if (item == null) {
            return Classification.NONE;
        }
        Access access = ACCESS_CACHE.computeIfAbsent(item.getClass(), Access::resolve);
        if (!access.possiblePostItem) {
            return Classification.NONE;
        }

        String entryId = mapKeyId;
        boolean metadata = false;
        for (Field field : access.stringFields) {
            Object value = read(field, item);
            String candidate = identifier(value);
            if (candidate == null) {
                continue;
            }
            if (entryId == null || startsPromoted(candidate)) {
                entryId = candidate;
            }
        }
        for (Field field : access.objectFields) {
            Object value = read(field, item);
            if (value != null && isPromotedMetadataType(value.getClass().getName())) {
                metadata = true;
                break;
            }
        }

        boolean promotedId = entryId != null && startsPromoted(entryId);
        boolean normalId = entryId != null
                && entryId.toLowerCase(Locale.ROOT).startsWith("tweet-");
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

    static List<Object> filteredElements(Object container) {
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

    private static boolean startsPromoted(String value) {
        return value.toLowerCase(Locale.ROOT).startsWith("promoted-");
    }

    private static String identifier(Object value) {
        if (!(value instanceof CharSequence)) {
            return null;
        }
        String text = value.toString();
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.startsWith("tweet-") || lower.startsWith("promoted-")
                ? text
                : null;
    }

    private static boolean isPromotedMetadataType(String typeName) {
        return BundledAdPatterns.PROMOTED_METADATA_CLASS.equals(typeName)
                || typeName.endsWith(".TimelinePromotedMetadata")
                || typeName.contains("PromotedMetadata");
    }

    private static Object read(Field field, Object owner) {
        try {
            field.setAccessible(true);
            return field.get(owner);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Field[] allFields(Class<?> type) {
        ArrayList<Field> fields = new ArrayList<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            Collections.addAll(fields, current.getDeclaredFields());
            current = current.getSuperclass();
        }
        return fields.toArray(new Field[0]);
    }

    private static void addLimited(Set<String> target, String value) {
        if (value != null && target.size() < 12) {
            target.add(value);
        }
    }

    private static final class Access {
        final boolean possiblePostItem;
        final List<Field> stringFields;
        final List<Field> objectFields;

        Access(boolean possiblePostItem, List<Field> stringFields, List<Field> objectFields) {
            this.possiblePostItem = possiblePostItem;
            this.stringFields = stringFields;
            this.objectFields = objectFields;
        }

        static Access resolve(Class<?> type) {
            ArrayList<Field> strings = new ArrayList<>();
            ArrayList<Field> objects = new ArrayList<>();
            boolean metadataField = false;
            for (Field field : allFields(type)) {
                if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                    continue;
                }
                if (CharSequence.class.isAssignableFrom(field.getType())) {
                    strings.add(field);
                } else {
                    objects.add(field);
                    if (isPromotedMetadataType(field.getType().getName())) {
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

    static final class Batch {
        static final Batch NONE = new Batch(
                -1, 0, 0, 0,
                Collections.<String>emptySet(),
                Collections.<String>emptySet()
        );

        final int containerSize;
        final int itemCount;
        final int promotedCount;
        final int normalCount;
        final Set<String> promotedIds;
        final Set<String> normalIds;

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

        boolean safeMixedVideoBatch() {
            return containerSize >= 3
                    && itemCount >= 2
                    && promotedCount > 0
                    && normalCount > 0;
        }
    }

    static final class Classification {
        static final Classification NONE = new Classification(false, false, false, null, null);

        final boolean postItem;
        final boolean promoted;
        final boolean normal;
        final String entryId;
        final String className;

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

        String displayId() {
            return entryId == null ? "metadata-only:" + className : entryId;
        }
    }
}
