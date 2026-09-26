package my.MrxSiN.twitterhideads;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Counts and logs suppressed posts without unbounded growth.
 *
 * Unique entries are tracked in a fixed-size LRU set, so {@code unique} is
 * exact up to {@link #MAX_TRACKED_KEYS} distinct entries and approximate
 * afterwards. Entry identifiers pass through {@link LogPrivacy}.
 */
final class BlockRecorder {
    static final int MAX_TRACKED_KEYS = 512;

    /**
     * Per-render boundary observations are a debugging aid: debug builds only,
     * so release builds never log organic posts.
     */
    static final boolean OBSERVE = BuildConfig.DEBUG;

    private static final int DETAILED_BLOCK_LOG_LIMIT = 300;
    private static final int PERIODIC_LOG_INTERVAL = 50;
    private static final int BOUNDARY_OBSERVATION_LOG_LIMIT = 4000;

    private final AtomicInteger attempts = new AtomicInteger();
    private final AtomicInteger unique = new AtomicInteger();
    private final Map<String, Boolean> recentKeys = new LinkedHashMap<String, Boolean>(
            64, 0.75f, true
    ) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
            return size() > MAX_TRACKED_KEYS;
        }
    };
    private final ConcurrentHashMap<String, AtomicInteger> observations =
            new ConcurrentHashMap<>();

    void observe(
            String boundary,
            Object postModel,
            BundledAdPatterns.Classification classification
    ) {
        AtomicInteger seen = observations.computeIfAbsent(boundary, key -> new AtomicInteger());
        if (seen.incrementAndGet() > BOUNDARY_OBSERVATION_LOG_LIMIT) {
            return;
        }
        ModuleRuntime.log("Boundary observation: boundary=" + boundary
                + ", model=" + postModel.getClass().getName()
                + ", verdict=" + classification.verdict
                + ", entryId=" + LogPrivacy.entryId(classification.entryId)
                + ", signals=" + classification.signals
                + ", actionFallbackUsed=" + classification.actionFallbackUsed);
    }

    void record(
            String boundary,
            Object postModel,
            BundledAdPatterns.Classification classification
    ) {
        int attemptCount = attempts.incrementAndGet();
        boolean firstObservation = markSeen(classification.stableLogKey(postModel));
        int uniqueCount = firstObservation ? unique.incrementAndGet() : unique.get();

        if (firstObservation && uniqueCount <= DETAILED_BLOCK_LOG_LIMIT) {
            ModuleRuntime.log("Blocked promoted post before Compose: boundary=" + boundary
                    + ", attempts=" + attemptCount
                    + ", unique=" + uniqueCount
                    + ", entryId=" + LogPrivacy.entryId(classification.entryId)
                    + ", signals=" + classification.signals
                    + ", actionFallbackUsed=" + classification.actionFallbackUsed
                    + ", " + PolicyStats.summary());
            return;
        }
        if (attemptCount % PERIODIC_LOG_INTERVAL == 0) {
            ModuleRuntime.log("Block summary: attempts=" + attemptCount
                    + ", unique=" + uniqueCount
                    + ", activeBoundary=" + boundary
                    + ", " + PolicyStats.summary());
        }
    }

    /** Returns true when {@code key} was not among the recently tracked keys. */
    boolean markSeen(String key) {
        synchronized (recentKeys) {
            return recentKeys.put(key, Boolean.TRUE) == null;
        }
    }

    int trackedKeys() {
        synchronized (recentKeys) {
            return recentKeys.size();
        }
    }
}
