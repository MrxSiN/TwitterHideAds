package my.MrxSiN.twitterhideads;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Bounded action-graph fallback used when a render model does not expose the
 * direct promoted signals {@link BundledAdPatterns} looks for, which happens
 * when the promoted-metadata class itself has been obfuscated away.
 */
final class PromotedActionScanner {
    static final String ACTION_ENUM_CLASS = "com.x.models.PostActionType";

    static final Set<String> PROMOTED_ACTIONS;

    private static final int MAX_DEPTH = 5;
    private static final int MAX_OBJECTS = 160;
    private static final int MAX_COLLECTION_ITEMS = 24;

    static {
        LinkedHashSet<String> actions = new LinkedHashSet<>();
        actions.add("PromotedDismissAd");
        actions.add("PromotedAdsInfo");
        actions.add("PromotedReportAd");
        PROMOTED_ACTIONS = Collections.unmodifiableSet(actions);
    }

    private PromotedActionScanner() {
    }

    static Match inspect(Object root) {
        if (root == null) {
            return Match.NOT_USED;
        }

        ArrayDeque<Node> queue = new ArrayDeque<>();
        IdentityHashMap<Object, Boolean> visited = new IdentityHashMap<>();
        LinkedHashSet<String> matched = new LinkedHashSet<>();
        queue.add(new Node(root, 0));

        int examined = 0;
        while (!queue.isEmpty() && examined < MAX_OBJECTS) {
            Node node = queue.removeFirst();
            Object value = node.value;
            if (value == null || node.depth > MAX_DEPTH) {
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
                        MAX_COLLECTION_ITEMS
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
                    if (count++ >= MAX_COLLECTION_ITEMS) {
                        break;
                    }
                    queue.addLast(new Node(item, node.depth + 1));
                }
                continue;
            }
            if (value instanceof Map<?, ?>) {
                int count = 0;
                for (Object item : ((Map<?, ?>) value).values()) {
                    if (count++ >= MAX_COLLECTION_ITEMS) {
                        break;
                    }
                    queue.addLast(new Node(item, node.depth + 1));
                }
                continue;
            }
            if (!shouldTraverse(type)) {
                continue;
            }

            for (Field field : Reflect.allFields(type)) {
                if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                    continue;
                }
                Object child = Reflect.read(field, value);
                if (child != null) {
                    queue.addLast(new Node(child, node.depth + 1));
                }
            }
        }

        return new Match(true, !matched.isEmpty(), matched);
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

    static final class Match {
        static final Match NOT_USED = new Match(
                false,
                false,
                Collections.<String>emptySet()
        );

        final boolean used;
        final boolean promoted;
        final Set<String> actions;

        Match(boolean used, boolean promoted, Set<String> actions) {
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
