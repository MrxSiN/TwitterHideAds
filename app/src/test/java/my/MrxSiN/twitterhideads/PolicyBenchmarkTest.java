package my.MrxSiN.twitterhideads;

import com.x.models.PostActionType;
import com.x.models.timelines.items.VideoItem;
import com.x.urt.items.post.Boundaries;
import com.x.urt.items.post.TimelinePost;

import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.IntSupplier;

import my.MrxSiN.twitterhideads.legacy.LegacyBundledAdPatterns;
import my.MrxSiN.twitterhideads.legacy.LegacyRenderBoundaryShape;
import my.MrxSiN.twitterhideads.legacy.LegacyVideoDatasetClassifier;

/**
 * Host microbenchmarks, legacy Java policy against the Brainfuck policy
 * through JNI. Reports p50/p95/p99/max per call and allocated bytes per call.
 * Host numbers only indicate relative cost; device numbers come from the
 * debug build's policy timing (docs/BRAINFUCK_ARCHITECTURE.md).
 */
public class PolicyBenchmarkTest {
    private static final int WARMUP = 20000;
    private static final int SAMPLES = 20000;

    @BeforeClass
    public static void load() {
        HostCore.ensure();
    }

    /** com.sun.management.ThreadMXBean#getThreadAllocatedBytes, reached reflectively (android.jar lacks it). */
    private static long allocatedBytes() {
        try {
            Object bean = Class.forName("java.lang.management.ManagementFactory")
                    .getMethod("getThreadMXBean").invoke(null);
            Method method = Class.forName("com.sun.management.ThreadMXBean")
                    .getMethod("getThreadAllocatedBytes", long.class);
            return (Long) method.invoke(bean, Thread.currentThread().getId());
        } catch (Throwable throwable) {
            return 0;
        }
    }

    private static volatile int sink;

    private static String measure(String name, IntSupplier call) {
        for (int index = 0; index < WARMUP; index++) {
            sink += call.getAsInt();
        }
        long[] nanos = new long[SAMPLES];
        long allocatedBefore = allocatedBytes();
        for (int index = 0; index < SAMPLES; index++) {
            long started = System.nanoTime();
            sink += call.getAsInt();
            nanos[index] = System.nanoTime() - started;
        }
        long allocated = allocatedBytes() - allocatedBefore;
        Arrays.sort(nanos);
        return String.format(Locale.ROOT, "%-34s p50=%6d p95=%6d p99=%6d max=%8d ns  alloc=%6.1f B/call",
                name, nanos[SAMPLES / 2], nanos[SAMPLES * 95 / 100], nanos[SAMPLES * 99 / 100],
                nanos[SAMPLES - 1], (double) allocated / SAMPLES);
    }

    /** Cold: first calls on fresh model classes and caches. */
    private static String cold(String name, IntSupplier call) {
        long started = System.nanoTime();
        sink += call.getAsInt();
        return String.format(Locale.ROOT, "%-34s first call %d ns", name, System.nanoTime() - started);
    }

    @Test
    public void benchmark() throws Exception {
        TimelinePost organic = new TimelinePost("tweet-2102692236387627106")
                .text("Just a normal post about my day, nothing to see here at all, promise.");
        TimelinePost promoted = new TimelinePost("promoted-tweet-2102692433305718851-6707f8b5db57b19e");
        TimelinePost nested = new TimelinePost(
                "conversationthread-2102634587474268204-promoted-tweet-2094154228436742320-6707349df11a283f");
        List<TimelinePost> fallbackModels = new ArrayList<>();
        for (int index = 0; index < 64; index++) {
            fallbackModels.add(new TimelinePost(null).actions(PostActionType.Reply, PostActionType.Like,
                    index % 2 == 0 ? PostActionType.PromotedDismissAd : PostActionType.Reply));
        }
        Method boundary = Boundaries.class.getDeclaredMethod("e", com.x.urt.items.post.PostModel.class,
                androidx.compose.ui.Modifier.class, androidx.compose.foundation.layout.ColumnScope.class,
                com.x.urt.items.post.Dependency.class, androidx.compose.runtime.Composer.class, int.class);
        List<Object> batch = new ArrayList<>();
        for (int index = 0; index < 20; index++) {
            batch.add(new VideoItem((index % 7 == 3 ? "promoted-tweet-" : "tweet-") + (1000 + index), null));
        }

        List<String> lines = new ArrayList<>();
        lines.add(cold("new organic post (cold)", () -> BundledAdPatterns.classifyPacked(organic, null, true)));
        lines.add(cold("legacy organic post (cold)",
                () -> LegacyBundledAdPatterns.classifyTimelinePost(organic, null, true).verdict.ordinal()));
        lines.add(measure("legacy organic post", () ->
                LegacyBundledAdPatterns.classifyTimelinePost(organic, null, true).verdict.ordinal()));
        lines.add(measure("new organic post (hot path)", () ->
                BundledAdPatterns.classifyPacked(organic, null, true)));
        lines.add(measure("legacy promoted post", () ->
                LegacyBundledAdPatterns.classifyTimelinePost(promoted, null, true).verdict.ordinal()));
        lines.add(measure("new promoted post (hot path)", () ->
                BundledAdPatterns.classifyPacked(promoted, null, true)));
        lines.add(measure("legacy nested promoted post", () ->
                LegacyBundledAdPatterns.classifyTimelinePost(nested, null, true).verdict.ordinal()));
        lines.add(measure("new nested promoted post", () ->
                BundledAdPatterns.classifyPacked(nested, null, true)));
        int[] cursor = {0};
        lines.add(measure("legacy fallback (cached)", () -> LegacyBundledAdPatterns.classifyTimelinePost(
                fallbackModels.get(cursor[0]++ & 63), null, true).verdict.ordinal()));
        lines.add(measure("new fallback (cached)", () -> BundledAdPatterns.classifyPacked(
                fallbackModels.get(cursor[0]++ & 63), null, true)));
        lines.add(measure("legacy fallback walk (uncached)", () -> my.MrxSiN.twitterhideads.legacy
                .LegacyPromotedActionScanner.inspect(fallbackModels.get(cursor[0]++ & 63)).actions.size()));
        lines.add(measure("new fallback walk (uncached)", () ->
                PromotedActionScanner.inspect(fallbackModels.get(cursor[0]++ & 63)).actions.size()));
        lines.add(measure("legacy boundary score", () -> LegacyRenderBoundaryShape.score(boundary)));
        lines.add(measure("new boundary score", () -> RenderBoundaryShape.score(boundary)));
        lines.add(measure("legacy video batch (20 items)", () ->
                LegacyVideoDatasetClassifier.inspect(batch).promotedCount));
        lines.add(measure("new video batch (20 items)", () ->
                VideoDatasetClassifier.inspect(batch).promotedCount));
        lines.add(measure("JNI round trip (OP_LIMITS frame)", () -> {
            PolicyFrame frame = PolicyFrame.begin(BfAbi.OP_LIMITS);
            try {
                return frame.send(BfAbi.PROG_DISCOVERY, BfAbi.LIMITS_SIZE) ? frame.out(0) : -1;
            } finally {
                frame.release();
            }
        }));
        System.out.println("PolicyBenchmarkTest (host JVM, " + System.getProperty("os.arch") + ")");
        for (String line : lines) {
            System.out.println("  " + line);
        }
    }
}
