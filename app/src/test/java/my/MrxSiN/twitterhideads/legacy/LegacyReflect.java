package my.MrxSiN.twitterhideads.legacy;

// Frozen copy of the v2.1.0 Java policy: the behavioural oracle for the
// Brainfuck parity tests. Never edit it to match new behaviour.

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Reflection helpers that replace the legacy {@code XposedHelpers} utilities.
 * The modern Xposed API deliberately ships no helper layer.
 */
public final class LegacyReflect {
    private LegacyReflect() {
    }

    /** Loads a class without initializing it, or returns {@code null}. */
    public static Class<?> findClassIfExists(String className, ClassLoader classLoader) {
        if (className == null || classLoader == null) {
            return null;
        }
        try {
            return Class.forName(className, false, classLoader);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** Reads {@code field} from {@code owner}, or returns {@code null}. */
    public static Object read(Field field, Object owner) {
        if (field == null || owner == null) {
            return null;
        }
        try {
            field.setAccessible(true);
            return field.get(owner);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** All declared fields of {@code type} and its superclasses below Object. */
    public static Field[] allFields(Class<?> type) {
        ArrayList<Field> fields = new ArrayList<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            Collections.addAll(fields, current.getDeclaredFields());
            current = current.getSuperclass();
        }
        return fields.toArray(new Field[0]);
    }

    public static String describeThrowable(Throwable throwable) {
        String message = throwable.getMessage();
        return throwable.getClass().getSimpleName()
                + (message == null ? "" : ": " + message);
    }
}
