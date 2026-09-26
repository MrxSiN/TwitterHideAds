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
 *
 * The walk stays in Java (it is reflection). Every scalar it meets is handed,
 * in batches, to {@code brainfuck/src/action.bf} (OP_ACTION_NAMES, specified
 * in docs/policy/action.md), which decides whether the value names one of the
 * promoted post actions.
 */
final class PromotedActionScanner {
    static final String ACTION_ENUM_CLASS = "com.x.models.PostActionType";

    /** Names of the OP_ACTION_NAMES result codes, indexed by code. */
    private static final String[] ACTION_NAMES = {
            null, "PromotedDismissAd", "PromotedAdsInfo", "PromotedReportAd"
    };

    private PromotedActionScanner() {
    }

    static Match inspect(Object root) {
        if (root == null) {
            return Match.NOT_USED;
        }

        int maxDepth = PolicyLimits.scanDepth();
        int maxObjects = PolicyLimits.scanObjects();
        int maxCollectionItems = PolicyLimits.scanItems();
        ArrayDeque<Node> queue = new ArrayDeque<>();
        IdentityHashMap<Object, Boolean> visited = new IdentityHashMap<>();
        LinkedHashSet<String> matched = new LinkedHashSet<>();
        queue.add(new Node(root, 0));

        Batch batch = new Batch(matched);
        try {
            walk(queue, visited, batch, maxDepth, maxObjects, maxCollectionItems);
            batch.flush();
        } finally {
            batch.frame.release();
        }
        return new Match(true, !matched.isEmpty(), matched);
    }

    private static void walk(
            ArrayDeque<Node> queue,
            IdentityHashMap<Object, Boolean> visited,
            Batch batch,
            int maxDepth,
            int maxObjects,
            int maxCollectionItems
    ) {
        int examined = 0;
        while (!queue.isEmpty() && examined < maxObjects) {
            Node node = queue.removeFirst();
            Object value = node.value;
            if (value == null || node.depth > maxDepth) {
                continue;
            }

            Class<?> type = value.getClass();
            if (isScalar(type)) {
                batch.add(value);
                continue;
            }
            if (visited.put(value, Boolean.TRUE) != null) {
                continue;
            }
            examined++;

            if (type.isArray()) {
                int length = Math.min(
                        Array.getLength(value),
                        maxCollectionItems
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
                    if (count++ >= maxCollectionItems) {
                        break;
                    }
                    queue.addLast(new Node(item, node.depth + 1));
                }
                continue;
            }
            if (value instanceof Map<?, ?>) {
                int count = 0;
                for (Object item : ((Map<?, ?>) value).values()) {
                    if (count++ >= maxCollectionItems) {
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
    }

    /** Scalars awaiting OP_ACTION_NAMES, flushed every {@link BfAbi#MAX_ITEMS} values. */
    private static final class Batch {
        final PolicyFrame frame = PolicyFrame.begin(BfAbi.OP_ACTION_NAMES);
        final Set<String> matched;
        int count;

        Batch(Set<String> matched) {
            this.matched = matched;
            frame.u8(0);
        }

        void add(Object value) {
            String raw = value instanceof Enum<?> ? ((Enum<?>) value).name() : String.valueOf(value);
            frame.bool(value instanceof Enum<?>);
            frame.bool(ACTION_ENUM_CLASS.equals(value.getClass().getName()));
            frame.actionText(raw);
            if (++count == BfAbi.MAX_ITEMS) {
                flush();
            }
        }

        void flush() {
            if (count == 0) {
                return;
            }
            frame.patch(BfAbi.HEADER_SIZE, count);
            if (frame.send(BfAbi.PROG_ACTION, count + 1)) {
                for (int index = 0; index < count; index++) {
                    int code = frame.out(index);
                    if (code > BfAbi.ACT_NONE && code < ACTION_NAMES.length) {
                        matched.add(ACTION_NAMES[code]);
                    }
                }
            }
            frame.restart(BfAbi.OP_ACTION_NAMES);
            frame.u8(0);
            count = 0;
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
