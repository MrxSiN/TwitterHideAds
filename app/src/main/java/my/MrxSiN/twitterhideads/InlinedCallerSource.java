package my.MrxSiN.twitterhideads;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Finds the direct callers of resolved render boundaries.
 *
 * X ships speed-profile AOT code, and the AOT compiler inlines small
 * composables into their callers. A hook on the boundary never runs for such
 * a call site, because the caller executes its own inlined copy. The modern
 * API's {@code deoptimize} exists for exactly this case and takes the caller,
 * not the hooked callee.
 */
final class InlinedCallerSource {
    private InlinedCallerSource() {
    }

    static List<Method> find(
            String apkPath,
            ClassLoader classLoader,
            List<Method> boundaries
    ) {
        // Upper bound on deoptimized callers, which then run interpreted or JIT-compiled.
        int maxCallers = PolicyLimits.maxCallers();
        Map<String, Method> callers = new LinkedHashMap<>();
        try (DexKitBridge bridge = DexKitBridge.create(apkPath)) {
            for (Method boundary : boundaries) {
                MethodData data = bridge.getMethodData(boundary);
                if (data == null) {
                    continue;
                }
                for (MethodData caller : data.getCallers()) {
                    if (callers.size() >= maxCallers) {
                        break;
                    }
                    if (caller.isConstructor() || caller.isStaticInitializer()) {
                        continue;
                    }
                    try {
                        Method method = caller.getMethodInstance(classLoader);
                        callers.put(AdaptiveBoundaryCache.signature(method), method);
                    } catch (Throwable ignored) {
                        // A caller that cannot resolve in the host loader cannot be deoptimized.
                    }
                }
            }
        } catch (Throwable throwable) {
            AdaptiveHookResolver.log("Inlined caller lookup failed; boundaries may be bypassed "
                    + "by AOT-inlined call sites: " + Reflect.describeThrowable(throwable));
        }
        for (Method boundary : boundaries) {
            callers.remove(AdaptiveBoundaryCache.signature(boundary));
        }
        return new ArrayList<>(callers.values());
    }
}
