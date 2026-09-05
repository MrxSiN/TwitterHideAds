package my.MrxSiN.twitterhideads;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.libxposed.api.XposedInterface;

/** Single-hook upstream Video Tab dataset filter. */
final class VideoDatasetFilter {
    private static final AtomicBoolean INSTALLED = new AtomicBoolean(false);
    private static final AtomicInteger FILTERED_BATCHES = new AtomicInteger();
    private static final AtomicInteger FILTER_ERRORS = new AtomicInteger();

    private VideoDatasetFilter() {
    }

    static void install(VideoDatasetResolver.Resolution resolution) {
        if (!INSTALLED.compareAndSet(false, true)) {
            return;
        }
        if (resolution == null || resolution.method == null) {
            log("Video dataset filter unavailable; fail-open mode active: reason="
                    + (resolution == null ? "missing-resolution" : resolution.reason));
            return;
        }

        final Method boundary = resolution.method;
        try {
            XposedInterface.HookHandle handle = ModuleRuntime.hook(boundary, chain -> {
                Object[] replacement = filteredArguments(boundary, chain);
                return replacement == null ? chain.proceed() : chain.proceed(replacement);
            });
            if (handle == null) {
                log("Video dataset filter hook failed; fail-open mode active: "
                        + "framework interface unavailable");
                return;
            }
            log("Video dataset filter initialized: boundary=" + boundary
                    + ", score=" + resolution.score
                    + ", source=" + resolution.source
                    + ", cacheHit=" + resolution.cacheHit
                    + ", installedHooks=1"
                    + ", composeHooks=0, autoplayHooks=0, playbackHooks=0, playerHooks=0");
        } catch (Throwable throwable) {
            log("Video dataset filter hook failed; fail-open mode active: "
                    + Reflect.describeThrowable(throwable));
        }
    }

    /**
     * Returns the argument array to proceed with when the promoted entries of
     * this batch can be removed safely, or {@code null} to proceed unchanged.
     */
    private static Object[] filteredArguments(
            Method boundary,
            XposedInterface.Chain chain
    ) {
        try {
            List<Object> args = chain.getArgs();
            if (args.size() < 2 || !calledFromVideoTab()) {
                return null;
            }
            Object original = args.get(1);
            VideoDatasetClassifier.Batch batch = VideoDatasetClassifier.inspect(original);
            if (!batch.safeMixedVideoBatch()) {
                return null;
            }

            List<Object> filteredElements = VideoDatasetClassifier.filteredElements(original);
            int removed = batch.containerSize - filteredElements.size();
            if (removed <= 0 || filteredElements.size() < batch.normalCount) {
                return null;
            }

            Class<?> declaredType = boundary.getParameterTypes()[1];
            Object replacement = createCompatibleCopy(original, declaredType, filteredElements);
            if (replacement == null || !declaredType.isInstance(replacement)) {
                int error = FILTER_ERRORS.incrementAndGet();
                if (error <= 5) {
                    log("Video dataset copy failed open: declaredType=" + declaredType.getName()
                            + ", runtimeType=" + original.getClass().getName()
                            + ", promoted=" + batch.promotedCount);
                }
                return null;
            }

            VideoDatasetClassifier.Batch verified = VideoDatasetClassifier.inspect(replacement);
            if (verified.promotedCount != 0
                    || verified.normalCount < batch.normalCount
                    || verified.containerSize != filteredElements.size()) {
                log("Video dataset replacement rejected by verification; fail-open mode active");
                return null;
            }

            Object[] replacementArgs = args.toArray();
            replacementArgs[1] = replacement;

            int number = FILTERED_BATCHES.incrementAndGet();
            log("Filtered promoted videos before pager creation: batch=" + number
                    + ", boundary=" + boundary.getDeclaringClass().getName()
                    + "." + boundary.getName()
                    + ", originalSize=" + batch.containerSize
                    + ", filteredSize=" + verified.containerSize
                    + ", removed=" + removed
                    + ", promotedIds=" + batch.promotedIds);
            return replacementArgs;
        } catch (Throwable throwable) {
            int error = FILTER_ERRORS.incrementAndGet();
            if (error <= 8) {
                log("Video dataset filtering failed open: "
                        + Reflect.describeThrowable(throwable));
            }
            return null;
        }
    }

    private static boolean calledFromVideoTab() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        int examined = 0;
        for (StackTraceElement element : stack) {
            if (++examined > 48) {
                break;
            }
            if (element.getClassName().startsWith("com.x.video.tab.")) {
                return true;
            }
        }
        return false;
    }

    private static Object createCompatibleCopy(
            Object original,
            Class<?> declaredType,
            List<Object> filtered
    ) {
        if (declaredType.isAssignableFrom(ArrayList.class)) {
            return new ArrayList<>(filtered);
        }

        Object built = buildThroughPersistentBuilder(original, declaredType, filtered);
        if (built != null) {
            return built;
        }

        if (declaredType.isInterface()) {
            return proxyList(declaredType, filtered);
        }
        return null;
    }

    private static Object buildThroughPersistentBuilder(
            Object original,
            Class<?> declaredType,
            List<Object> filtered
    ) {
        for (Method method : publicAndDeclaredMethods(original.getClass())) {
            if (method.getParameterTypes().length != 0
                    || !List.class.isAssignableFrom(method.getReturnType())) {
                continue;
            }
            Object builder;
            try {
                method.setAccessible(true);
                builder = method.invoke(original);
            } catch (Throwable ignored) {
                continue;
            }
            if (!(builder instanceof List<?>) || builder == original) {
                continue;
            }

            @SuppressWarnings("unchecked")
            List<Object> mutable = (List<Object>) builder;
            try {
                mutable.clear();
                mutable.addAll(filtered);
            } catch (Throwable ignored) {
                continue;
            }

            for (Method build : publicAndDeclaredMethods(builder.getClass())) {
                if (build.getParameterTypes().length != 0
                        || !declaredType.isAssignableFrom(build.getReturnType())) {
                    continue;
                }
                try {
                    build.setAccessible(true);
                    Object result = build.invoke(builder);
                    if (result != null && declaredType.isInstance(result)) {
                        return result;
                    }
                } catch (Throwable ignored) {
                    // try the next no-argument build-like method
                }
            }
        }
        return null;
    }

    private static List<Method> publicAndDeclaredMethods(Class<?> type) {
        LinkedHashSet<Method> methods = new LinkedHashSet<>();
        Collections.addAll(methods, type.getMethods());
        Class<?> current = type;
        while (current != null && current != Object.class) {
            Collections.addAll(methods, current.getDeclaredMethods());
            current = current.getSuperclass();
        }
        return new ArrayList<>(methods);
    }

    private static Object proxyList(Class<?> declaredType, List<Object> filtered) {
        final List<Object> delegate = Collections.unmodifiableList(new ArrayList<>(filtered));
        Set<Class<?>> interfaces = new LinkedHashSet<>();
        interfaces.add(declaredType);
        collectInterfaces(declaredType, interfaces);
        interfaces.add(List.class);
        return Proxy.newProxyInstance(
                declaredType.getClassLoader(),
                interfaces.toArray(new Class<?>[0]),
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        String name = method.getName();
                        Class<?>[] parameters = method.getParameterTypes();
                        if (method.getDeclaringClass() == Object.class) {
                            if ("toString".equals(name)) {
                                return delegate.toString();
                            }
                            if ("hashCode".equals(name)) {
                                return delegate.hashCode();
                            }
                            if ("equals".equals(name)) {
                                return proxy == (args == null ? null : args[0]);
                            }
                        }
                        if (parameters.length == 0 && method.getReturnType() == Integer.TYPE) {
                            return delegate.size();
                        }
                        if (parameters.length == 0 && method.getReturnType() == Boolean.TYPE) {
                            return delegate.isEmpty();
                        }
                        if (parameters.length == 0
                                && Iterator.class.isAssignableFrom(method.getReturnType())) {
                            return delegate.iterator();
                        }
                        if (parameters.length == 1
                                && parameters[0] == Integer.TYPE
                                && method.getReturnType() != Void.TYPE) {
                            return delegate.get((Integer) args[0]);
                        }
                        try {
                            Method listMethod = List.class.getMethod(name, parameters);
                            return listMethod.invoke(delegate, args);
                        } catch (NoSuchMethodException ignored) {
                            throw new UnsupportedOperationException("Unsupported immutable-list method: " + method);
                        }
                    }
                }
        );
    }

    private static void collectInterfaces(Class<?> type, Set<Class<?>> output) {
        for (Class<?> iface : type.getInterfaces()) {
            if (output.add(iface)) {
                collectInterfaces(iface, output);
            }
        }
    }

    private static void log(String message) {
        ModuleRuntime.log(message);
    }
}
