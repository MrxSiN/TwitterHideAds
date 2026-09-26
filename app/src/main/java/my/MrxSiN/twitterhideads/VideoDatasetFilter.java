package my.MrxSiN.twitterhideads;

import java.lang.reflect.Method;
import java.util.ArrayList;
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
            if (args.size() < 2) {
                return null;
            }
            // The batch check is bounded and field-cached; capturing a stack
            // is not, so the caller is only inspected for mixed batches that
            // would actually be rewritten.
            Object original = args.get(1);
            VideoDatasetClassifier.Batch batch = VideoDatasetClassifier.inspect(original);
            if (!batch.safeMixedVideoBatch() || !calledFromVideoTab()) {
                return null;
            }

            List<Object> filteredElements = VideoDatasetClassifier.filteredElements(original, batch);
            int removed = batch.containerSize - filteredElements.size();
            if (!proceedToCopy(batch.containerSize, filteredElements.size(), batch.normalCount)) {
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
            if (verified.containerSize < 0 || !acceptVerified(filteredElements.size(), batch.normalCount,
                    verified.promotedCount, verified.normalCount, verified.containerSize)) {
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
                    + ", promotedIds=" + redacted(batch.promotedIds));
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
        StackTraceElement[] stack = new Throwable().getStackTrace();
        int limit = PolicyLimits.stackFrames();
        int examined = 0;
        for (StackTraceElement element : stack) {
            if (++examined > limit) {
                break;
            }
            if (element.getClassName().startsWith("com.x.video.tab.")) {
                return true;
            }
        }
        return false;
    }

    /** OP_VIDEO_DECIDE, copy stage: something is removed and every organic item survives. */
    static boolean proceedToCopy(int containerSize, int filteredSize, int normalCount) {
        if (containerSize < 0 || containerSize > 0xFFFF || filteredSize > 0xFFFF) {
            return false;
        }
        PolicyFrame frame = PolicyFrame.begin(BfAbi.OP_VIDEO_DECIDE);
        try {
            frame.u8(BfAbi.VD_COPY);
            frame.u16(containerSize);
            frame.u16(filteredSize);
            frame.u8(Math.min(normalCount, 255));
            return frame.send(BfAbi.PROG_POST, 1) && frame.out(0) == 1;
        } finally {
            frame.release();
        }
    }

    /** OP_VIDEO_DECIDE, verify stage: the rebuilt list holds exactly the kept items. */
    static boolean acceptVerified(int filteredSize, int normalCount, int verifiedPromoted,
                                  int verifiedNormal, int verifiedSize) {
        if (filteredSize > 0xFFFF || verifiedSize > 0xFFFF) {
            return false;
        }
        PolicyFrame frame = PolicyFrame.begin(BfAbi.OP_VIDEO_DECIDE);
        try {
            frame.u8(BfAbi.VD_VERIFY);
            frame.u16(filteredSize);
            frame.u8(Math.min(normalCount, 255));
            frame.u8(Math.min(verifiedPromoted, 255));
            frame.u8(Math.min(verifiedNormal, 255));
            frame.u16(verifiedSize);
            return frame.send(BfAbi.PROG_POST, 1) && frame.out(0) == 1;
        } finally {
            frame.release();
        }
    }

    /**
     * An {@link ArrayList} when the declared type accepts one, otherwise a
     * copy through the persistent list's own builder. Anything else fails
     * open rather than emulating an immutable-collection interface.
     */
    private static Object createCompatibleCopy(
            Object original,
            Class<?> declaredType,
            List<Object> filtered
    ) {
        if (declaredType.isAssignableFrom(ArrayList.class)) {
            return new ArrayList<>(filtered);
        }
        return PersistentListCopier.copy(original, declaredType, filtered);
    }

    private static List<String> redacted(Set<String> entryIds) {
        ArrayList<String> out = new ArrayList<>(entryIds.size());
        for (String entryId : entryIds) {
            out.add(LogPrivacy.entryId(entryId));
        }
        return out;
    }

    private static void log(String message) {
        ModuleRuntime.log(message);
    }
}
