package my.MrxSiN.twitterhideads;

import android.content.Context;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.libxposed.api.XposedModule;

/** Modern Xposed API entry point. Scoped to com.twitter.android. */
public final class ModuleMain extends XposedModule {
    private static final String MODULE_VERSION = "2.1.0";

    private static final AtomicBoolean ATTACH_HOOK_INSTALLED =
            new AtomicBoolean(false);
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    private volatile String processName = "";
    private volatile ClassLoader hostClassLoader;

    @Override
    public void onModuleLoaded(ModuleLoadedParam param) {
        ModuleRuntime.attach(this);
        processName = param.getProcessName();
    }

    @Override
    public void onPackageReady(PackageReadyParam param) {
        if (!CompatibilityProfile.TARGET_PACKAGE.equals(param.getPackageName())) {
            return;
        }
        if (!processName.isEmpty()
                && !CompatibilityProfile.TARGET_PACKAGE.equals(processName)) {
            return;
        }
        if (!ATTACH_HOOK_INSTALLED.compareAndSet(false, true)) {
            return;
        }

        hostClassLoader = param.getClassLoader();
        ModuleRuntime.log("Twitter Hide Ads v" + MODULE_VERSION
                + ": loading in " + processName
                + ", framework=" + getFrameworkName()
                + " " + getFrameworkVersion()
                + ", api=" + getApiVersion());
        installAttachGuard(hostClassLoader);
    }

    /**
     * Waits for {@code Application.attach()} instead of running discovery from
     * the package callback, so X has supplied its real application context
     * before any boundary is resolved.
     */
    private void installAttachGuard(ClassLoader classLoader) {
        try {
            Class<?> applicationClass = Class.forName(
                    "android.app.Application",
                    false,
                    classLoader
            );
            Method attach = applicationClass.getDeclaredMethod("attach", Context.class);

            ModuleRuntime.hook(attach, chain -> {
                List<Object> args = chain.getArgs();
                Object result = chain.proceed();
                bootstrap(args.isEmpty() ? null : args.get(0));
                return result;
            });

            ModuleRuntime.log("Application attach guard installed");
        } catch (Throwable throwable) {
            ModuleRuntime.log(
                    "Unable to install application attach guard; fail-open mode active",
                    throwable
            );
        }
    }

    private void bootstrap(Object rawContext) {
        if (!INITIALIZED.compareAndSet(false, true)) {
            return;
        }

        Context context = rawContext instanceof Context ? (Context) rawContext : null;
        CompatibilityProfile.DetectedVersion detected =
                CompatibilityProfile.detect(context);
        CompatibilityProfile.Profile exact = CompatibilityProfile.selectExact(detected);

        ModuleRuntime.log("Detected X version=" + detected.displayName()
                + ", versionCode=" + detected.displayCode()
                + ", exactProfile=" + (exact == null ? "none" : exact.id)
                + ", adaptiveResolver=ENABLED");

        installTimelineBlocker(context, detected, exact);
        installVideoDatasetFilter(context, detected);
    }

    private void installTimelineBlocker(
            Context context,
            CompatibilityProfile.DetectedVersion detected,
            CompatibilityProfile.Profile exact
    ) {
        try {
            if (exact != null) {
                TwitterAdBlocker.installExact(hostClassLoader, detected, exact);
                return;
            }
            ModuleRuntime.log(
                    "No exact profile; starting adaptive structural resolution"
            );
            AdaptiveHookResolver.Resolution resolution = AdaptiveHookResolver.resolve(
                    context,
                    hostClassLoader,
                    detected
            );
            TwitterAdBlocker.installAdaptive(detected, resolution);
        } catch (Throwable throwable) {
            ModuleRuntime.log(
                    "Timeline blocker initialization failed; fail-open mode active",
                    throwable
            );
        }
    }

    private void installVideoDatasetFilter(
            Context context,
            CompatibilityProfile.DetectedVersion detected
    ) {
        try {
            VideoDatasetResolver.Resolution videoResolution =
                    VideoDatasetResolver.resolve(context, hostClassLoader, detected);
            VideoDatasetFilter.install(videoResolution);
        } catch (Throwable throwable) {
            ModuleRuntime.log(
                    "Video dataset filter failed; timeline blocker remains active",
                    throwable
            );
        }
    }
}
