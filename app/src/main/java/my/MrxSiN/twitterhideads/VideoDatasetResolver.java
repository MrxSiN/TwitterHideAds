package my.MrxSiN.twitterhideads;

import android.content.Context;
import android.content.SharedPreferences;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;

import dalvik.system.DexFile;

/** Resolves the single pre-pager Video Tab batch-copy method by structure. */
final class VideoDatasetResolver {
    private static final String PREFS = "twitterhideads_video_dataset_profile";
    private static final String CACHE_FORMAT = "1";
    private static final String URT_PREFIX = "com.x.urt.";

    private VideoDatasetResolver() {
    }

    static Resolution resolve(
            Context context,
            ClassLoader classLoader,
            CompatibilityProfile.DetectedVersion version
    ) {
        if (context == null || classLoader == null) {
            return Resolution.failure("missing-context-or-classloader", 0);
        }
        String apkPath = AdaptiveBoundaryCache.sourceDir(context);
        if (apkPath == null) {
            return Resolution.failure("missing-source-apk", 0);
        }
        String fingerprint = AdaptiveBoundaryCache.fingerprint(apkPath, version);
        Resolution cached = loadCached(context, classLoader, version, fingerprint);
        if (cached != null) {
            log("Video dataset cache hit: method=" + cached.method + ", score=" + cached.score);
            return cached;
        }

        long started = System.currentTimeMillis();
        ArrayList<Candidate> candidates = new ArrayList<>();
        int scanned = 0;
        DexFile dexFile = null;
        try {
            dexFile = new DexFile(apkPath);
            Enumeration<String> entries = dexFile.entries();
            int maxClasses = PolicyLimits.videoClasses();
            while (entries.hasMoreElements() && scanned < maxClasses) {
                String className = entries.nextElement();
                if (!isDirectUrtClass(className)) {
                    continue;
                }
                scanned++;
                Class<?> type;
                try {
                    type = Class.forName(className, false, classLoader);
                } catch (Throwable ignored) {
                    continue;
                }
                for (Method method : type.getDeclaredMethods()) {
                    Candidate candidate = evaluate(method, "dexfile");
                    if (candidate != null) {
                        candidates.add(candidate);
                    }
                }
            }
        } catch (Throwable throwable) {
            return Resolution.failure("scan-failed:" + Reflect.describeThrowable(throwable), candidates.size());
        } finally {
            if (dexFile != null) {
                try {
                    dexFile.close();
                } catch (Throwable ignored) {
                }
            }
        }

        Resolution selected = select(candidates);
        long elapsed = System.currentTimeMillis() - started;
        if (selected.method != null) {
            saveCached(context, version, fingerprint, selected);
            log("Video dataset resolver selected: method=" + selected.method
                    + ", score=" + selected.score
                    + ", candidates=" + selected.candidateCount
                    + ", scannedClasses=" + scanned
                    + ", elapsedMs=" + elapsed);
        } else {
            log("Video dataset resolver failed open: reason=" + selected.reason
                    + ", candidates=" + selected.candidateCount
                    + ", scannedClasses=" + scanned
                    + ", elapsedMs=" + elapsed);
        }
        return selected;
    }

    /**
     * Facts about {@code method} for OP_VIDEO_BOUNDARY; eligibility, score and
     * threshold are decided by discovery.bf (docs/policy/discovery.md).
     * Returns {@code null} for a non-candidate or a failed request.
     */
    static Candidate evaluate(Method method, String source) {
        Class<?>[] parameters = method.getParameterTypes();
        if (parameters.length > 255) {
            return null;
        }
        int modifiers = method.getModifiers();
        Class<?> returnType = method.getReturnType();
        PolicyFrame frame = PolicyFrame.begin(BfAbi.OP_VIDEO_BOUNDARY);
        try {
            frame.bool(Modifier.isStatic(modifiers));
            frame.bool(Modifier.isAbstract(modifiers));
            frame.bool(Modifier.isNative(modifiers));
            frame.bool(method.isSynthetic());
            frame.bool(method.isBridge());
            frame.bool(returnType == Void.TYPE);
            frame.bool(returnType.isPrimitive());
            frame.bool(parameters.length > 0 && parameters[0] == returnType);
            frame.bool(parameters.length > 1 && isImmutableListLike(parameters[1]));
            frame.bool(parameters.length > 1
                    && parameters[1].getName().startsWith("kotlinx.collections.immutable."));
            frame.bool(isDirectUrtClass(method.getDeclaringClass().getName()));
            frame.bool(returnType.getName().startsWith(URT_PREFIX));
            frame.bool(method.getName().length() <= 2);
            frame.bool(Modifier.isPublic(modifiers));
            frame.u8(parameters.length);
            for (Class<?> parameter : parameters) {
                frame.u8(parameter == Boolean.TYPE ? BfAbi.VR_BOOLEAN
                        : parameter == Integer.TYPE ? BfAbi.VR_INT : BfAbi.VR_OTHER);
            }
            if (!frame.send(BfAbi.PROG_DISCOVERY, 4) || frame.out(0) != 1) {
                return null;
            }
            return new Candidate(method, frame.out16(1), source, frame.out(3) == 1);
        } finally {
            frame.release();
        }
    }

    /** OP_VIDEO_SELECT over the two best scores (-1 when absent). */
    static int selectDecision(int count, int best, int second) {
        PolicyFrame frame = PolicyFrame.begin(BfAbi.OP_VIDEO_SELECT);
        try {
            frame.bool(count > 0);
            frame.u16(Math.max(0, Math.min(best, 0xFFFF)));
            frame.bool(second >= 0);
            frame.u16(Math.max(0, Math.min(second, 0xFFFF)));
            return frame.send(BfAbi.PROG_DISCOVERY, 1) ? frame.out(0) : BfAbi.VS_NONE;
        } finally {
            frame.release();
        }
    }

    private static Resolution select(List<Candidate> candidates) {
        if (candidates.isEmpty()) {
            return Resolution.failure("no-structural-candidate", 0);
        }
        candidates.sort(new Comparator<Candidate>() {
            @Override
            public int compare(Candidate left, Candidate right) {
                int score = Integer.compare(right.score, left.score);
                return score != 0 ? score : AdaptiveBoundaryCache.signature(left.method)
                        .compareTo(AdaptiveBoundaryCache.signature(right.method));
            }
        });
        int limit = Math.min(PolicyLimits.candidatesLogged(), candidates.size());
        for (int index = 0; index < limit; index++) {
            Candidate candidate = candidates.get(index);
            log("Video dataset candidate #" + (index + 1)
                    + ": score=" + candidate.score + ", method=" + candidate.method);
        }

        Candidate best = candidates.get(0);
        int second = candidates.size() > 1 ? candidates.get(1).score : -1;
        switch (selectDecision(candidates.size(), best.score, second)) {
            case BfAbi.VS_ACCEPT:
                return Resolution.success(best.method, best.score, candidates.size(), best.source, false);
            case BfAbi.VS_BELOW:
                return Resolution.failure("score-below-threshold:" + best.score, candidates.size());
            case BfAbi.VS_AMBIGUOUS:
                return Resolution.failure("ambiguous:" + best.score + "/" + second, candidates.size());
            default:
                return Resolution.failure("policy-rejected", candidates.size());
        }
    }

    private static boolean isImmutableListLike(Class<?> type) {
        return Iterable.class.isAssignableFrom(type)
                || type.getName().startsWith("kotlinx.collections.immutable.");
    }

    private static boolean isDirectUrtClass(String className) {
        if (!className.startsWith(URT_PREFIX)) {
            return false;
        }
        String remainder = className.substring(URT_PREFIX.length());
        int dollar = remainder.indexOf('$');
        if (dollar >= 0) {
            remainder = remainder.substring(0, dollar);
        }
        return remainder.indexOf('.') < 0;
    }

    private static Resolution loadCached(
            Context context,
            ClassLoader classLoader,
            CompatibilityProfile.DetectedVersion version,
            String fingerprint
    ) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String prefix = AdaptiveBoundaryCache.cachePrefix(version);
            if (!CACHE_FORMAT.equals(prefs.getString(prefix + "format", null))
                    || !fingerprint.equals(prefs.getString(prefix + "apk", null))) {
                return null;
            }
            String className = prefs.getString(prefix + "class", null);
            String methodName = prefs.getString(prefix + "method", null);
            String params = prefs.getString(prefix + "params", null);
            int score = prefs.getInt(prefix + "score", -1);
            if (className == null || methodName == null || params == null) {
                return null;
            }
            Class<?> type = Class.forName(className, false, classLoader);
            for (Method method : type.getDeclaredMethods()) {
                if (!methodName.equals(method.getName()) || !params.equals(AdaptiveBoundaryCache.parameterKey(method))) {
                    continue;
                }
                Candidate verified = evaluate(method, "cache");
                if (verified == null || !verified.accepted) {
                    return null;
                }
                return Resolution.success(method, Math.max(score, verified.score), 1, "cache", true);
            }
        } catch (Throwable throwable) {
            log("Video dataset cache ignored: " + Reflect.describeThrowable(throwable));
        }
        return null;
    }

    private static void saveCached(
            Context context,
            CompatibilityProfile.DetectedVersion version,
            String fingerprint,
            Resolution resolution
    ) {
        try {
            String prefix = AdaptiveBoundaryCache.cachePrefix(version);
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putString(prefix + "format", CACHE_FORMAT)
                    .putString(prefix + "apk", fingerprint)
                    .putString(prefix + "class", resolution.method.getDeclaringClass().getName())
                    .putString(prefix + "method", resolution.method.getName())
                    .putString(prefix + "params", AdaptiveBoundaryCache.parameterKey(resolution.method))
                    .putInt(prefix + "score", resolution.score)
                    .apply();
        } catch (Throwable throwable) {
            log("Video dataset cache save failed: " + Reflect.describeThrowable(throwable));
        }
    }

    private static void log(String message) {
        ModuleRuntime.log(message);
    }

    static final class Resolution {
        final Method method;
        final int score;
        final int candidateCount;
        final String source;
        final boolean cacheHit;
        final String reason;

        private Resolution(Method method, int score, int candidateCount, String source,
                           boolean cacheHit, String reason) {
            this.method = method;
            this.score = score;
            this.candidateCount = candidateCount;
            this.source = source;
            this.cacheHit = cacheHit;
            this.reason = reason;
        }

        static Resolution success(Method method, int score, int candidateCount,
                                  String source, boolean cacheHit) {
            return new Resolution(method, score, candidateCount, source, cacheHit, null);
        }

        static Resolution failure(String reason, int candidateCount) {
            return new Resolution(null, -1, candidateCount, "none", false, reason);
        }
    }

    static final class Candidate {
        final Method method;
        final int score;
        final String source;
        /** The policy's per-method threshold verdict. */
        final boolean accepted;

        Candidate(Method method, int score, String source, boolean accepted) {
            this.method = method;
            this.score = score;
            this.source = source;
            this.accepted = accepted;
        }
    }
}
