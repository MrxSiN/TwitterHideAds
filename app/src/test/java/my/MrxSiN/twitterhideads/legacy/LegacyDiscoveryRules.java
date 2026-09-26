package my.MrxSiN.twitterhideads.legacy;

// Frozen copy of the v2.1.0 Java policy: the behavioural oracle for the
// Brainfuck parity tests. Never edit it to match new behaviour.

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Pure decisions of v2.1.0 that lived inside classes with Android or
 * framework dependencies, copied verbatim: VideoDatasetResolver.evaluate and
 * its selection thresholds, CompatibilityProfile.selectExact,
 * TwitterAdBlocker.isExactRenderBoundary and the VideoDatasetFilter checks.
 */
public final class LegacyDiscoveryRules {
    public static final String URT_PREFIX = "com.x.urt.";
    public static final int VIDEO_ACTIVE_THRESHOLD = 330;
    public static final int VIDEO_MIN_MARGIN = 25;
    public static final int RENDER_ACTIVE_THRESHOLD = 320;

    private LegacyDiscoveryRules() {
    }

    /** VideoDatasetResolver.evaluate; -1 when the method is not a candidate. */
    public static int videoScore(Method method) {
        int modifiers = method.getModifiers();
        Class<?>[] parameters = method.getParameterTypes();
        Class<?> returnType = method.getReturnType();
        if (!Modifier.isStatic(modifiers)
                || Modifier.isAbstract(modifiers)
                || Modifier.isNative(modifiers)
                || method.isSynthetic()
                || method.isBridge()
                || returnType == Void.TYPE
                || returnType.isPrimitive()
                || parameters.length < 4
                || parameters.length > 8
                || parameters[0] != returnType
                || !isImmutableListLike(parameters[1])
                || !isDirectUrtClass(method.getDeclaringClass().getName())
                || !returnType.getName().startsWith(URT_PREFIX)) {
            return -1;
        }

        boolean hasBoolean = false;
        boolean hasInt = false;
        for (int index = 2; index < parameters.length; index++) {
            hasBoolean |= parameters[index] == Boolean.TYPE;
            hasInt |= parameters[index] == Integer.TYPE;
        }
        if (!hasBoolean || !hasInt) {
            return -1;
        }

        int score = 0;
        score += 110;
        score += 100;
        score += parameters[1].getName().startsWith("kotlinx.collections.immutable.") ? 95 : 70;
        score += hasBoolean ? 25 : 0;
        score += hasInt ? 25 : 0;
        score += parameters.length == 5 ? 30 : 10;
        score += method.getName().length() <= 2 ? 10 : 0;
        score += Modifier.isPublic(modifiers) ? 8 : 0;
        return score;
    }

    /** VideoDatasetResolver.select on the sorted scores: 0 accept, 1 none, 2 below, 3 ambiguous. */
    public static int videoSelect(int count, int best, int second) {
        if (count == 0) {
            return 1;
        }
        if (best < VIDEO_ACTIVE_THRESHOLD) {
            return 2;
        }
        if (second >= 0 && best - second < VIDEO_MIN_MARGIN) {
            return 3;
        }
        return 0;
    }

    public static boolean isImmutableListLike(Class<?> type) {
        return Iterable.class.isAssignableFrom(type)
                || type.getName().startsWith("kotlinx.collections.immutable.");
    }

    public static boolean isDirectUrtClass(String className) {
        if (!className.startsWith(URT_PREFIX)) {
            return false;
        }
        String remainder = className.substring(URT_PREFIX.length());
        int dollar = remainder.indexOf('$');
        if (dollar >= 0) {
            remainder = remainder.substring(0, dollar);
        }
        return remainder.indexOf('.') < 0;
    }

    /** CompatibilityProfile.selectExact: "x-12.8.0", "x-12.7.1" or null. */
    public static String selectExact(String versionName) {
        if (versionName == null) {
            return null;
        }
        if (matchesPrefix(versionName, "12.8.0")) {
            return "x-12.8.0";
        }
        if (matchesPrefix(versionName, "12.7.1")) {
            return "x-12.7.1";
        }
        return null;
    }

    private static boolean matchesPrefix(String rawName, String prefix) {
        String name = rawName.trim();
        return name.equals(prefix)
                || name.startsWith(prefix + "-")
                || name.startsWith(prefix + ".")
                || name.startsWith(prefix + "+")
                || name.startsWith(prefix + " ");
    }

    /** TwitterAdBlocker.isExactRenderBoundary with postArgument precomputed. */
    public static boolean isExactRenderBoundary(Method method, boolean postArgument, String expectedName) {
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

    /** VideoDatasetFilter: proceed to build a copy. */
    public static boolean proceedToCopy(int containerSize, int filteredSize, int normalCount) {
        int removed = containerSize - filteredSize;
        return !(removed <= 0 || filteredSize < normalCount);
    }

    /** VideoDatasetFilter: accept the verified copy. */
    public static boolean acceptVerified(int filteredSize, int normalCount,
                                         int verifiedPromoted, int verifiedNormal, int verifiedSize) {
        return !(verifiedPromoted != 0
                || verifiedNormal < normalCount
                || verifiedSize != filteredSize);
    }
}
