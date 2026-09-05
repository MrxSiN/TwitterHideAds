package my.MrxSiN.twitterhideads;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Reflection helpers that replace the legacy {@code XposedHelpers} utilities.
 * The modern Xposed API deliberately ships no helper layer.
 */
final class Reflect {
    private Reflect() {
    }

    /** Loads a class without initializing it, or returns {@code null}. */
    static Class<?> findClassIfExists(String className, ClassLoader classLoader) {
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
    static Object read(Field field, Object owner) {
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
    static Field[] allFields(Class<?> type) {
        ArrayList<Field> fields = new ArrayList<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            Collections.addAll(fields, current.getDeclaredFields());
            current = current.getSuperclass();
        }
        return fields.toArray(new Field[0]);
    }

    static String describeThrowable(Throwable throwable) {
        String message = throwable.getMessage();
        return throwable.getClass().getSimpleName()
                + (message == null ? "" : ": " + message);
    }
}
