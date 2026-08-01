package my.MrxSiN.twitterhideads;

import android.content.Context;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/** LSPosed entry point. Scope the module only to com.twitter.android. */
public final class XposedInit implements IXposedHookLoadPackage {
    private static final String TAG = "TwitterHideAds";
    private static final String MODULE_VERSION = "1.2.6-test-app-icon";

    private static final AtomicBoolean ATTACH_HOOK_INSTALLED =
            new AtomicBoolean(false);
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) {
        if (!CompatibilityProfile.TARGET_PACKAGE.equals(lpparam.packageName)) {
            return;
        }
        if (lpparam.processName != null
                && !CompatibilityProfile.TARGET_PACKAGE.equals(lpparam.processName)) {
            return;
        }
        if (!ATTACH_HOOK_INSTALLED.compareAndSet(false, true)) {
            return;
        }

        XposedBridge.log("Twitter Hide Ads v" + MODULE_VERSION
                + ": loading in " + lpparam.processName);

        try {
            Class<?> applicationClass = XposedHelpers.findClass(
                    "android.app.Application",
                    lpparam.classLoader
            );
            Class<?> contextClass = XposedHelpers.findClass(
                    "android.content.Context",
                    lpparam.classLoader
            );
            Method attach = applicationClass.getDeclaredMethod("attach", contextClass);
            attach.setAccessible(true);

            XposedBridge.hookMethod(attach, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!INITIALIZED.compareAndSet(false, true)) {
                        return;
                    }

                    Object rawContext = param.args != null && param.args.length > 0
                            ? param.args[0]
                            : null;
                    Context context = rawContext instanceof Context
                            ? (Context) rawContext
                            : null;
                    CompatibilityProfile.DetectedVersion detected =
                            CompatibilityProfile.detect(rawContext);
                    CompatibilityProfile.Profile exact =
                            CompatibilityProfile.selectExact(detected);

                    log("Detected X version=" + detected.displayName()
                            + ", versionCode=" + detected.displayCode()
                            + ", exactProfile="
                            + (exact == null ? "none" : exact.id)
                            + ", adaptiveResolver=ENABLED");

                    try {
                        if (exact != null) {
                            TwitterAdBlocker.installExact(lpparam, detected, exact);
                        } else {
                            log("No exact profile; starting adaptive structural resolution");
                            AdaptiveHookResolver.Resolution resolution =
                                    AdaptiveHookResolver.resolve(
                                            context,
                                            lpparam.classLoader,
                                            detected
                                    );
                            TwitterAdBlocker.installAdaptive(detected, resolution);
                        }
                    } catch (Throwable throwable) {
                        log("Timeline blocker initialization failed; fail-open mode active");
                        XposedBridge.log(throwable);
                    }

                    try {
                        VideoDatasetResolver.Resolution videoResolution =
                                VideoDatasetResolver.resolve(
                                        context,
                                        lpparam.classLoader,
                                        detected
                                );
                        VideoDatasetFilter.install(videoResolution);
                    } catch (Throwable throwable) {
                        log("Video dataset filter failed; timeline blocker remains active");
                        XposedBridge.log(throwable);
                    }
                }
            });

            log("Application attach guard installed");
        } catch (Throwable throwable) {
            log("Unable to install application attach guard; fail-open mode active");
            XposedBridge.log(throwable);
        }
    }

    private static void log(String message) {
        XposedBridge.log("[" + TAG + "] " + message);
    }
}
