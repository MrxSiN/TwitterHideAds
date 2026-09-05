package my.MrxSiN.twitterhideads;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.MethodDataList;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import dalvik.system.DexFile;
import de.robv.android.xposed.XposedBridge;

/**
 * Resolves the renamed X post render boundaries by structure instead of by
 * class name.
 *
 * Structural recognition lives in {@link RenderBoundaryShape}; this class owns
 * discovery (DexKit, with a bounded reflection fallback), ranking and caching.
 *
 * Every boundary at or above {@link #ACTIVE_THRESHOLD} is returned rather than
 * a single winner. X compiles one composable into both a defaulted and a
 * non-defaulted entry point, and renders posts through more than one boundary,
 * so demanding a unique winner made near-ties resolve to no hook at all.
 * Suppression is driven by the post model at each boundary, so hooking several
 * is redundant rather than harmful.
 */
final class AdaptiveHookResolver {
    private static final String TAG = "TwitterHideAds";

    private static final String PREFS = "twitterhideads_adaptive_profile";
    private static final String CACHE_FORMAT = "4";
    private static final int ACTIVE_THRESHOLD = 320;
    private static final int MAX_REFLECTION_CLASSES = 1500;
    private static final int MAX_BOUNDARIES = 16;

    private static final String POST_MODEL_DESCRIPTOR =
            "L" + RenderBoundaryShape.POST_PACKAGE_ROOT.replace('.', '/') + "/";

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

        String apkPath = sourceDir(context);
        if (apkPath == null || apkPath.isEmpty()) {
            return Resolution.failure("missing-source-apk");
        }
        String apkFingerprint = fingerprint(apkPath, version);

        Resolution cached = loadCached(
                context,
                classLoader,
                version,
                apkFingerprint
        );
        if (cached != null) {
            log("Adaptive cache hit: boundaries=" + cached.methods.size()
                    + ", score=" + cached.score
                    + ", primary=" + cached.method());
            return cached;
        }

        long started = System.currentTimeMillis();
        ArrayList<Candidate> candidates = new ArrayList<>();
        String dexKitStatus;
        try {
            System.loadLibrary("dexkit");
            candidates.addAll(findWithDexKit(apkPath, classLoader));
            dexKitStatus = "ok";
        } catch (Throwable throwable) {
            dexKitStatus = "failed:" + describeThrowable(throwable);
            log("DexKit resolver unavailable; using bounded DexFile fallback: "
                    + describeThrowable(throwable));
        }

        String fallbackStatus = "not-needed";
        if (candidates.isEmpty()) {
            try {
                candidates.addAll(findWithDexFile(apkPath, classLoader));
                fallbackStatus = "ok";
            } catch (Throwable throwable) {
                fallbackStatus = "failed:" + describeThrowable(throwable);
            }
        }

        Resolution selected = select(candidates, dexKitStatus, fallbackStatus);
        long elapsed = System.currentTimeMillis() - started;
        if (selected.method() != null) {
            saveCached(context, version, apkFingerprint, selected);
            logCandidateSummary(selected.methods);
            log("Adaptive resolver selected: boundaries=" + selected.methods.size()
                    + ", topScore=" + selected.score
                    + ", candidates=" + selected.candidateCount
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

    /**
     * Asks DexKit for every static void method in the application and lets
     * {@link RenderBoundaryShape} decide which ones are boundaries. Encoding
     * the parameter layout in the query itself would re-break on the next
     * release that adds or drops a parameter, and restricting the search to the
     * post package hides boundaries declared elsewhere.
     *
     * Results are pre-filtered on the raw descriptor so that only methods
     * taking a post model first are loaded through the host class loader, which
     * is the expensive step.
     */
    private static List<Candidate> findWithDexKit(
            String apkPath,
            ClassLoader classLoader
    ) throws Exception {
        ArrayList<Candidate> candidates = new ArrayList<>();
        try (DexKitBridge bridge = DexKitBridge.create(apkPath)) {
            MethodMatcher matcher = MethodMatcher.create()
                    .modifiers(Modifier.STATIC)
                    .returnType("void");
            FindMethod query = FindMethod.create().matcher(matcher);
            MethodDataList result = bridge.findMethod(query);
            int inspected = 0;
            for (MethodData data : result) {
                if (!takesPostModelFirst(data)) {
                    continue;
                }
                inspected++;
                try {
                    Method method = data.getMethodInstance(classLoader);
                    Candidate candidate = evaluate(method, "dexkit");
                    if (candidate != null) {
                        candidates.add(candidate);
                    }
                } catch (Throwable ignored) {
                    // A metadata result that cannot resolve in the host loader is unusable.
                }
            }
            log("DexKit structural query: dexFiles=" + bridge.getDexNum()
                    + ", rawMatches=" + result.size()
                    + ", postModelFirst=" + inspected
                    + ", eligible=" + candidates.size());
        }
        return candidates;
    }

    /** Cheap descriptor test that avoids loading every static void method. */
    private static boolean takesPostModelFirst(MethodData data) {
        try {
            String descriptor = data.getDescriptor();
            int open = descriptor.indexOf('(');
            return open >= 0
                    && descriptor.startsWith(POST_MODEL_DESCRIPTOR, open + 1);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static List<Candidate> findWithDexFile(
            String apkPath,
            ClassLoader classLoader
    ) throws Exception {
        ArrayList<Candidate> candidates = new ArrayList<>();
        int scanned = 0;
        DexFile dexFile = new DexFile(apkPath);
        try {
            Enumeration<String> entries = dexFile.entries();
            while (entries.hasMoreElements() && scanned < MAX_REFLECTION_CLASSES) {
                String className = entries.nextElement();
                if (!className.startsWith(RenderBoundaryShape.POST_PACKAGE)) {
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
        } finally {
            dexFile.close();
        }
        log("DexFile structural fallback: scannedPostClasses=" + scanned
                + ", eligible=" + candidates.size());
        return candidates;
    }

    private static Candidate evaluate(Method method, String source) {
        int score = RenderBoundaryShape.score(method);
        if (score == RenderBoundaryShape.NOT_A_BOUNDARY || score < ACTIVE_THRESHOLD) {
            return null;
        }
        return new Candidate(method, score, source);
    }

    private static Resolution select(
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
            if (seen.add(signature(candidate.method))) {
                candidates.add(candidate);
            }
        }
        candidates.sort(new Comparator<Candidate>() {
            @Override
            public int compare(Candidate left, Candidate right) {
                int score = Integer.compare(right.score, left.score);
                return score != 0
                        ? score
                        : signature(left.method).compareTo(signature(right.method));
            }
        });

        int limit = Math.min(MAX_BOUNDARIES, candidates.size());
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

    private static Resolution loadCached(
            Context context,
            ClassLoader classLoader,
            CompatibilityProfile.DetectedVersion version,
            String apkFingerprint
    ) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String prefix = cachePrefix(version);
            if (!CACHE_FORMAT.equals(prefs.getString(prefix + "format", null))
                    || !apkFingerprint.equals(prefs.getString(prefix + "apk", null))) {
                return null;
            }
            Set<String> signatures = prefs.getStringSet(prefix + "boundaries", null);
            if (signatures == null || signatures.isEmpty()) {
                return null;
            }

            ArrayList<Method> methods = new ArrayList<>(signatures.size());
            int topScore = 0;
            for (String cached : sorted(signatures)) {
                Method method = resolveSignature(cached, classLoader);
                if (method == null) {
                    return null;
                }
                int score = RenderBoundaryShape.score(method);
                if (score == RenderBoundaryShape.NOT_A_BOUNDARY || score < ACTIVE_THRESHOLD) {
                    return null;
                }
                topScore = Math.max(topScore, score);
                methods.add(method);
            }
            return Resolution.success(
                    methods,
                    topScore,
                    methods.size(),
                    "cache",
                    true
            );
        } catch (Throwable throwable) {
            log("Adaptive cache ignored: " + describeThrowable(throwable));
        }
        return null;
    }

    private static Method resolveSignature(String cached, ClassLoader classLoader)
            throws ClassNotFoundException {
        int hash = cached.indexOf('#');
        int open = cached.indexOf('(', hash + 1);
        if (hash < 0 || open < 0 || !cached.endsWith(")")) {
            return null;
        }
        String className = cached.substring(0, hash);
        String methodName = cached.substring(hash + 1, open);
        String params = cached.substring(open + 1, cached.length() - 1);

        Class<?> type = Class.forName(className, false, classLoader);
        for (Method method : type.getDeclaredMethods()) {
            if (methodName.equals(method.getName())
                    && params.equals(parameterKey(method))) {
                return method;
            }
        }
        return null;
    }

    private static void saveCached(
            Context context,
            CompatibilityProfile.DetectedVersion version,
            String apkFingerprint,
            Resolution resolution
    ) {
        if (resolution.methods.isEmpty()) {
            return;
        }
        try {
            LinkedHashSet<String> signatures = new LinkedHashSet<>();
            for (Method method : resolution.methods) {
                signatures.add(signature(method));
            }
            String prefix = cachePrefix(version);
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .putString(prefix + "format", CACHE_FORMAT)
                    .putString(prefix + "apk", apkFingerprint)
                    .putStringSet(prefix + "boundaries", signatures)
                    .apply();
        } catch (Throwable throwable) {
            log("Adaptive cache save failed: " + describeThrowable(throwable));
        }
    }

    private static List<String> sorted(Set<String> values) {
        ArrayList<String> ordered = new ArrayList<>(values);
        Collections.sort(ordered);
        return ordered;
    }

    private static String sourceDir(Context context) {
        try {
            ApplicationInfo info = context.getApplicationInfo();
            return info == null ? null : info.sourceDir;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String fingerprint(
            String apkPath,
            CompatibilityProfile.DetectedVersion version
    ) {
        File file = new File(apkPath);
        return version.displayCode()
                + ":" + file.length()
                + ":" + file.lastModified();
    }

    private static String cachePrefix(CompatibilityProfile.DetectedVersion version) {
        return "v" + version.displayCode() + ".";
    }

    private static String parameterKey(Method method) {
        StringBuilder value = new StringBuilder();
        for (Class<?> parameter : method.getParameterTypes()) {
            if (value.length() > 0) {
                value.append(',');
            }
            value.append(parameter.getName());
        }
        return value.toString();
    }

    private static String signature(Method method) {
        return method.getDeclaringClass().getName()
                + "#" + method.getName()
                + "(" + parameterKey(method) + ")";
    }

    private static String describeThrowable(Throwable throwable) {
        String message = throwable.getMessage();
        return throwable.getClass().getSimpleName()
                + (message == null ? "" : ": " + message);
    }

    private static void log(String message) {
        XposedBridge.log("[" + TAG + "] " + message);
    }

    static final class Resolution {
        final List<Method> methods;
        final int score;
        final int candidateCount;
        final String source;
        final boolean cacheHit;
        final String reason;

        private Resolution(
                List<Method> methods,
                int score,
                int candidateCount,
                String source,
                boolean cacheHit,
                String reason
        ) {
            this.methods = methods;
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

        static Resolution success(
                List<Method> methods,
                int score,
                int candidateCount,
                String source,
                boolean cacheHit
        ) {
            return new Resolution(
                    methods,
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
                    -1,
                    candidateCount,
                    "none",
                    false,
                    reason
            );
        }
    }

    private static final class Candidate {
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
