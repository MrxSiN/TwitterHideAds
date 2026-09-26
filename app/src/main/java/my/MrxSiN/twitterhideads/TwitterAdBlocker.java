package my.MrxSiN.twitterhideads;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import io.github.libxposed.api.XposedInterface;

/** Exact and adaptively resolved pre-render promoted-post suppression. */
public final class TwitterAdBlocker {
    private static final AtomicBoolean INSTALLED = new AtomicBoolean(false);
    private static final AtomicInteger REJECTED_BOUNDARIES = new AtomicInteger();

    private static final Set<String> HOOKED_MEMBERS =
            Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    private static final BlockRecorder BLOCKS = new BlockRecorder();

    private TwitterAdBlocker() {
    }

    static void installExact(
            ClassLoader classLoader,
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

        Class<?> postInterface = Reflect.findClassIfExists(
                profile.postInterface,
                classLoader
        );

        int primaryHooks = installExactBoundaryHooks(
                classLoader,
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
                    classLoader,
                    postInterface,
                    profile,
                    profile.secondaryClass,
                    profile.secondaryMethod,
                    "fallback-" + simpleName(profile.secondaryClass)
                            + "." + profile.secondaryMethod
            );
            tertiaryHooks = installExactBoundaryHooks(
                    classLoader,
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
        for (Method method : resolution.methods) {
            String boundary = "adaptive-"
                    + simpleName(method.getDeclaringClass().getName())
                    + "." + method.getName();
            if (!installResolvedBoundary(
                    method,
                    boundary,
                    null,
                    new BoundaryWitness(PolicyLimits.witnessWindow())
            )) {
                continue;
            }
            installed++;
            log("Installed pre-render boundary [" + boundary + "]: " + method);
        }
        // X's AOT code inlines these composables into their callers, which
        // would otherwise keep running the inlined copy past the hook.
        int deoptimized = 0;
        for (Method caller : resolution.callers) {
            if (ModuleRuntime.deoptimize(caller)) {
                deoptimized++;
            }
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
                + ", deoptimizeSupported=" + ModuleRuntime.isAttached()
                + ", inlinedCallers=" + resolution.callers.size()
                + ", deoptimized=" + deoptimized
                + ", witnessWindow=" + PolicyLimits.witnessWindow()
                + ", " + PolicyStats.summary());
    }

    private static int installExactBoundaryHooks(
            ClassLoader classLoader,
            Class<?> postInterface,
            final CompatibilityProfile.Profile profile,
            String className,
            String methodName,
            final String boundary
    ) {
        Class<?> type = Reflect.findClassIfExists(className, classLoader);
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
                    null
            )) {
                installed++;
                log("Installed pre-render boundary [" + boundary + "]: " + method);
            }
        }
        return installed;
    }

    /**
     * A promoted post is suppressed by returning without calling
     * {@link XposedInterface.Chain#proceed()}, which is how the modern API
     * declines to run the original void render call. Normal posts proceed
     * untouched.
     *
     * An adaptive boundary carries a {@link BoundaryWitness}; exact boundaries
     * are validated per X version and pass {@code null}.
     */
    private static boolean installResolvedBoundary(
            final Method method,
            final String boundary,
            final String expectedRenderModel,
            final BoundaryWitness witness
    ) {
        final AtomicReference<XposedInterface.HookHandle> handle = new AtomicReference<>();
        return hookMemberOnce(method, handle, chain -> {
            if (witness != null && witness.state() == BoundaryWitness.State.REJECTED) {
                return chain.proceed();
            }
            List<Object> args = chain.getArgs();
            Object postModel = args.isEmpty() ? null : args.get(0);
            if (postModel == null) {
                return chain.proceed();
            }

            // One Brainfuck policy call; allocation-free unless the model
            // needs the fallback walk or is blocked.
            int result = BundledAdPatterns.classifyPacked(postModel, expectedRenderModel, true);
            if (BlockRecorder.OBSERVE) {
                BLOCKS.observe(boundary, postModel,
                        BundledAdPatterns.classifyTimelinePost(postModel, expectedRenderModel, true));
            }
            if (witness != null) {
                witness(boundary, witness, handle.get(), postModel,
                        (result & BundledAdPatterns.IDENTIFIES_POST) != 0);
            }
            if ((result & BundledAdPatterns.PROMOTED) == 0) {
                return chain.proceed();
            }

            PolicyStats.blocked();
            BLOCKS.record(boundary, postModel,
                    BundledAdPatterns.classifyTimelinePost(postModel, expectedRenderModel, true));
            return null;
        });
    }

    private static void witness(
            String boundary,
            BoundaryWitness witness,
            XposedInterface.HookHandle handle,
            Object postModel,
            boolean identifiesPost
    ) {
        BoundaryWitness.State transition = witness.record(identifiesPost);
        if (transition == BoundaryWitness.State.CONFIRMED) {
            log("Boundary confirmed as post renderer: boundary=" + boundary
                    + ", model=" + postModel.getClass().getName());
        } else if (transition == BoundaryWitness.State.REJECTED) {
            boolean unhooked = unhook(handle);
            log("Boundary rejected after witness window: boundary=" + boundary
                    + ", invocations=" + witness.observed()
                    + ", lastModel=" + postModel.getClass().getName()
                    + ", unhooked=" + unhooked
                    + ", rejectedTotal=" + REJECTED_BOUNDARIES.incrementAndGet());
        }
    }

    private static boolean unhook(XposedInterface.HookHandle handle) {
        if (handle == null) {
            return false;
        }
        try {
            handle.unhook();
            return true;
        } catch (Throwable throwable) {
            log("Unhook failed; boundary stays pass-through: "
                    + Reflect.describeThrowable(throwable));
            return false;
        }
    }

    /**
     * Facts about {@code method} for OP_EXACT_BOUNDARY; the rule itself is in
     * discovery.bf. Any policy failure rejects the method (no hook).
     */
    private static boolean isExactRenderBoundary(
            Method method,
            Class<?> postInterface,
            CompatibilityProfile.Profile profile,
            String expectedName
    ) {
        Class<?>[] parameters = method.getParameterTypes();
        if (parameters.length > 255) {
            return false;
        }
        boolean postArgument = false;
        if (parameters.length > 0) {
            Class<?> first = parameters[0];
            postArgument = postInterface != null
                    ? postInterface.isAssignableFrom(first)
                    : first.getName().equals(profile.expectedRenderModel)
                    || first.getName().startsWith(profile.postInterface);
        }
        int modifiers = method.getModifiers();
        PolicyFrame frame = PolicyFrame.begin(BfAbi.OP_EXACT_BOUNDARY);
        try {
            frame.bool(expectedName.equals(method.getName()));
            frame.bool(Modifier.isAbstract(modifiers));
            frame.bool(Modifier.isNative(modifiers));
            frame.bool(method.isSynthetic());
            frame.bool(method.getReturnType() == Void.TYPE);
            frame.bool(postArgument);
            frame.u8(parameters.length);
            for (Class<?> parameter : parameters) {
                frame.u8(RenderBoundaryShape.COMPOSER.equals(parameter.getName())
                        ? BfAbi.R_COMPOSER : BfAbi.R_OTHER);
            }
            return frame.send(BfAbi.PROG_DISCOVERY, 1) && frame.out(0) == 1;
        } finally {
            frame.release();
        }
    }

    private static boolean hookMemberOnce(
            Executable member,
            AtomicReference<XposedInterface.HookHandle> handleOut,
            XposedInterface.Hooker hooker
    ) {
        String key = member.getDeclaringClass().getName() + "#" + member;
        if (!HOOKED_MEMBERS.add(key)) {
            return false;
        }
        try {
            XposedInterface.HookHandle handle = ModuleRuntime.hook(member, hooker);
            if (handle == null) {
                HOOKED_MEMBERS.remove(key);
                log("Hook failed for " + member + ": framework interface unavailable");
                return false;
            }
            handleOut.set(handle);
            return true;
        } catch (Throwable throwable) {
            HOOKED_MEMBERS.remove(key);
            log("Hook failed for " + member + ": " + Reflect.describeThrowable(throwable));
            return false;
        }
    }

    private static String simpleName(String className) {
        int dot = className.lastIndexOf('.');
        return dot >= 0 ? className.substring(dot + 1) : className;
    }

    private static void log(String message) {
        ModuleRuntime.log(message);
    }
}
