package my.MrxSiN.twitterhideads;

import android.content.Context;
import android.content.SharedPreferences;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.enums.StringMatchType;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.UsingFieldData;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Real-time-bidding ads (Google SSP), added to timelines in X 12.37. They are
 * timeline items of their own ({@code UrtTimelineRtbImageAd}, obfuscated to
 * {@code com.x.models.timelines.items.r1} on X 12.37.2), not posts, so no post
 * render boundary sees them. Two hooks keep them off every surface:
 *
 * <ul>
 * <li>The paging state ({@code com.x.urt.b2}, the class declaring the Video
 * Tab copy method {@link VideoDatasetResolver} resolves) drops RTB items, and
 * modules wrapping only RTB items, from its list in its constructor. The Video
 * Tab pager and conversations read that list. ART inlines the small
 * constructor, so its callers are deoptimized.</li>
 * <li>Home loads its items from the timeline database without that state. Its
 * item-to-presenter mapper ({@code com.x.urt.b.a}) already maps an RTB item to
 * X's empty presenter ({@code com.x.urt.b0.a}) when Google SSP is unavailable;
 * the hook returns that presenter for every RTB item, so nothing is drawn.</li>
 * </ul>
 *
 * The mapper and the empty presenter are found by structure through DexKit
 * once per X build and cached.
 */
final class RtbAdFilter {
    private static final String ITEM_PACKAGE = "com.x.models.timelines.items.";
    private static final String SSP_PACKAGE = "com.x.googlessp";
    /** Kotlin data-class toString keeps the pre-obfuscation class name. */
    private static final String RTB_PREFIX = "UrtTimelineRtb";
    private static final String MODULE_PREFIX = "UrtTimelineModule(";
    private static final String PREFS = "twitterhideads_rtb_cache";
    private static final String CALLER = "caller:";
    private static final String MAPPER = "mapper:";
    private static final String EMPTY = "empty:";
    private static final String FORMAT = "format:2";

    private static final ConcurrentHashMap<Class<?>, Boolean> RTB_TYPES = new ConcurrentHashMap<>();
    /** Module class to its list fields; empty means not a module. */
    private static final ConcurrentHashMap<Class<?>, Field[]> MODULE_LISTS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, Field[]> OBJECT_FIELDS = new ConcurrentHashMap<>();
    private static final Field[] NONE = new Field[0];
    private static final AtomicInteger REMOVED = new AtomicInteger();
    private static final AtomicInteger MAPPED = new AtomicInteger();

    private RtbAdFilter() {
    }

    static void install(
            Method copyBoundary,
            Context context,
            CompatibilityProfile.DetectedVersion version
    ) {
        if (copyBoundary == null || copyBoundary.getParameterTypes().length < 2) {
            log("RTB ad filter unavailable: no paging state boundary");
            return;
        }
        long started = System.currentTimeMillis();
        Class<?> state = copyBoundary.getDeclaringClass();
        Class<?> listType = copyBoundary.getParameterTypes()[1];
        ArrayList<Executable> inlinable = new ArrayList<>();
        for (Constructor<?> constructor : state.getDeclaredConstructors()) {
            Class<?>[] parameters = constructor.getParameterTypes();
            for (int index = 0; index < parameters.length; index++) {
                if (parameters[index] == listType) {
                    if (hookList(constructor, index, listType)) {
                        inlinable.add(constructor);
                    }
                    break;
                }
            }
        }
        int stateHooks = inlinable.size();
        // The copy method calls the constructor and is inlined in turn.
        inlinable.add(copyBoundary);

        Targets targets = targets(context, version, state.getClassLoader(), inlinable);
        boolean mapperHooked = targets.mapper != null && targets.empty != null
                && hookMapper(targets.mapper, targets.empty);
        int deoptimized = 0;
        for (Method caller : targets.callers) {
            if (ModuleRuntime.deoptimize(caller)) {
                deoptimized++;
            }
        }
        log("RTB ad filter initialized: state=" + state.getName()
                + ", stateHooks=" + stateHooks
                + ", mapper=" + (mapperHooked ? targets.mapper : "none")
                + ", callers=" + targets.callers.size()
                + ", deoptimized=" + deoptimized
                + ", cacheHit=" + targets.cacheHit
                + ", elapsedMs=" + (System.currentTimeMillis() - started));
    }

    static final class Targets {
        final ArrayList<Method> callers = new ArrayList<>();
        Method mapper;
        Object empty;
        boolean cacheHit;
    }

    /** Callers to deoptimize, the Home mapper and its empty presenter; DexKit once per X build. */
    private static Targets targets(
            Context context,
            CompatibilityProfile.DetectedVersion version,
            ClassLoader classLoader,
            List<Executable> inlinable
    ) {
        Targets targets = new Targets();
        String apkPath = context == null ? null : AdaptiveBoundaryCache.sourceDir(context);
        if (apkPath == null) {
            return targets;
        }
        String fingerprint = AdaptiveBoundaryCache.fingerprint(apkPath, version);
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Set<String> cached = prefs.getStringSet(fingerprint, null);
        // No callers means the lookup failed: the constructor always has some.
        if (cached != null && cached.contains(FORMAT)) {
            targets.cacheHit = true;
            for (String entry : cached) {
                try {
                    if (entry.startsWith(CALLER)) {
                        Method caller = AdaptiveBoundaryCache.resolveSignature(
                                entry.substring(CALLER.length()), classLoader);
                        if (caller != null) {
                            targets.callers.add(caller);
                        }
                    } else if (entry.startsWith(MAPPER)) {
                        targets.mapper = AdaptiveBoundaryCache.resolveSignature(
                                entry.substring(MAPPER.length()), classLoader);
                    } else if (entry.startsWith(EMPTY)) {
                        targets.empty = staticValue(entry.substring(EMPTY.length()), classLoader);
                    }
                } catch (Throwable ignored) {
                    // an entry that no longer resolves is skipped
                }
            }
            return targets;
        }

        LinkedHashSet<String> entries = new LinkedHashSet<>();
        entries.add(FORMAT);
        String empty = findMapper(apkPath, classLoader, targets);
        if (targets.mapper != null && empty != null) {
            targets.empty = staticValue(empty, classLoader);
            entries.add(MAPPER + AdaptiveBoundaryCache.signature(targets.mapper));
            entries.add(EMPTY + empty);
            inlinable.add(targets.mapper);
        }
        targets.callers.addAll(InlinedCallerSource.find(apkPath, classLoader, inlinable));
        for (Method caller : targets.callers) {
            entries.add(CALLER + AdaptiveBoundaryCache.signature(caller));
        }
        if (!targets.callers.isEmpty()) {
            prefs.edit().clear().putStringSet(fingerprint, entries).apply();
        }
        return targets;
    }

    /**
     * RTB item classes are the timeline items whose toString starts with
     * {@link #RTB_PREFIX}; their presenter factories are googlessp methods
     * taking one; the mapper is the single caller of those factories outside
     * googlessp; the empty presenter is the mapper's only self-typed static
     * field read that is a presenter. Sets {@code targets.mapper} and returns
     * "class#field" of the empty presenter, or {@code null} when unsure.
     */
    private static String findMapper(String apkPath, ClassLoader classLoader, Targets targets) {
        try {
            System.loadLibrary("dexkit");
        } catch (Throwable ignored) {
            // DexKitBridge.create reports it below
        }
        try (DexKitBridge bridge = DexKitBridge.create(apkPath)) {
            Set<String> rtbTypes = new LinkedHashSet<>();
            for (MethodData data : bridge.findMethod(FindMethod.create()
                    .searchPackages(ITEM_PACKAGE.substring(0, ITEM_PACKAGE.length() - 1))
                    .matcher(MethodMatcher.create().name("toString")
                            .usingStrings(Collections.singletonList(RTB_PREFIX),
                                    StringMatchType.StartsWith, false)))) {
                rtbTypes.add(data.getDeclaredClassName());
            }
            Set<String> seen = new LinkedHashSet<>();
            String found = null;
            for (MethodData factory : bridge.findMethod(FindMethod.create()
                    .searchPackages(SSP_PACKAGE).matcher(MethodMatcher.create()))) {
                if (!takesAny(factory, rtbTypes)) {
                    continue;
                }
                for (MethodData caller : factory.getCallers()) {
                    if (caller.getDeclaredClassName().startsWith(SSP_PACKAGE)
                            || !seen.add(caller.getDescriptor())) {
                        continue;
                    }
                    Method method = caller.getMethodInstance(classLoader);
                    String empty = emptyPresenter(caller, method, classLoader);
                    if (empty == null) {
                        continue;
                    }
                    if (targets.mapper != null) {
                        log("RTB mapper ambiguous; Home filter skipped: " + targets.mapper + ", " + method);
                        targets.mapper = null;
                        return null;
                    }
                    targets.mapper = method;
                    found = empty;
                }
            }
            if (found == null) {
                log("RTB mapper not resolved: rtbTypes=" + rtbTypes + ", factoryCallers=" + seen.size());
            }
            return found;
        } catch (Throwable throwable) {
            log("RTB mapper lookup failed: " + Reflect.describeThrowable(throwable));
            return null;
        }
    }

    /** The one self-typed static presenter {@code caller} reads, or {@code null}. */
    private static String emptyPresenter(MethodData caller, Method method, ClassLoader classLoader)
            throws ClassNotFoundException {
        String empty = null;
        for (UsingFieldData using : caller.getUsingFields()) {
            String owner = using.getField().getDeclaredClassName();
            if (!Modifier.isStatic(using.getField().getModifiers())
                    || !owner.equals(using.getField().getTypeName())
                    || !method.getReturnType().isAssignableFrom(
                            Class.forName(owner, false, classLoader))) {
                continue;
            }
            String candidate = owner + "#" + using.getField().getFieldName();
            if (empty != null && !empty.equals(candidate)) {
                return null;
            }
            empty = candidate;
        }
        return empty;
    }

    private static boolean takesAny(MethodData method, Set<String> types) {
        for (String type : types) {
            if (method.getDescriptor().contains("L" + type.replace('.', '/') + ";")) {
                return true;
            }
        }
        return false;
    }

    private static Object staticValue(String entry, ClassLoader classLoader) {
        try {
            int hash = entry.indexOf('#');
            Field field = Class.forName(entry.substring(0, hash), false, classLoader)
                    .getDeclaredField(entry.substring(hash + 1));
            field.setAccessible(true);
            return field.get(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean hookList(Executable target, final int index, final Class<?> listType) {
        try {
            return ModuleRuntime.hook(target, chain -> {
                Object[] args = chain.getArgs().toArray();
                Object filtered = withoutRtbAds(args[index], listType);
                if (filtered == null) {
                    return chain.proceed();
                }
                args[index] = filtered;
                return chain.proceed(args);
            }) != null;
        } catch (Throwable throwable) {
            log("RTB ad filter hook failed: " + Reflect.describeThrowable(throwable));
            return false;
        }
    }

    private static boolean hookMapper(Method mapper, final Object empty) {
        if (!mapper.getReturnType().isInstance(empty)) {
            return false;
        }
        // The item is the one timeline-item parameter; the rest never need a look.
        int itemIndex = -1;
        Class<?>[] parameters = mapper.getParameterTypes();
        for (int index = 0; index < parameters.length; index++) {
            if (parameters[index].getName().startsWith(ITEM_PACKAGE)) {
                itemIndex = index;
                break;
            }
        }
        if (itemIndex < 0) {
            return false;
        }
        final int item = itemIndex;
        try {
            return ModuleRuntime.hook(mapper, chain -> {
                if (isRtbAd(chain.getArg(item))) {
                    int mapped = MAPPED.incrementAndGet();
                    if (mapped == 1 || mapped % 50 == 0) {
                        log("Mapped RTB ad to the empty presenter: mapped=" + mapped);
                    }
                    return empty;
                }
                return chain.proceed();
            }) != null;
        } catch (Throwable throwable) {
            log("RTB mapper hook failed: " + Reflect.describeThrowable(throwable));
            return false;
        }
    }

    /** A copy without RTB items, or {@code null} to keep the original. */
    static Object withoutRtbAds(Object list, Class<?> listType) {
        if (!(list instanceof List<?>)) {
            return null;
        }
        List<?> items = (List<?>) list;
        ArrayList<Object> kept = null;
        for (int index = 0; index < items.size(); index++) {
            Object item = items.get(index);
            if (isRtbAd(item) || isRtbOnlyModule(item)) {
                if (kept == null) {
                    kept = new ArrayList<>(items.subList(0, index));
                }
            } else if (kept != null) {
                kept.add(item);
            }
        }
        if (kept == null) {
            return null;
        }
        Object copy = listType.isAssignableFrom(ArrayList.class)
                ? kept
                : PersistentListCopier.copy(list, listType, kept);
        if (copy == null || !listType.isInstance(copy)) {
            return null; // fail open
        }
        // Every state copy re-filters the same ad, so the log is sampled.
        int calls = REMOVED.incrementAndGet();
        if (calls == 1 || calls % 50 == 0) {
            log("Removed RTB ads from timeline state: removed=" + (items.size() - kept.size())
                    + ", filteredStates=" + calls);
        }
        return copy;
    }

    /** Decided once per runtime class. */
    static boolean isRtbAd(Object item) {
        if (item == null) {
            return false;
        }
        Class<?> type = item.getClass();
        Boolean known = RTB_TYPES.get(type);
        if (known == null) {
            known = type.getName().startsWith(ITEM_PACKAGE)
                    && String.valueOf(item).startsWith(RTB_PREFIX);
            RTB_TYPES.put(type, known);
        }
        return known;
    }

    /**
     * Conversations wrap each RTB ad in its own module,
     * {@code UrtTimelineModule(innerContent=[UrtTimelineModuleItem(item=UrtTimelineRtbImageAd(..))])}.
     * A module goes only when every inner item wraps an RTB ad.
     */
    static boolean isRtbOnlyModule(Object item) {
        if (item == null) {
            return false;
        }
        Field[] lists = MODULE_LISTS.get(item.getClass());
        if (lists == null) {
            lists = item.getClass().getName().startsWith(ITEM_PACKAGE)
                    && String.valueOf(item).startsWith(MODULE_PREFIX)
                    ? fields(item.getClass(), true) : NONE;
            MODULE_LISTS.put(item.getClass(), lists);
        }
        for (Field field : lists) {
            Object value = read(field, item);
            if (!(value instanceof List<?>) || ((List<?>) value).isEmpty()) {
                continue;
            }
            for (Object inner : (List<?>) value) {
                if (!wrapsRtbAd(inner)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private static boolean wrapsRtbAd(Object moduleItem) {
        if (moduleItem == null) {
            return false;
        }
        Field[] fields = OBJECT_FIELDS.get(moduleItem.getClass());
        if (fields == null) {
            fields = fields(moduleItem.getClass(), false);
            OBJECT_FIELDS.put(moduleItem.getClass(), fields);
        }
        for (Field field : fields) {
            if (isRtbAd(read(field, moduleItem))) {
                return true;
            }
        }
        return false;
    }

    /** Instance reference fields, only {@link List} ones when {@code lists}. */
    private static Field[] fields(Class<?> type, boolean lists) {
        ArrayList<Field> out = new ArrayList<>();
        for (Field field : Reflect.allFields(type)) {
            Class<?> fieldType = field.getType();
            if (Modifier.isStatic(field.getModifiers()) || fieldType.isPrimitive()
                    || (lists && !List.class.isAssignableFrom(fieldType))) {
                continue;
            }
            try {
                field.setAccessible(true);
                out.add(field);
            } catch (Throwable ignored) {
                // unreadable field
            }
        }
        return out.toArray(NONE);
    }

    private static Object read(Field field, Object owner) {
        try {
            return field.get(owner);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void log(String message) {
        ModuleRuntime.log(message);
    }
}
