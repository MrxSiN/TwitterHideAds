package my.MrxSiN.twitterhideads;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Host side of the Video Tab paging-batch policy.
 *
 * The host enumerates the batch and sends each item's facts (whether its
 * class can hold a post, whether it carries a promoted-metadata model, its
 * map key and text fields) in one OP_VIDEO_BATCH request. {@code post.bf}
 * returns every item's class and whether the batch is a safe mixed batch
 * (docs/policy/post.md). Rebuilding the collection stays here and in
 * {@link PersistentListCopier}.
 */
final class VideoDatasetClassifier {
    private static final int MAX_LOGGED_IDS = 12;
    private static final int TALLY_WINDOW = 128;
    private static final ConcurrentHashMap<Class<?>, Access> ACCESS_CACHE =
            new ConcurrentHashMap<>();

    private VideoDatasetClassifier() {
    }

    /**
     * Classifies every item of {@code container}. A container that is not a
     * collection, map or array, holds more than {@link BfAbi#MAX_ITEMS}
     * items, or whose request fails yields {@link Batch#NONE}.
     */
    static Batch inspect(Object container) {
        int size = sizeOf(container);
        if (size < 0) {
            return Batch.NONE;
        }
        if (size > BfAbi.MAX_ITEMS) {
            return Batch.NONE;
        }
        Object[] items = new Object[size];
        Object[] keys = container instanceof Map<?, ?> ? new Object[size] : null;
        int count = 0;
        if (container instanceof Map<?, ?>) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) container).entrySet()) {
                if (count == size) {
                    return Batch.NONE;
                }
                keys[count] = entry.getKey();
                items[count++] = entry.getValue();
            }
        } else if (container instanceof Iterable<?>) {
            Iterator<?> iterator = ((Iterable<?>) container).iterator();
            while (iterator.hasNext()) {
                if (count == size) {
                    return Batch.NONE;
                }
                items[count++] = iterator.next();
            }
        } else {
            for (; count < size; count++) {
                items[count] = Array.get(container, count);
            }
        }
        return request(size, items, keys, count);
    }

    private static Batch request(int size, Object[] items, Object[] keys, int count) {
        int[] refStart = new int[count];
        PolicyFrame frame = PolicyFrame.begin(BfAbi.OP_VIDEO_BATCH);
        try {
            frame.u16(size);
            frame.u8(count);
            for (int index = 0; index < count; index++) {
                refStart[index] = frame.refCount;
                if (!encode(frame, items[index], keys == null ? null : keys[index])) {
                    return Batch.NONE;
                }
            }
            if (!frame.send(BfAbi.PROG_POST, 2 * count + 4)) {
                return Batch.NONE;
            }
            int[] classes = new int[count];
            String[] ids = new String[count];
            for (int index = 0; index < count; index++) {
                int cls = frame.out(2 * index);
                int entry = frame.out(2 * index + 1);
                if (cls != 0 && cls != (BfAbi.VI_POST | BfAbi.VI_PROMOTED)
                        && cls != (BfAbi.VI_POST | BfAbi.VI_NORMAL)
                        && cls != BfAbi.VI_POST) {
                    return Batch.NONE;
                }
                classes[index] = cls;
                int ref = entry == BfAbi.MAP_KEY_INDEX ? refStart[index]
                        : entry == BfAbi.NO_INDEX ? -1
                        : refStart[index] + entry + (hasTextKey(keys, index) ? 1 : 0);
                ids[index] = ref >= 0 && ref < frame.refCount ? frame.refs[ref].toString() : null;
            }
            int base = 2 * count;
            return new Batch(size, frame.out(base), frame.out(base + 1), frame.out(base + 2),
                    frame.out(base + 3) == 1, classes, ids, items);
        } finally {
            frame.release();
        }
    }

    private static boolean hasTextKey(Object[] keys, int index) {
        return keys != null && keys[index] instanceof CharSequence;
    }

    /** One item: POSS, then metadata, key flag, field count, key, fields. */
    private static boolean encode(PolicyFrame frame, Object item, Object key) {
        if (item == null) {
            frame.u8(0);
            return true;
        }
        Access access = ACCESS_CACHE.computeIfAbsent(item.getClass(), Access::resolve);
        if (!access.possiblePostItem) {
            frame.u8(0);
            return true;
        }
        boolean metadata = false;
        for (Field field : access.objectFields) {
            if (PromotedMetadata.isInstance(Reflect.read(field, item))) {
                metadata = true;
                break;
            }
        }
        boolean textKey = key instanceof CharSequence;
        frame.u8(1);
        frame.bool(metadata);
        frame.bool(textKey);
        int countAt = frame.position();
        frame.u8(0);
        if (textKey) {
            if (!frame.ref(key)) {
                return false;
            }
            frame.entryText((CharSequence) key);
        }
        int fields = 0;
        for (Field field : access.stringFields) {
            Object value = Reflect.read(field, item);
            if (!(value instanceof CharSequence)) {
                continue;
            }
            if (fields + (textKey ? 1 : 0) >= BfAbi.MAX_FIELDS || !frame.ref(value)) {
                return false;
            }
            frame.entryText((CharSequence) value);
            fields++;
        }
        frame.patch(countAt, fields);
        return true;
    }

    /** Single-item classification (tests and diagnostics). */
    static Classification classify(Object item, CharSequence mapKey) {
        Batch batch = request(1, new Object[]{item}, mapKey == null ? null : new Object[]{mapKey}, 1);
        if (batch == Batch.NONE) {
            return Classification.NONE;
        }
        int cls = batch.classes[0];
        if ((cls & BfAbi.VI_POST) == 0) {
            return Classification.NONE;
        }
        return new Classification(true, (cls & BfAbi.VI_PROMOTED) != 0, (cls & BfAbi.VI_NORMAL) != 0,
                batch.ids[0], item.getClass().getName());
    }

    /**
     * The items of {@code container} the policy keeps, in order. Map
     * containers are never rebuilt (empty result, so the caller's size check
     * fails open); a batch that could not be classified keeps everything.
     */
    static List<Object> filteredElements(Object container, Batch batch) {
        ArrayList<Object> filtered = new ArrayList<>();
        if (container instanceof Map<?, ?> || (!(container instanceof Iterable<?>)
                && (container == null || !container.getClass().isArray()))) {
            return filtered;
        }
        if (batch.items == null) {
            if (container instanceof Iterable<?>) {
                for (Object item : (Iterable<?>) container) {
                    filtered.add(item);
                }
            } else {
                for (int index = 0; index < Array.getLength(container); index++) {
                    filtered.add(Array.get(container, index));
                }
            }
            return filtered;
        }
        for (int index = 0; index < batch.items.length; index++) {
            if ((batch.classes[index] & BfAbi.VI_PROMOTED) == 0) {
                filtered.add(batch.items[index]);
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

    private static final class Access {
        final boolean possiblePostItem;
        final Field[] stringFields;
        final Field[] objectFields;

        Access(boolean possiblePostItem, List<Field> stringFields, List<Field> objectFields) {
            this.possiblePostItem = possiblePostItem;
            this.stringFields = stringFields.toArray(new Field[0]);
            this.objectFields = objectFields.toArray(new Field[0]);
        }

        static Access resolve(Class<?> type) {
            ArrayList<Field> strings = new ArrayList<>();
            ArrayList<Field> objects = new ArrayList<>();
            boolean metadataField = false;
            for (Field field : Reflect.allFields(type)) {
                if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                    continue;
                }
                if (CharSequence.class.isAssignableFrom(field.getType())) {
                    strings.add(field);
                } else {
                    objects.add(field);
                    if (PromotedMetadata.isType(field.getType().getName())) {
                        metadataField = true;
                    }
                }
            }
            String name = type.getName();
            boolean postClass = name.startsWith("com.x.models.timelines.items.")
                    || name.startsWith("com.x.urt.items.post.");
            return new Access(postClass || metadataField, strings, objects);
        }
    }

    static final class Batch {
        static final Batch NONE = new Batch(-1, 0, 0, 0, false, null, null, null);

        final int containerSize;
        final int itemCount;
        final int promotedCount;
        final int normalCount;
        final Set<String> promotedIds;
        final Set<String> normalIds;
        private final boolean safe;
        private final int[] classes;
        private final String[] ids;
        private final Object[] items;

        Batch(int containerSize, int itemCount, int promotedCount, int normalCount, boolean safe,
              int[] classes, String[] ids, Object[] items) {
            this.containerSize = containerSize;
            this.itemCount = itemCount;
            this.promotedCount = promotedCount;
            this.normalCount = normalCount;
            this.safe = safe;
            this.classes = classes;
            this.ids = ids;
            this.items = items;
            LinkedHashSet<String> promoted = new LinkedHashSet<>();
            LinkedHashSet<String> normal = new LinkedHashSet<>();
            if (classes != null) {
                // Log sets: the tallied window only, as before.
                for (int index = 0; index < Math.min(classes.length, TALLY_WINDOW); index++) {
                    if ((classes[index] & BfAbi.VI_PROMOTED) != 0) {
                        addLimited(promoted, display(ids[index], items[index]));
                    } else if ((classes[index] & BfAbi.VI_NORMAL) != 0) {
                        addLimited(normal, display(ids[index], items[index]));
                    }
                }
            }
            this.promotedIds = Collections.unmodifiableSet(promoted);
            this.normalIds = Collections.unmodifiableSet(normal);
        }

        private static String display(String id, Object item) {
            return id != null ? id : "metadata-only:" + item.getClass().getName();
        }

        private static void addLimited(Set<String> target, String value) {
            if (target.size() < MAX_LOGGED_IDS) {
                target.add(value);
            }
        }

        /** The policy's safe-mixed-batch decision. */
        boolean safeMixedVideoBatch() {
            return safe;
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
