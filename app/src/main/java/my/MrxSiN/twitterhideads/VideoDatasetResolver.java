package my.MrxSiN.twitterhideads;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;

import java.io.File;
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
    private static final int ACTIVE_THRESHOLD = 330;
    private static final int MIN_MARGIN = 25;
    private static final int MAX_CLASSES = 500;

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
        String apkPath = sourceDir(context);
        if (apkPath == null) {
            return Resolution.failure("missing-source-apk", 0);
        }
        String fingerprint = fingerprint(apkPath, version);
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
            while (entries.hasMoreElements() && scanned < MAX_CLASSES) {
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
            return Resolution.failure("scan-failed:" + describeThrowable(throwable), candidates.size());
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

    static Candidate evaluate(Method method, String source) {
        int modifiers = method.getModifiers();
        Class<?>[] parameters = method.getParameterTypes();
        Class<?> returnType = method.getReturnType();
        if (!Modifier.isStatic(modifiers)
                || Modifier.isAbstract(modifiers)
                || Modifier.isNative(modifiers)
                || method.isSynthetic()
                || method.isBridge()
                || returnType == Void.TYPE
                || returnType.isPrimitive()
                || parameters.length < 4
                || parameters.length > 8
                || parameters[0] != returnType
                || !isImmutableListLike(parameters[1])
                || !isDirectUrtClass(method.getDeclaringClass().getName())
                || !returnType.getName().startsWith(URT_PREFIX)) {
            return null;
        }

        boolean hasBoolean = false;
        boolean hasInt = false;
        for (int index = 2; index < parameters.length; index++) {
            hasBoolean |= parameters[index] == Boolean.TYPE;
            hasInt |= parameters[index] == Integer.TYPE;
        }
        if (!hasBoolean || !hasInt) {
            return null;
        }

        int score = 0;
        score += 110; // static direct-URT method
        score += 100; // state copy: first parameter equals return type
        score += parameters[1].getName().startsWith("kotlinx.collections.immutable.") ? 95 : 70;
        score += hasBoolean ? 25 : 0;
        score += hasInt ? 25 : 0;
        score += parameters.length == 5 ? 30 : 10;
        score += method.getName().length() <= 2 ? 10 : 0;
        score += Modifier.isPublic(modifiers) ? 8 : 0;
        return new Candidate(method, score, source);
    }

    private static Resolution select(List<Candidate> candidates) {
        if (candidates.isEmpty()) {
            return Resolution.failure("no-structural-candidate", 0);
        }
        candidates.sort(new Comparator<Candidate>() {
            @Override
            public int compare(Candidate left, Candidate right) {
                int score = Integer.compare(right.score, left.score);
                return score != 0 ? score : signature(left.method).compareTo(signature(right.method));
            }
        });
        int limit = Math.min(5, candidates.size());
        for (int index = 0; index < limit; index++) {
            Candidate candidate = candidates.get(index);
            log("Video dataset candidate #" + (index + 1)
                    + ": score=" + candidate.score + ", method=" + candidate.method);
        }

        Candidate best = candidates.get(0);
        int second = candidates.size() > 1 ? candidates.get(1).score : -1;
        if (best.score < ACTIVE_THRESHOLD) {
            return Resolution.failure("score-below-threshold:" + best.score, candidates.size());
        }
        if (second >= 0 && best.score - second < MIN_MARGIN) {
            return Resolution.failure("ambiguous:" + best.score + "/" + second, candidates.size());
        }
        return Resolution.success(best.method, best.score, candidates.size(), best.source, false);
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
            String prefix = prefix(version);
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
                if (!methodName.equals(method.getName()) || !params.equals(parameterKey(method))) {
                    continue;
                }
                Candidate verified = evaluate(method, "cache");
                if (verified == null || verified.score < ACTIVE_THRESHOLD) {
                    return null;
                }
                return Resolution.success(method, Math.max(score, verified.score), 1, "cache", true);
            }
        } catch (Throwable throwable) {
            log("Video dataset cache ignored: " + describeThrowable(throwable));
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
            String prefix = prefix(version);
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putString(prefix + "format", CACHE_FORMAT)
                    .putString(prefix + "apk", fingerprint)
                    .putString(prefix + "class", resolution.method.getDeclaringClass().getName())
                    .putString(prefix + "method", resolution.method.getName())
                    .putString(prefix + "params", parameterKey(resolution.method))
                    .putInt(prefix + "score", resolution.score)
                    .apply();
        } catch (Throwable throwable) {
            log("Video dataset cache save failed: " + describeThrowable(throwable));
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

    private static String fingerprint(String apkPath, CompatibilityProfile.DetectedVersion version) {
        File file = new File(apkPath);
        return version.displayCode() + ":" + file.length() + ":" + file.lastModified();
    }

    private static String prefix(CompatibilityProfile.DetectedVersion version) {
        return "v" + version.displayCode() + ".";
    }

    private static String parameterKey(Method method) {
        StringBuilder builder = new StringBuilder();
        for (Class<?> parameter : method.getParameterTypes()) {
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(parameter.getName());
        }
        return builder.toString();
    }

    private static String signature(Method method) {
        return method.getDeclaringClass().getName() + "#" + method.getName()
                + "(" + parameterKey(method) + ")";
    }

    private static String describeThrowable(Throwable throwable) {
        String message = throwable.getMessage();
        return throwable.getClass().getSimpleName() + (message == null ? "" : ": " + message);
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

        Candidate(Method method, int score, String source) {
            this.method = method;
            this.score = score;
            this.source = source;
        }
    }
}
