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
 * Resolves a renamed X post-render boundary by structure instead of class names.
 *
 * The accepted shape is intentionally narrow and mirrors the primary boundary
 * observed in X 12.7.1, 12.8.0 and 12.9.1:
 *
 *   static void method(PostInterface, Modifier, PostDependency, LayoutScope,
 *                      Composer, int, int)
 */
final class AdaptiveHookResolver {
    private static final String TAG = "TwitterHideAds";
    private static final String POST_PACKAGE = "com.x.urt.items.post.";
    private static final String COMPOSER = "androidx.compose.runtime.Composer";
    private static final String MODIFIER_PREFIX = "androidx.compose.ui.Modifier";
    private static final String LAYOUT_PREFIX = "androidx.compose.foundation.layout.";

    private static final String PREFS = "twitterhideads_adaptive_profile";
    private static final String CACHE_FORMAT = "2";
    private static final int ACTIVE_THRESHOLD = 320;
    private static final int MIN_WIN_MARGIN = 25;
    private static final int MAX_REFLECTION_CLASSES = 1500;

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
            log("Adaptive cache hit: method=" + cached.method
                    + ", score=" + cached.score);
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
        if (selected.method != null) {
            saveCached(context, version, apkFingerprint, selected);
            log("Adaptive resolver selected: method=" + selected.method
                    + ", score=" + selected.score
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

    private static List<Candidate> findWithDexKit(
            String apkPath,
            ClassLoader classLoader
    ) throws Exception {
        ArrayList<Candidate> candidates = new ArrayList<>();
        try (DexKitBridge bridge = DexKitBridge.create(apkPath)) {
            MethodMatcher matcher = MethodMatcher.create()
                    .modifiers(Modifier.STATIC)
                    .returnType("void")
                    .paramTypes(
                            (String) null,
                            null,
                            null,
                            null,
                            COMPOSER,
                            "int",
                            "int"
                    );
            FindMethod query = FindMethod.create()
                    .searchPackages("com.x.urt.items.post")
                    .matcher(matcher);
            MethodDataList result = bridge.findMethod(query);
            for (MethodData data : result) {
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
                    + ", eligible=" + candidates.size());
        }
        return candidates;
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
                if (!className.startsWith(POST_PACKAGE)) {
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
        int modifiers = method.getModifiers();
        Class<?>[] parameters = method.getParameterTypes();
        if (!Modifier.isStatic(modifiers)
                || Modifier.isAbstract(modifiers)
                || Modifier.isNative(modifiers)
                || method.isSynthetic()
                || method.getReturnType() != Void.TYPE
                || parameters.length != 7
                || !COMPOSER.equals(parameters[4].getName())
                || parameters[5] != Integer.TYPE
                || parameters[6] != Integer.TYPE) {
            return null;
        }

        String declaring = method.getDeclaringClass().getName();
        String first = parameters[0].getName();
        if (!declaring.startsWith(POST_PACKAGE)
                || !first.startsWith(POST_PACKAGE)) {
            return null;
        }

        int score = 0;
        score += Modifier.isStatic(modifiers) ? 25 : 0;
        score += method.getReturnType() == Void.TYPE ? 25 : 0;
        score += directPostPackage(declaring) ? 80 : 25;
        score += first.startsWith(POST_PACKAGE) ? 80 : 0;
        score += parameters[0].isInterface() ? 15 : 0;
        score += COMPOSER.equals(parameters[4].getName()) ? 60 : 0;
        score += parameters[1].getName().startsWith(MODIFIER_PREFIX) ? 35 : 0;
        score += parameters[2].getName().startsWith(POST_PACKAGE) ? 20 : 0;
        score += parameters[3].getName().startsWith(LAYOUT_PREFIX) ? 35 : 0;
        score += parameters[5] == Integer.TYPE && parameters[6] == Integer.TYPE
                ? 25 : 0;
        score += method.getName().length() <= 2 ? 10 : 0;

        if (score < ACTIVE_THRESHOLD - 40) {
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
            String key = signature(candidate.method);
            if (seen.add(key)) {
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

        Candidate best = candidates.get(0);
        int secondScore = candidates.size() > 1 ? candidates.get(1).score : -1;
        if (best.score < ACTIVE_THRESHOLD) {
            return Resolution.failure(
                    "best-score-below-threshold:" + best.score,
                    candidates.size()
            );
        }
        if (secondScore >= 0 && best.score - secondScore < MIN_WIN_MARGIN) {
            logCandidateSummary(candidates);
            return Resolution.failure(
                    "ambiguous-top-candidates:" + best.score + "/" + secondScore,
                    candidates.size()
            );
        }
        return Resolution.success(
                best.method,
                best.score,
                candidates.size(),
                best.source,
                false
        );
    }

    private static void logCandidateSummary(List<Candidate> candidates) {
        int limit = Math.min(5, candidates.size());
        for (int index = 0; index < limit; index++) {
            Candidate candidate = candidates.get(index);
            log("Adaptive candidate #" + (index + 1)
                    + ": score=" + candidate.score
                    + ", source=" + candidate.source
                    + ", method=" + candidate.method);
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
            String className = prefs.getString(prefix + "class", null);
            String methodName = prefs.getString(prefix + "method", null);
            String params = prefs.getString(prefix + "params", null);
            int score = prefs.getInt(prefix + "score", -1);
            if (className == null || methodName == null || params == null) {
                return null;
            }
            Class<?> type = Class.forName(className, false, classLoader);
            for (Method method : type.getDeclaredMethods()) {
                if (!methodName.equals(method.getName())
                        || !params.equals(parameterKey(method))) {
                    continue;
                }
                Candidate verified = evaluate(method, "cache");
                if (verified == null || verified.score < ACTIVE_THRESHOLD) {
                    return null;
                }
                return Resolution.success(
                        method,
                        Math.max(score, verified.score),
                        1,
                        "cache",
                        true
                );
            }
        } catch (Throwable throwable) {
            log("Adaptive cache ignored: " + describeThrowable(throwable));
        }
        return null;
    }

    private static void saveCached(
            Context context,
            CompatibilityProfile.DetectedVersion version,
            String apkFingerprint,
            Resolution resolution
    ) {
        if (resolution.method == null) {
            return;
        }
        try {
            String prefix = cachePrefix(version);
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .putString(prefix + "format", CACHE_FORMAT)
                    .putString(prefix + "apk", apkFingerprint)
                    .putString(prefix + "class", resolution.method.getDeclaringClass().getName())
                    .putString(prefix + "method", resolution.method.getName())
                    .putString(prefix + "params", parameterKey(resolution.method))
                    .putInt(prefix + "score", resolution.score)
                    .apply();
        } catch (Throwable throwable) {
            log("Adaptive cache save failed: " + describeThrowable(throwable));
        }
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

    private static boolean directPostPackage(String className) {
        int lastDot = className.lastIndexOf('.');
        return lastDot >= 0
                && "com.x.urt.items.post".equals(className.substring(0, lastDot));
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
        final Method method;
        final int score;
        final int candidateCount;
        final String source;
        final boolean cacheHit;
        final String reason;

        private Resolution(
                Method method,
                int score,
                int candidateCount,
                String source,
                boolean cacheHit,
                String reason
        ) {
            this.method = method;
            this.score = score;
            this.candidateCount = candidateCount;
            this.source = source;
            this.cacheHit = cacheHit;
            this.reason = reason;
        }

        static Resolution success(
                Method method,
                int score,
                int candidateCount,
                String source,
                boolean cacheHit
        ) {
            return new Resolution(
                    method,
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
                    null,
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
