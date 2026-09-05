package my.MrxSiN.twitterhideads;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Recognises a Compose render boundary by structure instead of by name.
 *
 * The Kotlin Compose compiler rewrites every composable into a static method
 * that carries the declared parameters, a {@code Composer}, a {@code $changed}
 * mask and, when the source declares default arguments, a {@code $default}
 * mask:
 *
 *   static void render(Model, Modifier, LayoutScope, Composer, int)
 *   static void render(Model, Modifier, LayoutScope, Composer, int, int)
 *
 * Only the trailing shape is fixed by the compiler. Everything before the
 * {@code Composer} is source-defined and therefore moves between X releases,
 * so parameters are matched by role rather than by position. X 12.7.1 through
 * 12.9.1 passed a post dependency alongside the model; 12.22.0 dropped it.
 * Both remain acceptable here.
 *
 * A boundary is identified by the post model it renders, not by where it is
 * declared. X 12.22.0 renders the home timeline from
 * {@code com.x.jetfuel.v2.element.attribute}, so requiring the declaring class
 * to sit in the post package hid the only boundary that matters.
 */
final class RenderBoundaryShape {
    /** Package holding the post render boundaries, without a trailing dot. */
    static final String POST_PACKAGE_ROOT = "com.x.urt.items.post";

    static final String POST_PACKAGE = POST_PACKAGE_ROOT + ".";

    private static final String COMPOSER = "androidx.compose.runtime.Composer";
    private static final String MODIFIER_PREFIX = "androidx.compose.ui.Modifier";
    private static final String LAYOUT_PREFIX = "androidx.compose.foundation.layout.";


    private static final int SCORE_STATIC = 25;
    private static final int SCORE_VOID = 25;
    private static final int SCORE_DIRECT_PACKAGE = 80;
    private static final int SCORE_NESTED_PACKAGE = 25;
    private static final int SCORE_MODEL_PACKAGE = 80;
    private static final int SCORE_COMPOSER = 60;
    private static final int SCORE_MODIFIER = 35;
    private static final int SCORE_LAYOUT_SCOPE = 35;
    private static final int SCORE_POST_DEPENDENCY = 20;
    private static final int SCORE_TRAILING_MASKS = 25;
    private static final int SCORE_SHORT_NAME = 10;
    private static final int SCORE_MODEL_INTERFACE = 15;

    private RenderBoundaryShape() {
    }

    /**
     * Scores {@code method} as a render boundary, or returns
     * {@link #NOT_A_BOUNDARY} when its structure rules it out.
     */
    static int score(Method method) {
        if (!isStructurallyEligible(method)) {
            return NOT_A_BOUNDARY;
        }

        Class<?>[] parameters = method.getParameterTypes();
        String declaring = method.getDeclaringClass().getName();
        Class<?> model = parameters[0];

        int score = SCORE_STATIC
                + SCORE_VOID
                + SCORE_COMPOSER
                + SCORE_TRAILING_MASKS
                + SCORE_MODEL_PACKAGE;
        score += isDirectlyInPostPackage(declaring)
                ? SCORE_DIRECT_PACKAGE
                : SCORE_NESTED_PACKAGE;
        score += model.isInterface() ? SCORE_MODEL_INTERFACE : 0;
        score += method.getName().length() <= 2 ? SCORE_SHORT_NAME : 0;

        int composerIndex = composerIndex(parameters);
        boolean modifier = false;
        boolean layoutScope = false;
        boolean postDependency = false;
        for (int index = 1; index < composerIndex; index++) {
            String name = parameters[index].getName();
            modifier |= name.startsWith(MODIFIER_PREFIX);
            layoutScope |= name.startsWith(LAYOUT_PREFIX);
            postDependency |= name.startsWith(POST_PACKAGE);
        }
        score += modifier ? SCORE_MODIFIER : 0;
        score += layoutScope ? SCORE_LAYOUT_SCOPE : 0;
        score += postDependency ? SCORE_POST_DEPENDENCY : 0;

        return score;
    }

    /** Sentinel returned by {@link #score(Method)} for a non-boundary. */
    static final int NOT_A_BOUNDARY = -1;

    private static boolean isStructurallyEligible(Method method) {
        int modifiers = method.getModifiers();
        if (!Modifier.isStatic(modifiers)
                || Modifier.isAbstract(modifiers)
                || Modifier.isNative(modifiers)
                || method.isSynthetic()
                || method.getReturnType() != Void.TYPE) {
            return false;
        }
        Class<?>[] parameters = method.getParameterTypes();
        int composerIndex = composerIndex(parameters);
        if (composerIndex < 1) {
            return false;
        }
        if (!parameters[0].getName().startsWith(POST_PACKAGE)) {
            return false;
        }

        // Exactly the compiler-generated $changed mask, optionally followed by
        // the $default mask. Anything else is not a composable entry point.
        int trailing = parameters.length - composerIndex - 1;
        if (trailing < 1 || trailing > 2) {
            return false;
        }
        for (int index = composerIndex + 1; index < parameters.length; index++) {
            if (parameters[index] != Integer.TYPE) {
                return false;
            }
        }
        return true;
    }

    private static int composerIndex(Class<?>[] parameters) {
        for (int index = 0; index < parameters.length; index++) {
            if (COMPOSER.equals(parameters[index].getName())) {
                return index;
            }
        }
        return -1;
    }

    private static boolean isDirectlyInPostPackage(String className) {
        int lastDot = className.lastIndexOf('.');
        return lastDot >= 0
                && POST_PACKAGE_ROOT.equals(className.substring(0, lastDot));
    }
}
