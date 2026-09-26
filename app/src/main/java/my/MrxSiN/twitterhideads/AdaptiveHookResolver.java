package my.MrxSiN.twitterhideads;

import android.content.Context;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Resolves the renamed X post render boundaries by structure instead of by
 * class name.
 *
 * Structural recognition lives in {@link RenderBoundaryShape}, candidate
 * discovery in {@link BoundaryCandidateSource} and persistence in
 * {@link AdaptiveBoundaryCache}; this class owns ranking and reporting.
 *
 * Every boundary the policy accepts (discovery.bf, score threshold 320) is
 * returned, up to the policy's boundary limit, rather than
 * a single winner. X compiles one composable into both a defaulted and a
 * non-defaulted entry point, and renders posts through more than one boundary,
 * so demanding a unique winner made near-ties resolve to no hook at all.
 * Suppression is driven by the post model at each boundary, so hooking several
 * is redundant rather than harmful.
 */
final class AdaptiveHookResolver {
    private AdaptiveHookResolver() {
    }

    static Resolution resolve(
            Context context,
            ClassLoader classLoader,
            CompatibilityProfile.DetectedVersion version
    ) {
        if (context == null || classLoader == null) {
            return Resolution.failure("missing-context-or-classloader");
        }

        String apkPath = AdaptiveBoundaryCache.sourceDir(context);
        if (apkPath == null || apkPath.isEmpty()) {
            return Resolution.failure("missing-source-apk");
        }
        String apkFingerprint = AdaptiveBoundaryCache.fingerprint(apkPath, version);

        Resolution cached = AdaptiveBoundaryCache.load(
                context,
                classLoader,
                version,
                apkFingerprint
        );
        if (cached != null) {
            log("Adaptive cache hit: boundaries=" + cached.methods.size()
                    + ", inlinedCallers=" + cached.callers.size()
                    + ", score=" + cached.score
                    + ", primary=" + cached.method());
            return cached;
        }

        long started = System.currentTimeMillis();
        ArrayList<Candidate> candidates = new ArrayList<>();
        String dexKitStatus;
        try {
            System.loadLibrary("dexkit");
            candidates.addAll(BoundaryCandidateSource.findWithDexKit(apkPath, classLoader));
            dexKitStatus = "ok";
        } catch (Throwable throwable) {
            dexKitStatus = "failed:" + Reflect.describeThrowable(throwable);
            log("DexKit resolver unavailable; using bounded DexFile fallback: "
                    + Reflect.describeThrowable(throwable));
        }

        String fallbackStatus = "not-needed";
        if (candidates.isEmpty()) {
            try {
                candidates.addAll(BoundaryCandidateSource.findWithDexFile(apkPath, classLoader));
                fallbackStatus = "ok";
            } catch (Throwable throwable) {
                fallbackStatus = "failed:" + Reflect.describeThrowable(throwable);
            }
        }

        Resolution selected = select(candidates, dexKitStatus, fallbackStatus);
        if (selected.method() != null && "ok".equals(dexKitStatus)) {
            selected = selected.withCallers(
                    InlinedCallerSource.find(apkPath, classLoader, selected.methods)
            );
        }
        long elapsed = System.currentTimeMillis() - started;
        if (selected.method() != null) {
            AdaptiveBoundaryCache.save(context, version, apkFingerprint, selected);
            logCandidateSummary(selected.methods);
            log("Adaptive resolver selected: boundaries=" + selected.methods.size()
                    + ", topScore=" + selected.score
                    + ", candidates=" + selected.candidateCount
                    + ", inlinedCallers=" + selected.callers.size()
                    + ", source=" + selected.source
                    + ", elapsedMs=" + elapsed);
        } else {
            log("Adaptive resolver failed open: reason=" + selected.reason
                    + ", candidates=" + selected.candidateCount
                    + ", dexKit=" + dexKitStatus
                    + ", fallback=" + fallbackStatus
                    + ", elapsedMs=" + elapsed);
        }
        return selected;
    }

    static Resolution select(
            List<Candidate> rawCandidates,
            String dexKitStatus,
            String fallbackStatus
    ) {
        if (rawCandidates.isEmpty()) {
            return Resolution.failure(
                    "no-structural-candidate;dexkit=" + dexKitStatus
                            + ";fallback=" + fallbackStatus
            );
        }

        // Deduplicate candidates when DexKit and DexFile resolve the same method.
        Set<String> seen = new LinkedHashSet<>();
        ArrayList<Candidate> candidates = new ArrayList<>();
        for (Candidate candidate : rawCandidates) {
            if (seen.add(AdaptiveBoundaryCache.signature(candidate.method))) {
                candidates.add(candidate);
            }
        }
        candidates.sort(new Comparator<Candidate>() {
            @Override
            public int compare(Candidate left, Candidate right) {
                int score = Integer.compare(right.score, left.score);
                return score != 0
                        ? score
                        : AdaptiveBoundaryCache.signature(left.method)
                        .compareTo(AdaptiveBoundaryCache.signature(right.method));
            }
        });

        int limit = Math.min(PolicyLimits.maxBoundaries(), candidates.size());
        ArrayList<Method> methods = new ArrayList<>(limit);
        for (int index = 0; index < limit; index++) {
            methods.add(candidates.get(index).method);
        }
        return Resolution.success(
                methods,
                candidates.get(0).score,
                candidates.size(),
                candidates.get(0).source,
                false
        );
    }

    private static void logCandidateSummary(List<Method> methods) {
        for (int index = 0; index < methods.size(); index++) {
            Method method = methods.get(index);
            log("Adaptive boundary #" + (index + 1)
                    + ": score=" + RenderBoundaryShape.score(method)
                    + ", method=" + method);
        }
    }

    static void log(String message) {
        ModuleRuntime.log(message);
    }

    static final class Resolution {
        final List<Method> methods;
        /** Direct callers of {@link #methods}, deoptimized so AOT-inlined copies reach the hooks. */
        final List<Method> callers;
        final int score;
        final int candidateCount;
        final String source;
        final boolean cacheHit;
        final String reason;

        private Resolution(
                List<Method> methods,
                List<Method> callers,
                int score,
                int candidateCount,
                String source,
                boolean cacheHit,
                String reason
        ) {
            this.methods = methods;
            this.callers = callers;
            this.score = score;
            this.candidateCount = candidateCount;
            this.source = source;
            this.cacheHit = cacheHit;
            this.reason = reason;
        }

        /** The highest scoring boundary, or {@code null} when unresolved. */
        Method method() {
            return methods.isEmpty() ? null : methods.get(0);
        }

        Resolution withCallers(List<Method> resolvedCallers) {
            return new Resolution(
                    methods,
                    resolvedCallers,
                    score,
                    candidateCount,
                    source,
                    cacheHit,
                    reason
            );
        }

        static Resolution success(
                List<Method> methods,
                int score,
                int candidateCount,
                String source,
                boolean cacheHit
        ) {
            return new Resolution(
                    methods,
                    Collections.<Method>emptyList(),
                    score,
                    candidateCount,
                    source,
                    cacheHit,
                    null
            );
        }

        static Resolution failure(String reason) {
            return failure(reason, 0);
        }

        static Resolution failure(String reason, int candidateCount) {
            return new Resolution(
                    Collections.<Method>emptyList(),
                    Collections.<Method>emptyList(),
                    -1,
                    candidateCount,
                    "none",
                    false,
                    reason
            );
        }
    }

    static final class Candidate {
        final Method method;
        final int score;
        final String source;

        Candidate(Method method, int score, String source) {
            this.method = method;
            this.score = score;
            this.source = source;
        }
    }
}
