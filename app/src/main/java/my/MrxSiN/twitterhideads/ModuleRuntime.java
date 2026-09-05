package my.MrxSiN.twitterhideads;

import android.util.Log;

import java.lang.reflect.Executable;

import io.github.libxposed.api.XposedInterface;

/**
 * Process-wide access to the framework interface handed to the module entry.
 *
 * The modern Xposed API exposes hooking, deoptimization and logging through the
 * {@link XposedInterface} instance attached to the module entry class instead of
 * the static {@code XposedBridge} of the legacy API, so the resolvers and
 * filters reach it through this holder.
 */
final class ModuleRuntime {
    private static final String TAG = "TwitterHideAds";

    private static volatile XposedInterface API;

    private ModuleRuntime() {
    }

    static void attach(XposedInterface api) {
        API = api;
    }

    /** Returns true once the framework interface is available. */
    static boolean isAttached() {
        return API != null;
    }

    static void log(String message) {
        XposedInterface api = API;
        if (api == null) {
            Log.i(TAG, message);
            return;
        }
        api.log(Log.INFO, TAG, message);
    }

    static void log(String message, Throwable throwable) {
        XposedInterface api = API;
        if (api == null) {
            Log.e(TAG, message, throwable);
            return;
        }
        api.log(Log.ERROR, TAG, message, throwable);
    }

    /**
     * Installs a hook, or returns {@code null} when the framework interface is
     * missing or the framework rejected the hook.
     */
    static XposedInterface.HookHandle hook(
            Executable origin,
            XposedInterface.Hooker hooker
    ) {
        XposedInterface api = API;
        if (api == null || origin == null) {
            return null;
        }
        return api.hook(origin).intercept(hooker);
    }

    /**
     * Forces ART to stop serving an already compiled copy of {@code executable}.
     *
     * Compose render boundaries are small static methods that ART inlines into
     * their callers. A hook installed on such a method is never reached, because
     * the caller keeps executing the inlined copy.
     */
    static boolean deoptimize(Executable executable) {
        XposedInterface api = API;
        if (api == null || executable == null) {
            return false;
        }
        try {
            return api.deoptimize(executable);
        } catch (Throwable ignored) {
            return false;
        }
    }
}
