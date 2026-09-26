package my.MrxSiN.twitterhideads;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.x.models.ObfuscatedPromotedMetadata;
import com.x.urt.items.post.TimelinePost;
import com.x.urt.items.post.WidePost;

import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import my.MrxSiN.twitterhideads.legacy.LegacyEntryIds;

/** Malformed frames, fail-open paths, determinism, concurrency and invariants. */
public class PolicyRobustnessTest {
    @BeforeClass
    public static void load() {
        HostCore.ensure();
    }

    private static ByteBuffer direct(byte[] bytes, int capacity) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(Math.max(capacity, 1)).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(bytes);
        return buffer;
    }

    private static byte[] run(int program, byte[] request) {
        ByteBuffer out = ByteBuffer.allocateDirect(BfAbi.RUNTIME_OUT_CAP);
        int length = NativePolicy.nativeRun(program, direct(request, request.length), request.length, out);
        if (length < 0) {
            return new byte[]{(byte) length};
        }
        byte[] result = new byte[length];
        out.get(result);
        return result;
    }

    private static byte[] frame(int major, int op, byte[] payload) {
        byte[] out = new byte[BfAbi.HEADER_SIZE + payload.length];
        out[0] = (byte) major;
        out[2] = (byte) op;
        out[4] = (byte) payload.length;
        out[5] = (byte) (payload.length >> 8);
        out[6] = 0x34;
        out[7] = 0x12;
        System.arraycopy(payload, 0, out, BfAbi.HEADER_SIZE, payload.length);
        return out;
    }

    @Test
    public void randomBytesNeverCrashAndStayBounded() {
        Random random = new Random(0xBAD);
        int[] programs = {BfAbi.PROG_POST, BfAbi.PROG_ACTION, BfAbi.PROG_DISCOVERY};
        int[] ops = {0x10, 0x11, 0x12, 0x20, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35};
        int cases = HostCore.parityCases();
        for (int index = 0; index < cases; index++) {
            byte[] payload = new byte[random.nextInt(700)];
            random.nextBytes(payload);
            byte[] request = random.nextInt(5) == 0 ? payload
                    : frame(1, random.nextInt(4) == 0 ? random.nextInt(256) : ops[random.nextInt(ops.length)], payload);
            byte[] response = run(programs[random.nextInt(programs.length)], request);
            // A response, or a negative status of 1 byte: never a crash, never more than the cap.
            assertTrue(response.length <= BfAbi.RUNTIME_OUT_CAP);
            if (response.length == 1) {
                assertEquals("only the budget may stop a policy program", -3, response[0]);
            }
        }
    }

    @Test
    public void invalidProgramsAndBuffersFailCleanly() {
        byte[] request = frame(1, BfAbi.OP_LIMITS, new byte[0]);
        for (int program : new int[]{-1, 3, 255, Integer.MAX_VALUE}) {
            assertEquals(-4, run(program, request)[0]);
        }
        ByteBuffer out = ByteBuffer.allocateDirect(64);
        assertTrue(NativePolicy.nativeRun(BfAbi.PROG_DISCOVERY, null, 0, out) < 0);
        assertTrue(NativePolicy.nativeRun(BfAbi.PROG_DISCOVERY, direct(request, 8), 9, out) < 0);
        assertTrue(NativePolicy.nativeRun(BfAbi.PROG_DISCOVERY, direct(request, 8), -1, out) < 0);
        assertTrue(NativePolicy.nativeRun(BfAbi.PROG_DISCOVERY, ByteBuffer.allocate(8), 8, out) < 0);
    }

    @Test
    public void versionAndOpcodeMismatchesAreRefused() {
        for (int program : new int[]{BfAbi.PROG_POST, BfAbi.PROG_ACTION, BfAbi.PROG_DISCOVERY}) {
            byte[] badVersion = run(program, frame(2, BfAbi.OP_LIMITS, new byte[]{1, 2, 3}));
            assertEquals(BfAbi.ST_BAD_VERSION, badVersion[3]);
            byte[] badOp = run(program, frame(1, 0x7F, new byte[]{1, 2, 3}));
            assertEquals(BfAbi.ST_BAD_OPCODE, badOp[3]);
            assertEquals(BfAbi.HEADER_SIZE, badOp.length);
        }
    }

    @Test
    public void everyTruncationOfAValidRequestIsAnswered() {
        byte[] full = frame(1, BfAbi.OP_CLASSIFY_POST, new byte[]{
                0, 1, 0, 2,
                18, 0, 18, 5, 6, 7, 8, 7, 9, 10, 11, 1, 9, 12, 10, 10, 9, 1, 2, 1, 4, 0,
                7, 0, 7, 9, 12, 10, 10, 9, 1, 2, 0});
        byte[] expected = run(BfAbi.PROG_POST, full);
        assertEquals(BfAbi.V_PROMOTED, expected[8]);
        for (int length = 0; length < full.length; length++) {
            byte[] prefix = new byte[length];
            System.arraycopy(full, 0, prefix, 0, length);
            byte[] response = run(BfAbi.PROG_POST, prefix);
            assertTrue(response.length == 1 ? response[0] == -3 : response.length >= BfAbi.HEADER_SIZE);
        }
    }

    @Test
    public void oversizedRequestsFailOpen() {
        StringBuilder huge = new StringBuilder();
        while (huge.length() <= BfAbi.RUNTIME_IN_CAP) {
            huge.append("promoted-tweet-1-");
        }
        TimelinePost post = new TimelinePost("promoted-tweet-1-a").text(huge.toString());
        long failures = PolicyStats.failures();
        // The request cannot be sent, so even a promoted post is kept.
        assertEquals(BundledAdPatterns.Verdict.UNKNOWN,
                BundledAdPatterns.classifyTimelinePost(post, null, true).verdict);
        assertEquals(0, BundledAdPatterns.classifyPacked(post, null, true));
        assertTrue(PolicyStats.failures() > failures);
    }

    @Test
    public void sameRequestSameResponse() {
        Random random = new Random(0xD37);
        for (int index = 0; index < 2000; index++) {
            byte[] payload = new byte[random.nextInt(200)];
            random.nextBytes(payload);
            byte[] request = frame(1, 0x10 + random.nextInt(3), payload);
            assertArrayEquals(run(BfAbi.PROG_POST, request), run(BfAbi.PROG_POST, request));
        }
    }

    @Test
    public void concurrentClassificationMatchesSequential() throws Exception {
        Random random = new Random(0xC0C0);
        List<WidePost> posts = new ArrayList<>();
        for (int index = 0; index < 400; index++) {
            WidePost post = new WidePost();
            post.a = ParityInputs.text(random);
            post.b = ParityInputs.text(random);
            post.meta = random.nextInt(10) == 0 ? new ObfuscatedPromotedMetadata() : null;
            posts.add(post);
        }
        int[] expected = new int[posts.size()];
        for (int index = 0; index < posts.size(); index++) {
            expected[index] = BundledAdPatterns.classifyPacked(posts.get(index), null, false);
        }
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (int thread = 0; thread < 8; thread++) {
                final int seed = thread;
                futures.add(pool.submit((Callable<Integer>) () -> {
                    Random order = new Random(seed);
                    int mismatches = 0;
                    for (int round = 0; round < 5000; round++) {
                        int pick = order.nextInt(posts.size());
                        if (BundledAdPatterns.classifyPacked(posts.get(pick), null, false) != expected[pick]) {
                            mismatches++;
                        }
                    }
                    return mismatches;
                }));
            }
            for (Future<Integer> future : futures) {
                assertEquals(Integer.valueOf(0), future.get());
            }
        } finally {
            pool.shutdownNow();
        }
    }

    /** A text field whose reading re-enters the classifier on the same thread. */
    private static final class ReentrantText implements CharSequence {
        private final String value;

        ReentrantText(String value) {
            this.value = value;
        }

        @Override
        public int length() {
            BundledAdPatterns.classifyPacked(new TimelinePost("tweet-9"), null, false);
            return value.length();
        }

        @Override
        public char charAt(int index) {
            return value.charAt(index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return value.subSequence(start, end);
        }

        @Override
        public String toString() {
            return value;
        }
    }

    @Test
    public void reentrantRequestsUseTheirOwnFrame() {
        WidePost post = new WidePost();
        post.a = "tweet-1";
        post.c = new ReentrantText("promoted-tweet-2-a");
        BundledAdPatterns.Classification result = BundledAdPatterns.classifyTimelinePost(post, null, false);
        assertEquals(BundledAdPatterns.Verdict.PROMOTED, result.verdict);
        assertEquals("promoted-tweet-2-a", result.entryId);
    }

    @Test
    public void organicContentIsNeverBlockedWithoutPromotedEvidence() {
        Random random = new Random(0x0C);
        int checked = 0;
        for (int index = 0; index < HostCore.parityCases(); index++) {
            WidePost post = new WidePost();
            post.a = ParityInputs.text(random);
            post.b = ParityInputs.text(random);
            post.d = ParityInputs.text(random);
            if (LegacyEntryIds.kind(post.a) == LegacyEntryIds.Kind.PROMOTED
                    || LegacyEntryIds.kind(post.b) == LegacyEntryIds.Kind.PROMOTED
                    || LegacyEntryIds.kind(post.d) == LegacyEntryIds.Kind.PROMOTED) {
                continue;
            }
            checked++;
            assertEquals(0, BundledAdPatterns.classifyPacked(post, null, true) & BundledAdPatterns.PROMOTED);
        }
        assertTrue(checked > HostCore.parityCases() / 3);
    }

    @Test
    public void limitsComeFromThePolicy() {
        assertEquals(12, PolicyLimits.witnessWindow());
        assertEquals(16, PolicyLimits.maxBoundaries());
        assertEquals(64, PolicyLimits.maxCallers());
        assertEquals(5, PolicyLimits.scanDepth());
        assertEquals(160, PolicyLimits.scanObjects());
        assertEquals(24, PolicyLimits.scanItems());
        assertEquals(48, PolicyLimits.stackFrames());
        assertEquals(5, PolicyLimits.candidatesLogged());
        assertEquals(2500, PolicyLimits.reflectionClasses());
        assertEquals(500, PolicyLimits.videoClasses());
    }

    @Test
    public void responseValidationRejectsWrongLengths() {
        PolicyFrame frame = PolicyFrame.begin(BfAbi.OP_LIMITS);
        try {
            assertFalse(frame.send(BfAbi.PROG_DISCOVERY, BfAbi.LIMITS_SIZE + 1));
            frame.restart(BfAbi.OP_LIMITS);
            assertTrue(frame.send(BfAbi.PROG_DISCOVERY, BfAbi.LIMITS_SIZE));
            frame.restart(BfAbi.OP_LIMITS);
            // The right frame sent to the wrong program is refused as an unknown opcode.
            assertFalse(frame.send(BfAbi.PROG_POST, BfAbi.LIMITS_SIZE));
        } finally {
            frame.release();
        }
        assertNotEquals(0, PolicyStats.calls());
    }
}
