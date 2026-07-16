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

/**
 * Pre-render advertisement suppression for the validated X 12.7.1 profile.
 *
 * The primary boundary receives the a6$a post render model before Compose emits
 * the post. Promoted posts are skipped by returning from the void method before
 * its original body runs.
 */
public final class TwitterAdBlocker {
    private static final String TAG = "TwitterHideAds";

    private static final String POST_INTERFACE = "com.x.urt.items.post.a6";
    private static final String PRIMARY_CLASS = "com.x.urt.items.post.c7";
    private static final String SECONDARY_CLASS = "com.x.urt.items.post.e";

    private static final int DETAILED_BLOCK_LOG_LIMIT = 3;
    private static final int PERIODIC_LOG_INTERVAL = 50;

    private static final AtomicBoolean INSTALLED = new AtomicBoolean(false);
    private static final AtomicInteger BLOCK_ATTEMPTS = new AtomicInteger();
    private static final AtomicInteger UNIQUE_BLOCKS = new AtomicInteger();

    private static final Set<String> HOOKED_MEMBERS =
            Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    private static final Set<String> UNIQUE_BLOCK_KEYS =
            Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    private TwitterAdBlocker() {
    }

    public static void install(
            XC_LoadPackage.LoadPackageParam lpparam,
            CompatibilityProfile.DetectedVersion detectedVersion
    ) {
        if (!INSTALLED.compareAndSet(false, true)) {
            return;
        }

        log("Installing pre-render suppression: profile="
                + CompatibilityProfile.PROFILE_ID
                + ", X=" + detectedVersion.displayName()
                + ", classifierSchema=" + BundledAdPatterns.SCHEMA_VERSION
                + ", renderModel=" + BundledAdPatterns.EXPECTED_RENDER_MODEL);

        Class<?> postInterface = XposedHelpers.findClassIfExists(
                POST_INTERFACE,
                lpparam.classLoader
        );

        int primaryHooks = installBoundaryHooks(
                lpparam.classLoader,
                postInterface,
                PRIMARY_CLASS,
                "e",
                "primary-c7.e"
        );

        int secondaryHooks = 0;
        int tertiaryHooks = 0;
        String mode = "ACTIVE_PRIMARY";

        // The fallback methods are nested beneath the primary path. Installing
        // all three causes every normal post to be classified repeatedly. Only
        // use them when the primary boundary is unavailable on the active APK.
        if (primaryHooks == 0) {
            secondaryHooks = installBoundaryHooks(
                    lpparam.classLoader,
                    postInterface,
                    SECONDARY_CLASS,
                    "a",
                    "fallback-e.a"
            );
            tertiaryHooks = installBoundaryHooks(
                    lpparam.classLoader,
                    postInterface,
                    PRIMARY_CLASS,
                    "a",
                    "fallback-c7.a"
            );
            mode = secondaryHooks + tertiaryHooks > 0
                    ? "ACTIVE_FALLBACK"
                    : "FAIL_OPEN_NO_BOUNDARY";
        }

        log("Initialization complete: primaryHooks=" + primaryHooks
                + ", secondaryHooks=" + secondaryHooks
                + ", tertiaryHooks=" + tertiaryHooks
                + ", enforcement=" + mode
                + ", persistence=NOT_REQUIRED"
                + ", normalPostLogging=OFF"
                + ", dexKit=0, globalViewHooks=0, deoptimized=0");
    }

    private static int installBoundaryHooks(
            ClassLoader classLoader,
            Class<?> postInterface,
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
            if (!isRenderBoundary(method, postInterface, methodName)) {
                continue;
            }

            if (hookMemberOnce(method, new XC_MethodHook() {
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
                            BundledAdPatterns.classifyTimelinePost(postModel);
                    if (!classification.promoted) {
                        return;
                    }

                    param.setResult(null);
                    recordBlock(boundary, postModel, classification);
                }
            })) {
                installed++;
                log("Installed pre-render boundary [" + boundary + "]: " + method);
            }
        }
        return installed;
    }

    private static boolean isRenderBoundary(
            Method method,
            Class<?> postInterface,
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
                : first.getName().startsWith("com.x.urt.items.post.a6");
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
