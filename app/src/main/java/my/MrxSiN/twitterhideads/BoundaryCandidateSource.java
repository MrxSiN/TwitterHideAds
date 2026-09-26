package my.MrxSiN.twitterhideads;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.MethodDataList;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import dalvik.system.DexFile;

/**
 * Discovery of adaptive render-boundary candidates, through a whole-APK DexKit
 * query first and a namespace-bounded {@link DexFile} scan when DexKit is
 * unavailable.
 */
final class BoundaryCandidateSource {
    /**
     * Namespaces that have declared post render boundaries: the post package
     * itself, com.x.jetfuel from X 12.22.0 and com.x.mappers from X 12.28.0.
     */
    static final String[] FALLBACK_NAMESPACES = {
            "com.x.urt.",
            "com.x.jetfuel.",
            "com.x.mappers."
    };

    private static final String POST_MODEL_DESCRIPTOR =
            "L" + RenderBoundaryShape.POST_PACKAGE_ROOT.replace('.', '/') + "/";

    private BoundaryCandidateSource() {
    }

    /**
     * Asks DexKit for every static void method in the application and lets
     * {@link RenderBoundaryShape} decide which ones are boundaries. Encoding
     * the parameter layout in the query itself would re-break on the next
     * release that adds or drops a parameter, and restricting the search to the
     * post package hides boundaries declared elsewhere.
     *
     * Results are pre-filtered on the raw descriptor so that only methods
     * taking a post model first are loaded through the host class loader, which
     * is the expensive step.
     */
    static List<AdaptiveHookResolver.Candidate> findWithDexKit(
            String apkPath,
            ClassLoader classLoader
    ) throws Exception {
        ArrayList<AdaptiveHookResolver.Candidate> candidates = new ArrayList<>();
        try (DexKitBridge bridge = DexKitBridge.create(apkPath)) {
            MethodMatcher matcher = MethodMatcher.create()
                    .modifiers(Modifier.STATIC)
                    .returnType("void");
            FindMethod query = FindMethod.create().matcher(matcher);
            MethodDataList result = bridge.findMethod(query);
            int inspected = 0;
            for (MethodData data : result) {
                if (!takesPostModelFirst(data)) {
                    continue;
                }
                inspected++;
                try {
                    Method method = data.getMethodInstance(classLoader);
                    AdaptiveHookResolver.Candidate candidate = evaluate(method, "dexkit");
                    if (candidate != null) {
                        candidates.add(candidate);
                    }
                } catch (Throwable ignored) {
                    // A metadata result that cannot resolve in the host loader is unusable.
                }
            }
            AdaptiveHookResolver.log("DexKit structural query: dexFiles=" + bridge.getDexNum()
                    + ", rawMatches=" + result.size()
                    + ", postModelFirst=" + inspected
                    + ", eligible=" + candidates.size());
        }
        return candidates;
    }

    /** Cheap descriptor test that avoids loading every static void method. */
    private static boolean takesPostModelFirst(MethodData data) {
        try {
            String descriptor = data.getDescriptor();
            int open = descriptor.indexOf('(');
            return open >= 0
                    && descriptor.startsWith(POST_MODEL_DESCRIPTOR, open + 1);
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Bounded fallback for when DexKit cannot load. Loading a class through
     * the host loader is the expensive step and X ships tens of thousands of
     * com.x classes, so this scans only the namespaces render boundaries have
     * been observed in ({@link #FALLBACK_NAMESPACES}), post package first, and
     * then applies the same {@link RenderBoundaryShape} test as DexKit. A
     * boundary relocated outside those namespaces is found only by DexKit.
     */
    static List<AdaptiveHookResolver.Candidate> findWithDexFile(
            String apkPath,
            ClassLoader classLoader
    ) throws Exception {
        List<String> classNames;
        DexFile dexFile = new DexFile(apkPath);
        try {
            classNames = fallbackScanOrder(dexFile.entries());
        } finally {
            dexFile.close();
        }

        ArrayList<AdaptiveHookResolver.Candidate> candidates = new ArrayList<>();
        int limit = Math.min(PolicyLimits.reflectionClasses(), classNames.size());
        for (int index = 0; index < limit; index++) {
            Class<?> type;
            try {
                type = Class.forName(classNames.get(index), false, classLoader);
            } catch (Throwable ignored) {
                continue;
            }
            for (Method method : type.getDeclaredMethods()) {
                AdaptiveHookResolver.Candidate candidate = evaluate(method, "dexfile");
                if (candidate != null) {
                    candidates.add(candidate);
                }
            }
        }
        AdaptiveHookResolver.log("DexFile structural fallback: namespaceClasses="
                + classNames.size()
                + ", scannedClasses=" + limit
                + ", truncated=" + (classNames.size() > limit)
                + ", eligible=" + candidates.size());
        return candidates;
    }

    /**
     * Classes under {@link #FALLBACK_NAMESPACES}, with the post package first
     * so truncation drops the least likely namespaces.
     */
    static List<String> fallbackScanOrder(Enumeration<String> entries) {
        ArrayList<String> post = new ArrayList<>();
        ArrayList<String> other = new ArrayList<>();
        while (entries.hasMoreElements()) {
            String className = entries.nextElement();
            if (className.startsWith(RenderBoundaryShape.POST_PACKAGE)) {
                post.add(className);
                continue;
            }
            for (String namespace : FALLBACK_NAMESPACES) {
                if (className.startsWith(namespace)) {
                    other.add(className);
                    break;
                }
            }
        }
        post.addAll(other);
        return post;
    }

    private static AdaptiveHookResolver.Candidate evaluate(Method method, String source) {
        if (!RenderBoundaryShape.accepts(method)) {
            return null;
        }
        return new AdaptiveHookResolver.Candidate(method, RenderBoundaryShape.score(method), source);
    }
}
