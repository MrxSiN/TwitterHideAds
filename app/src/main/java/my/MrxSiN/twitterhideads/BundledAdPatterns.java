package my.MrxSiN.twitterhideads;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Advertisement-classification rules shipped inside the APK.
 *
 * X 12.7.1 exposes two ad-specific properties on the post render model:
 *  - an entry ID beginning with "promoted-";
 *  - a direct com.x.models.TimelinePromotedMetadata field.
 *
 * The promoted PostActionType values discovered by the menu probes are retained
 * as a compatibility fallback for an unfamiliar model shape. No user-specific
 * advertiser or post IDs are required.
 */
final class BundledAdPatterns {
    static final String SCHEMA_VERSION = "3";
    static final String TESTED_X_VERSION = CompatibilityProfile.TESTED_VERSION_PREFIX;
    static final String EXPECTED_RENDER_MODEL = "com.x.urt.items.post.a6$a";
    static final String PROMOTED_METADATA_CLASS =
            "com.x.models.TimelinePromotedMetadata";
    static final String ACTION_ENUM_CLASS = "com.x.models.PostActionType";

    static final Set<String> PROMOTED_ACTIONS;

    private static final ConcurrentHashMap<Class<?>, DirectAccess> DIRECT_ACCESS =
            new ConcurrentHashMap<>();

    private static final int ACTION_MAX_DEPTH = 5;
    private static final int ACTION_MAX_OBJECTS = 160;
    private static final int ACTION_MAX_COLLECTION_ITEMS = 24;

    static {
        LinkedHashSet<String> actions = new LinkedHashSet<>();
        actions.add("PromotedDismissAd");
        actions.add("PromotedAdsInfo");
        actions.add("PromotedReportAd");
        PROMOTED_ACTIONS = Collections.unmodifiableSet(actions);
    }

    private BundledAdPatterns() {
    }

    static Classification classifyTimelinePost(Object model) {
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
            Object value = read(field, model);
            if (!(value instanceof CharSequence)) {
                continue;
            }
            String text = value.toString();
            if (entryId == null && isLikelyEntryField(field, text)) {
                entryId = text;
            }
            if (startsWithPromoted(text)) {
                promotedEntry = true;
                entryId = text;
                break;
            }
        }

        for (Field field : access.metadataFields) {
            Object value = read(field, model);
            if (isPromotedMetadata(value)) {
                promotedMetadata = true;
                break;
            }
        }

        // Obfuscated builds may declare the metadata field as Object. The exact
        // X 12.7.1 field is named "m", so inspect that cached field as well.
        if (!promotedMetadata && access.obfuscatedMetadataField != null) {
            promotedMetadata = isPromotedMetadata(
                    read(access.obfuscatedMetadataField, model)
            );
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

        // The known a6$a render model has definitive direct fields. Avoid a
        // graph walk for every normal timeline item on the tested X version.
        if (EXPECTED_RENDER_MODEL.equals(model.getClass().getName())) {
            return new Classification(
                    false,
                    entryId,
                    Collections.<String>emptySet(),
                    false
            );
        }

        ActionMatch actionMatch = inspectPromotedActions(model);
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

    private static ActionMatch inspectPromotedActions(Object root) {
        if (root == null) {
            return ActionMatch.NOT_USED;
        }

        ArrayDeque<Node> queue = new ArrayDeque<>();
        IdentityHashMap<Object, Boolean> visited = new IdentityHashMap<>();
        LinkedHashSet<String> matched = new LinkedHashSet<>();
        queue.add(new Node(root, 0));

        int examined = 0;
        while (!queue.isEmpty() && examined < ACTION_MAX_OBJECTS) {
            Node node = queue.removeFirst();
            Object value = node.value;
            if (value == null || node.depth > ACTION_MAX_DEPTH) {
                continue;
            }

            Class<?> type = value.getClass();
            if (isScalar(type)) {
                String action = promotedActionName(value);
                if (action != null) {
                    matched.add(action);
                }
                continue;
            }
            if (visited.put(value, Boolean.TRUE) != null) {
                continue;
            }
            examined++;

            if (type.isArray()) {
                int length = Math.min(
                        Array.getLength(value),
                        ACTION_MAX_COLLECTION_ITEMS
                );
                for (int index = 0; index < length; index++) {
                    queue.addLast(new Node(
                            Array.get(value, index),
                            node.depth + 1
                    ));
                }
                continue;
            }
            if (value instanceof Collection<?>) {
                int count = 0;
                for (Object item : (Collection<?>) value) {
                    if (count++ >= ACTION_MAX_COLLECTION_ITEMS) {
                        break;
                    }
                    queue.addLast(new Node(item, node.depth + 1));
                }
                continue;
            }
            if (value instanceof Map<?, ?>) {
                int count = 0;
                for (Object item : ((Map<?, ?>) value).values()) {
                    if (count++ >= ACTION_MAX_COLLECTION_ITEMS) {
                        break;
                    }
                    queue.addLast(new Node(item, node.depth + 1));
                }
                continue;
            }
            if (!shouldTraverse(type)) {
                continue;
            }

            for (Field field : allFields(type)) {
                if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                    continue;
                }
                Object child = read(field, value);
                if (child != null) {
                    queue.addLast(new Node(child, node.depth + 1));
                }
            }
        }

        return new ActionMatch(true, !matched.isEmpty(), matched);
    }

    private static String promotedActionName(Object value) {
        Class<?> type = value.getClass();
        String raw;
        if (value instanceof Enum<?>) {
            raw = ((Enum<?>) value).name();
        } else {
            raw = String.valueOf(value);
            int hash = raw.lastIndexOf('#');
            if (hash >= 0 && hash + 1 < raw.length()) {
                raw = raw.substring(hash + 1);
            }
        }

        if (PROMOTED_ACTIONS.contains(raw)) {
            return raw;
        }
        if (ACTION_ENUM_CLASS.equals(type.getName())) {
            for (String action : PROMOTED_ACTIONS) {
                if (raw.endsWith(action)) {
                    return action;
                }
            }
        }
        return null;
    }

    private static boolean startsWithPromoted(String value) {
        return value != null
                && value.toLowerCase(Locale.ROOT).startsWith("promoted-");
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
                || lowerValue.startsWith("promoted-");
    }

    private static boolean isPromotedMetadata(Object value) {
        return value != null
                && PROMOTED_METADATA_CLASS.equals(value.getClass().getName());
    }

    private static Object read(Field field, Object owner) {
        if (field == null || owner == null) {
            return null;
        }
        try {
            field.setAccessible(true);
            return field.get(owner);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean shouldTraverse(Class<?> type) {
        String name = type.getName();
        return name.startsWith("com.x.urt.items.post.")
                || name.startsWith("com.x.models.")
                || name.startsWith("kotlin.collections.")
                || name.startsWith("java.util.");
    }

    private static boolean isScalar(Class<?> type) {
        return type.isPrimitive()
                || type.isEnum()
                || Number.class.isAssignableFrom(type)
                || CharSequence.class.isAssignableFrom(type)
                || Boolean.class == type
                || Character.class == type
                || Class.class == type;
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
        final List<Field> metadataFields;
        final Field obfuscatedMetadataField;

        DirectAccess(
                List<Field> stringFields,
                List<Field> metadataFields,
                Field obfuscatedMetadataField
        ) {
            this.stringFields = stringFields;
            this.metadataFields = metadataFields;
            this.obfuscatedMetadataField = obfuscatedMetadataField;
        }

        static DirectAccess resolve(Class<?> type) {
            ArrayList<Field> strings = new ArrayList<>();
            ArrayList<Field> metadata = new ArrayList<>();
            Field obfuscatedMetadata = null;

            for (Field field : allFields(type)) {
                if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                } catch (Throwable ignored) {
                    // Field#get may still work under LSPosed in the target process.
                }

                Class<?> declared = field.getType();
                if (CharSequence.class.isAssignableFrom(declared)
                        || declared == String.class) {
                    strings.add(field);
                }
                if (PROMOTED_METADATA_CLASS.equals(declared.getName())
                        || declared.getName().contains("PromotedMetadata")) {
                    metadata.add(field);
                }
                if ("m".equals(field.getName())) {
                    obfuscatedMetadata = field;
                }
            }

            // Prefer the known obfuscated entry field first, then semantic fields.
            strings.sort((left, right) -> fieldPriority(left) - fieldPriority(right));
            return new DirectAccess(
                    Collections.unmodifiableList(strings),
                    Collections.unmodifiableList(metadata),
                    obfuscatedMetadata
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
    }

    private static final class ActionMatch {
        static final ActionMatch NOT_USED = new ActionMatch(
                false,
                false,
                Collections.<String>emptySet()
        );

        final boolean used;
        final boolean promoted;
        final Set<String> actions;

        ActionMatch(boolean used, boolean promoted, Set<String> actions) {
            this.used = used;
            this.promoted = promoted;
            this.actions = Collections.unmodifiableSet(
                    new LinkedHashSet<>(actions)
            );
        }
    }

    private static final class Node {
        final Object value;
        final int depth;

        Node(Object value, int depth) {
            this.value = value;
            this.depth = depth;
        }
    }
}
