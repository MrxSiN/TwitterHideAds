package my.MrxSiN.twitterhideads;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Builds a filtered copy of a kotlinx.collections.immutable persistent list
 * through the library's own builder API:
 *
 *   fun builder(): PersistentList.Builder<E>     // a MutableList
 *   fun Builder<E>.build(): PersistentList<E>
 *
 * R8 renames these members, strips them from the interfaces and keeps them
 * on the implementation classes (X 12.28.0: {@code immutableList.a#c()}
 * returns the concrete builder {@code immutableList.d}). The pair is therefore
 * resolved by shape on the runtime type's kotlinx.collections.immutable
 * classes, once per runtime class:
 *
 *   builder: no-arg instance method returning a kotlinx mutable
 *            {@link List} that is not itself a persistent list;
 *   build:   no-arg instance method on that builder returning a persistent
 *            list.
 *
 * A runtime type without the shape yields {@code null} and the caller fails
 * open; no other list implementation is emulated.
 */
final class PersistentListCopier {
    static final String NAMESPACE = "kotlinx.collections.immutable.";

    private static final Api UNSUPPORTED = new Api(null, null);
    private static final ConcurrentHashMap<Class<?>, Api> APIS = new ConcurrentHashMap<>();

    private PersistentListCopier() {
    }

    /**
     * Returns a {@code declaredType} instance holding {@code elements}, or
     * {@code null} when the runtime type does not expose the builder API.
     */
    static Object copy(Object original, Class<?> declaredType, List<Object> elements) {
        if (original == null || declaredType == null) {
            return null;
        }
        Api api = APIS.computeIfAbsent(original.getClass(), PersistentListCopier::resolve);
        if (api == UNSUPPORTED) {
            return null;
        }
        try {
            @SuppressWarnings("unchecked")
            List<Object> builder = (List<Object>) api.builder.invoke(original);
            builder.clear();
            builder.addAll(elements);
            Object built = api.build.invoke(builder);
            return declaredType.isInstance(built) ? built : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Api resolve(Class<?> runtimeType) {
        Set<Class<?>> persistentTypes = namespaceTypes(runtimeType);
        for (Class<?> owner : persistentTypes) {
            for (Method builder : owner.getDeclaredMethods()) {
                Method build = buildMethod(builder, persistentTypes);
                if (build != null) {
                    try {
                        builder.setAccessible(true);
                        build.setAccessible(true);
                    } catch (Throwable ignored) {
                        return UNSUPPORTED;
                    }
                    return new Api(builder, build);
                }
            }
        }
        return UNSUPPORTED;
    }

    /** The build method when {@code builder} has the builder() shape. */
    private static Method buildMethod(Method builder, Set<Class<?>> persistentTypes) {
        Class<?> builderType = builder.getReturnType();
        if (!isInstanceNoArg(builder)
                || !builderType.getName().startsWith(NAMESPACE)
                || !List.class.isAssignableFrom(builderType)
                || isPersistent(builderType, persistentTypes)) {
            return null;
        }
        for (Class<?> type : namespaceTypes(builderType)) {
            for (Method build : type.getDeclaredMethods()) {
                if (isInstanceNoArg(build)
                        && isPersistent(build.getReturnType(), persistentTypes)) {
                    return build;
                }
            }
        }
        return null;
    }

    private static boolean isInstanceNoArg(Method method) {
        return !Modifier.isStatic(method.getModifiers())
                && !method.isSynthetic()
                && method.getParameterTypes().length == 0;
    }

    /** True when {@code type} is, or implements, one of the persistent list's own types. */
    private static boolean isPersistent(Class<?> type, Set<Class<?>> persistentTypes) {
        for (Class<?> persistent : persistentTypes) {
            if (persistent.isInterface() && persistent.isAssignableFrom(type)) {
                return true;
            }
        }
        return false;
    }

    /** Superclasses and interfaces of {@code type} inside {@link #NAMESPACE}. */
    private static Set<Class<?>> namespaceTypes(Class<?> type) {
        Set<Class<?>> all = new LinkedHashSet<>();
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            all.add(current);
            collectInterfaces(current, all);
        }
        Set<Class<?>> inNamespace = new LinkedHashSet<>();
        for (Class<?> candidate : all) {
            if (candidate.getName().startsWith(NAMESPACE)) {
                inNamespace.add(candidate);
            }
        }
        return inNamespace;
    }

    private static void collectInterfaces(Class<?> type, Set<Class<?>> output) {
        for (Class<?> iface : type.getInterfaces()) {
            if (output.add(iface)) {
                collectInterfaces(iface, output);
            }
        }
    }

    private static final class Api {
        final Method builder;
        final Method build;

        Api(Method builder, Method build) {
            this.builder = builder;
            this.build = build;
        }
    }
}
