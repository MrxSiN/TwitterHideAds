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
 * Discovery of adaptive render-boundary candidates, through DexKit first and a
 * bounded {@link DexFile} scan when DexKit is unavailable.
 */
final class BoundaryCandidateSource {
    private static final int MAX_REFLECTION_CLASSES = 1500;

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

    static List<AdaptiveHookResolver.Candidate> findWithDexFile(
            String apkPath,
            ClassLoader classLoader
    ) throws Exception {
        ArrayList<AdaptiveHookResolver.Candidate> candidates = new ArrayList<>();
        int scanned = 0;
        DexFile dexFile = new DexFile(apkPath);
        try {
            Enumeration<String> entries = dexFile.entries();
            while (entries.hasMoreElements() && scanned < MAX_REFLECTION_CLASSES) {
                String className = entries.nextElement();
                if (!className.startsWith(RenderBoundaryShape.POST_PACKAGE)) {
                    continue;
                }
                scanned++;
                Class<?> type;
                try {
                    type = Class.forName(className, false, classLoader);
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
        } finally {
            dexFile.close();
        }
        AdaptiveHookResolver.log("DexFile structural fallback: scannedPostClasses=" + scanned
                + ", eligible=" + candidates.size());
        return candidates;
    }

    private static AdaptiveHookResolver.Candidate evaluate(Method method, String source) {
        int score = RenderBoundaryShape.score(method);
        if (score == RenderBoundaryShape.NOT_A_BOUNDARY
                || score < AdaptiveHookResolver.ACTIVE_THRESHOLD) {
            return null;
        }
        return new AdaptiveHookResolver.Candidate(method, score, source);
    }
}
