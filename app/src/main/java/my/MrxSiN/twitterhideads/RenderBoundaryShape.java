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
 * Only the trailing shape is fixed by the compiler; parameters before the
 * {@code Composer} are matched by role. This class reduces a method to those
 * facts (modifiers, declaring package, one role per parameter). Eligibility,
 * the score and the acceptance threshold are decided by
 * {@code brainfuck/src/discovery.bf} (OP_RENDER_BOUNDARY, docs/policy/discovery.md).
 *
 * A boundary is identified by the post model it renders, not by where it is
 * declared: X 12.22.0 renders the home timeline from
 * {@code com.x.jetfuel.v2.element.attribute}.
 */
final class RenderBoundaryShape {
    /** Package holding the post render boundaries, without a trailing dot. */
    static final String POST_PACKAGE_ROOT = "com.x.urt.items.post";

    static final String POST_PACKAGE = POST_PACKAGE_ROOT + ".";

    static final String COMPOSER = "androidx.compose.runtime.Composer";
    private static final String MODIFIER_PREFIX = "androidx.compose.ui.Modifier";
    private static final String LAYOUT_PREFIX = "androidx.compose.foundation.layout.";

    /** Sentinel returned by {@link #score(Method)} for a non-boundary. */
    static final int NOT_A_BOUNDARY = -1;

    private RenderBoundaryShape() {
    }

    /** The boundary score, or {@link #NOT_A_BOUNDARY}. */
    static int score(Method method) {
        int result = evaluate(method);
        return result < 0 ? NOT_A_BOUNDARY : result & 0xFFFF;
    }

    /** True for an eligible boundary whose score reaches the policy threshold. */
    static boolean accepts(Method method) {
        int result = evaluate(method);
        return result >= 0 && (result & 0x10000) != 0;
    }

    /** -1 when ineligible, else score | accepted << 16. */
    private static int evaluate(Method method) {
        Class<?>[] parameters = method.getParameterTypes();
        if (parameters.length > 255) {
            return -1;
        }
        int modifiers = method.getModifiers();
        PolicyFrame frame = PolicyFrame.begin(BfAbi.OP_RENDER_BOUNDARY);
        try {
            frame.bool(Modifier.isStatic(modifiers));
            frame.bool(Modifier.isAbstract(modifiers));
            frame.bool(Modifier.isNative(modifiers));
            frame.bool(method.isSynthetic());
            frame.bool(method.getReturnType() == Void.TYPE);
            frame.bool(isDirectlyInPostPackage(method.getDeclaringClass().getName()));
            frame.bool(parameters.length > 0 && parameters[0].isInterface());
            frame.bool(method.getName().length() <= 2);
            frame.u8(parameters.length);
            for (Class<?> parameter : parameters) {
                frame.u8(role(parameter));
            }
            if (!frame.send(BfAbi.PROG_DISCOVERY, 4) || frame.out(0) != 1) {
                return -1;
            }
            return frame.out16(1) | (frame.out(3) == 1 ? 0x10000 : 0);
        } finally {
            frame.release();
        }
    }

    static int role(Class<?> parameter) {
        if (parameter == Integer.TYPE) {
            return BfAbi.R_INT;
        }
        String name = parameter.getName();
        if (COMPOSER.equals(name)) {
            return BfAbi.R_COMPOSER;
        }
        if (name.startsWith(MODIFIER_PREFIX)) {
            return BfAbi.R_MODIFIER;
        }
        if (name.startsWith(LAYOUT_PREFIX)) {
            return BfAbi.R_LAYOUT;
        }
        if (name.startsWith(POST_PACKAGE)) {
            return BfAbi.R_POST;
        }
        return BfAbi.R_OTHER;
    }

    private static boolean isDirectlyInPostPackage(String className) {
        int lastDot = className.lastIndexOf('.');
        return lastDot >= 0
                && POST_PACKAGE_ROOT.equals(className.substring(0, lastDot));
    }
}
