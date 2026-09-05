package my.MrxSiN.twitterhideads;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/** Exact and adaptively resolved pre-render promoted-post suppression. */
public final class TwitterAdBlocker {
    private static final String TAG = "TwitterHideAds";

    private static final int DETAILED_BLOCK_LOG_LIMIT = 3;
    private static final int PERIODIC_LOG_INTERVAL = 50;

    private static final AtomicBoolean INSTALLED = new AtomicBoolean(false);
    private static final AtomicInteger BLOCK_ATTEMPTS = new AtomicInteger();
    private static final AtomicInteger UNIQUE_BLOCKS = new AtomicInteger();

    private static final Set<String> HOOKED_MEMBERS =
            Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    private static final Set<String> UNIQUE_BLOCK_KEYS =
            Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    private static final int BOUNDARY_OBSERVATION_LOG_LIMIT = 4;
    private static final ConcurrentHashMap<String, AtomicInteger> BOUNDARY_OBSERVATIONS =
            new ConcurrentHashMap<>();

    private TwitterAdBlocker() {
    }

    static void installExact(
            XC_LoadPackage.LoadPackageParam lpparam,
            CompatibilityProfile.DetectedVersion detectedVersion,
            CompatibilityProfile.Profile profile
    ) {
        if (!INSTALLED.compareAndSet(false, true)) {
            return;
        }

        log("Installing exact pre-render suppression: profile=" + profile.id
                + ", X=" + detectedVersion.displayName()
                + ", classifierSchema=" + BundledAdPatterns.SCHEMA_VERSION
                + ", renderModel=" + profile.expectedRenderModel);

        Class<?> postInterface = XposedHelpers.findClassIfExists(
                profile.postInterface,
                lpparam.classLoader
        );

        int primaryHooks = installExactBoundaryHooks(
                lpparam.classLoader,
                postInterface,
                profile,
                profile.primaryClass,
                profile.primaryMethod,
                "primary-" + simpleName(profile.primaryClass)
                        + "." + profile.primaryMethod
        );

        int secondaryHooks = 0;
        int tertiaryHooks = 0;
        String mode = "ACTIVE_EXACT_PRIMARY";

        if (primaryHooks == 0) {
            secondaryHooks = installExactBoundaryHooks(
                    lpparam.classLoader,
                    postInterface,
                    profile,
                    profile.secondaryClass,
                    profile.secondaryMethod,
                    "fallback-" + simpleName(profile.secondaryClass)
                            + "." + profile.secondaryMethod
            );
            tertiaryHooks = installExactBoundaryHooks(
                    lpparam.classLoader,
                    postInterface,
                    profile,
                    profile.tertiaryClass,
                    profile.tertiaryMethod,
                    "fallback-" + simpleName(profile.tertiaryClass)
                            + "." + profile.tertiaryMethod
            );
            mode = secondaryHooks + tertiaryHooks > 0
                    ? "ACTIVE_EXACT_FALLBACK"
                    : "FAIL_OPEN_NO_BOUNDARY";
        }

        log("Initialization complete: resolver=exact"
                + ", primaryHooks=" + primaryHooks
                + ", secondaryHooks=" + secondaryHooks
                + ", tertiaryHooks=" + tertiaryHooks
                + ", enforcement=" + mode
                + ", persistence=NOT_REQUIRED"
                + ", normalPostLogging=OFF"
                + ", dexKit=0, globalViewHooks=0, deoptimized=0");
    }

    static void installAdaptive(
            CompatibilityProfile.DetectedVersion detectedVersion,
            AdaptiveHookResolver.Resolution resolution
    ) {
        if (!INSTALLED.compareAndSet(false, true)) {
            return;
        }
        if (resolution == null || resolution.method() == null) {
            log("Initialization complete: resolver=adaptive"
                    + ", enforcement=FAIL_OPEN_NO_BOUNDARY"
                    + ", reason=" + (resolution == null ? "null-resolution" : resolution.reason));
            return;
        }

        int installed = 0;
        int deoptimized = 0;
        for (Method method : resolution.methods) {
            String boundary = "adaptive-"
                    + simpleName(method.getDeclaringClass().getName())
                    + "." + method.getName();
            if (!installResolvedBoundary(method, boundary, null, true)) {
                continue;
            }
            installed++;
            // ART inlines these composables into their callers, which would
            // otherwise keep running the compiled copy past the hook.
            if (Deoptimizer.deoptimize(method)) {
                deoptimized++;
            }
            log("Installed pre-render boundary [" + boundary + "]: " + method);
        }
        log("Initialization complete: resolver=adaptive"
                + ", X=" + detectedVersion.displayName()
                + ", boundaries=" + resolution.methods.size()
                + ", installedHooks=" + installed
                + ", topScore=" + resolution.score
                + ", source=" + resolution.source
                + ", cacheHit=" + resolution.cacheHit
                + ", candidateCount=" + resolution.candidateCount
                + ", enforcement=" + (installed > 0 ? "ACTIVE_ADAPTIVE" : "FAIL_OPEN_HOOK_ERROR")
                + ", deoptimizeSupported=" + Deoptimizer.isSupported()
                + ", deoptimized=" + deoptimized);
    }

    private static int installExactBoundaryHooks(
            ClassLoader classLoader,
            Class<?> postInterface,
            final CompatibilityProfile.Profile profile,
            String className,
            String methodName,
            final String boundary
    ) {
        Class<?> type = XposedHelpers.findClassIfExists(className, classLoader);
        if (type == null) {
            log("Render boundary class not found: " + className);
            return 0;
        }

        int installed = 0;
        for (final Method method : type.getDeclaredMethods()) {
            if (!isExactRenderBoundary(method, postInterface, profile, methodName)) {
                continue;
            }
            if (installResolvedBoundary(
                    method,
                    boundary,
                    profile.expectedRenderModel,
                    true
            )) {
                installed++;
                log("Installed pre-render boundary [" + boundary + "]: " + method);
            }
        }
        return installed;
    }

    private static boolean installResolvedBoundary(
            final Method method,
            final String boundary,
            final String expectedRenderModel,
            final boolean allowActionFallback
    ) {
        return hookMemberOnce(method, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (param.args == null || param.args.length == 0) {
                    return;
                }
                Object postModel = param.args[0];
                if (postModel == null) {
                    return;
                }

                BundledAdPatterns.Classification classification =
                        BundledAdPatterns.classifyTimelinePost(
                                postModel,
                                expectedRenderModel,
                                allowActionFallback
                        );
                logBoundaryObservation(boundary, postModel, classification);
                if (!classification.promoted) {
                    return;
                }

                param.setResult(null);
                recordBlock(boundary, postModel, classification);
            }
        });
    }

    private static boolean isExactRenderBoundary(
            Method method,
            Class<?> postInterface,
            CompatibilityProfile.Profile profile,
            String expectedName
    ) {
        int modifiers = method.getModifiers();
        if (!expectedName.equals(method.getName())
                || Modifier.isAbstract(modifiers)
                || Modifier.isNative(modifiers)
                || method.isSynthetic()
                || method.getReturnType() != Void.TYPE) {
            return false;
        }

        Class<?>[] parameters = method.getParameterTypes();
        if (parameters.length == 0 || parameters.length > 10) {
            return false;
        }

        Class<?> first = parameters[0];
        boolean postArgument = postInterface != null
                ? postInterface.isAssignableFrom(first)
                : first.getName().equals(profile.expectedRenderModel)
                || first.getName().startsWith(profile.postInterface);
        if (!postArgument) {
            return false;
        }

        for (Class<?> parameter : parameters) {
            if ("androidx.compose.runtime.Composer".equals(parameter.getName())) {
                return true;
            }
        }
        return false;
    }

    private static int observationCount(String key) {
        AtomicInteger seen = BOUNDARY_OBSERVATIONS.get(key);
        if (seen == null) {
            seen = new AtomicInteger();
            AtomicInteger existing = BOUNDARY_OBSERVATIONS.putIfAbsent(key, seen);
            if (existing != null) {
                seen = existing;
            }
        }
        return seen.incrementAndGet();
    }

    private static void logBoundaryObservation(
            String boundary,
            Object postModel,
            BundledAdPatterns.Classification classification
    ) {
        if (observationCount(boundary) > BOUNDARY_OBSERVATION_LOG_LIMIT) {
            return;
        }
        log("Boundary observation: boundary=" + boundary
                + ", model=" + postModel.getClass().getName()
                + ", promoted=" + classification.promoted
                + ", entryId=" + safeEntryId(classification.entryId)
                + ", signals=" + classification.signals
                + ", actionFallbackUsed=" + classification.actionFallbackUsed);
    }

    private static void recordBlock(
            String boundary,
            Object postModel,
            BundledAdPatterns.Classification classification
    ) {
        int attempts = BLOCK_ATTEMPTS.incrementAndGet();
        String key = classification.stableLogKey(postModel);
        boolean firstObservation = UNIQUE_BLOCK_KEYS.add(key);
        int unique = firstObservation
                ? UNIQUE_BLOCKS.incrementAndGet()
                : UNIQUE_BLOCKS.get();

        if (firstObservation && unique <= DETAILED_BLOCK_LOG_LIMIT) {
            log("Blocked promoted post before Compose: boundary=" + boundary
                    + ", attempts=" + attempts
                    + ", unique=" + unique
                    + ", entryId=" + safeEntryId(classification.entryId)
                    + ", signals=" + classification.signals
                    + ", actionFallbackUsed="
                    + classification.actionFallbackUsed);
            return;
        }

        if (attempts % PERIODIC_LOG_INTERVAL == 0) {
            log("Block summary: attempts=" + attempts
                    + ", unique=" + unique
                    + ", activeBoundary=" + boundary);
        }
    }

    private static boolean hookMemberOnce(Member member, XC_MethodHook hook) {
        String key = member.getDeclaringClass().getName() + "#" + member;
        if (!HOOKED_MEMBERS.add(key)) {
            return false;
        }
        try {
            if (member instanceof Method) {
                ((Method) member).setAccessible(true);
            }
            XposedBridge.hookMethod(member, hook);
            return true;
        } catch (Throwable throwable) {
            HOOKED_MEMBERS.remove(key);
            log("Hook failed for " + member + ": " + describeThrowable(throwable));
            return false;
        }
    }

    private static String simpleName(String className) {
        int dot = className.lastIndexOf('.');
        return dot >= 0 ? className.substring(dot + 1) : className;
    }

    private static String safeEntryId(String entryId) {
        if (entryId == null || entryId.isEmpty()) {
            return "unknown";
        }
        return entryId.length() > 96 ? entryId.substring(0, 96) : entryId;
    }

    private static String describeThrowable(Throwable throwable) {
        String message = throwable.getMessage();
        return throwable.getClass().getSimpleName()
                + (message == null ? "" : ": " + message);
    }

    private static void log(String message) {
        XposedBridge.log("[" + TAG + "] " + message);
    }
}
