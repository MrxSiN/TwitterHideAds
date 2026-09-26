package my.MrxSiN.twitterhideads;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Persistence for resolved render boundaries, keyed by X version and APK
 * fingerprint so an X update forces a rescan.
 */
final class AdaptiveBoundaryCache {
    private static final String PREFS = "twitterhideads_adaptive_profile";
    private static final String CACHE_FORMAT = "5";

    private AdaptiveBoundaryCache() {
    }

    static AdaptiveHookResolver.Resolution load(
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
                if (!RenderBoundaryShape.accepts(method)) {
                    return null;
                }
                int score = RenderBoundaryShape.score(method);
                topScore = Math.max(topScore, score);
                methods.add(method);
            }
            // A caller that no longer resolves only loses its deoptimization.
            ArrayList<Method> callers = new ArrayList<>();
            Set<String> callerSignatures = prefs.getStringSet(
                    prefix + "callers",
                    Collections.<String>emptySet()
            );
            for (String cached : sorted(callerSignatures)) {
                try {
                    Method caller = resolveSignature(cached, classLoader);
                    if (caller != null) {
                        callers.add(caller);
                    }
                } catch (Throwable ignored) {
                    // skip
                }
            }
            return AdaptiveHookResolver.Resolution.success(
                    methods,
                    topScore,
                    methods.size(),
                    "cache",
                    true
            ).withCallers(callers);
        } catch (Throwable throwable) {
            AdaptiveHookResolver.log(
                    "Adaptive cache ignored: " + Reflect.describeThrowable(throwable)
            );
        }
        return null;
    }

    static void save(
            Context context,
            CompatibilityProfile.DetectedVersion version,
            String apkFingerprint,
            AdaptiveHookResolver.Resolution resolution
    ) {
        if (resolution.methods.isEmpty()) {
            return;
        }
        try {
            LinkedHashSet<String> signatures = new LinkedHashSet<>();
            for (Method method : resolution.methods) {
                signatures.add(signature(method));
            }
            LinkedHashSet<String> callers = new LinkedHashSet<>();
            for (Method caller : resolution.callers) {
                callers.add(signature(caller));
            }
            String prefix = cachePrefix(version);
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .putString(prefix + "format", CACHE_FORMAT)
                    .putString(prefix + "apk", apkFingerprint)
                    .putStringSet(prefix + "boundaries", signatures)
                    .putStringSet(prefix + "callers", callers)
                    .apply();
        } catch (Throwable throwable) {
            AdaptiveHookResolver.log(
                    "Adaptive cache save failed: " + Reflect.describeThrowable(throwable)
            );
        }
    }

    static Method resolveSignature(String cached, ClassLoader classLoader)
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

    private static List<String> sorted(Set<String> values) {
        ArrayList<String> ordered = new ArrayList<>(values);
        Collections.sort(ordered);
        return ordered;
    }

    static String sourceDir(Context context) {
        try {
            ApplicationInfo info = context.getApplicationInfo();
            return info == null ? null : info.sourceDir;
        } catch (Throwable ignored) {
            return null;
        }
    }

    static String fingerprint(
            String apkPath,
            CompatibilityProfile.DetectedVersion version
    ) {
        File file = new File(apkPath);
        return version.displayCode()
                + ":" + file.length()
                + ":" + file.lastModified();
    }

    static String cachePrefix(CompatibilityProfile.DetectedVersion version) {
        return "v" + version.displayCode() + ".";
    }

    static String parameterKey(Method method) {
        StringBuilder value = new StringBuilder();
        for (Class<?> parameter : method.getParameterTypes()) {
            if (value.length() > 0) {
                value.append(',');
            }
            value.append(parameter.getName());
        }
        return value.toString();
    }

    static String signature(Method method) {
        return method.getDeclaringClass().getName()
                + "#" + method.getName()
                + "(" + parameterKey(method) + ")";
    }
}
