package my.MrxSiN.twitterhideads.legacy;

// Frozen copy of the v2.1.0 Java policy: the behavioural oracle for the
// Brainfuck parity tests. Never edit it to match new behaviour.

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Small direct-mapped cache keyed by object identity.
 *
 * Compose recomposes the same immutable post model many times; this lets an
 * expensive classification run once per model. Keys are weak, so the cache
 * never keeps a model alive, and a slot collision simply evicts the older
 * entry.
 */
public final class LegacyIdentityCache<V> {
    private final AtomicReferenceArray<Slot<V>> slots;
    private final int mask;

    /** @param capacityPowerOfTwo number of slots; must be a power of two. */
    LegacyIdentityCache(int capacityPowerOfTwo) {
        if (Integer.bitCount(capacityPowerOfTwo) != 1) {
            throw new IllegalArgumentException("capacity must be a power of two");
        }
        slots = new AtomicReferenceArray<>(capacityPowerOfTwo);
        mask = capacityPowerOfTwo - 1;
    }

    V get(Object key) {
        Slot<V> slot = slots.get(index(key));
        return slot != null && slot.key.get() == key ? slot.value : null;
    }

    void put(Object key, V value) {
        slots.set(index(key), new Slot<>(key, value));
    }

    private int index(Object key) {
        int hash = System.identityHashCode(key);
        return (hash ^ (hash >>> 16)) & mask;
    }

    private static final class Slot<V> {
        public final WeakReference<Object> key;
        public final V value;

        Slot(Object key, V value) {
            this.key = new WeakReference<>(key);
            this.value = value;
        }
    }
}
