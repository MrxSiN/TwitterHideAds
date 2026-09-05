package my.MrxSiN.twitterhideads;

import java.lang.reflect.Member;
import java.lang.reflect.Method;

import de.robv.android.xposed.XposedBridge;

/**
 * Forces ART to stop serving an already compiled copy of a hooked method.
 *
 * Compose render boundaries are small static methods that ART inlines into
 * their callers. A hook installed on such a method is never reached, because
 * the caller keeps executing the inlined copy. LSPosed exposes
 * {@code XposedBridge.deoptimizeMethod} to discard those compiled copies; it is
 * absent from the Xposed API 82 artifact this module compiles against, so it is
 * resolved reflectively and treated as unavailable when missing.
 */
final class Deoptimizer {
    private static final Method DEOPTIMIZE = resolve();

    private Deoptimizer() {
    }

    /** Returns true when the platform can deoptimize methods at all. */
    static boolean isSupported() {
        return DEOPTIMIZE != null;
    }

    /** Returns true when {@code member} was deoptimized. */
    static boolean deoptimize(Member member) {
        if (DEOPTIMIZE == null || member == null) {
            return false;
        }
        try {
            DEOPTIMIZE.invoke(null, member);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Method resolve() {
        try {
            Method method = XposedBridge.class.getDeclaredMethod(
                    "deoptimizeMethod",
                    Member.class
            );
            method.setAccessible(true);
            return method;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
