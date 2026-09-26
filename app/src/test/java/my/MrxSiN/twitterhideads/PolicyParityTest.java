package my.MrxSiN.twitterhideads;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.x.models.ObfuscatedPromotedMetadata;
import com.x.models.OtherActions;
import com.x.models.PostActionType;
import com.x.models.TimelinePromotedMetadata;
import com.x.models.timelines.items.VideoItem;
import com.x.urt.VideoShapes;
import com.x.urt.items.post.WidePost;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import kotlinx.collections.immutable.FakePersistentVector;
import my.MrxSiN.twitterhideads.legacy.LegacyBundledAdPatterns;
import my.MrxSiN.twitterhideads.legacy.LegacyDiscoveryRules;
import my.MrxSiN.twitterhideads.legacy.LegacyEntryIds;
import my.MrxSiN.twitterhideads.legacy.LegacyPromotedActionScanner;
import my.MrxSiN.twitterhideads.legacy.LegacyRenderBoundaryShape;
import my.MrxSiN.twitterhideads.legacy.LegacyVideoDatasetClassifier;

/**
 * Old Java result must equal new Brainfuck result: the frozen v2.1.0 policy
 * (legacy package) against the host build of libtwitterbf, on randomized and
 * exhaustive inputs. Case counts scale with -PparityCases.
 */
public class PolicyParityTest {
    private static final int CASES = HostCore.parityCases();
    private static long total;

    @BeforeClass
    public static void load() {
        HostCore.ensure();
    }

    @AfterClass
    public static void report() {
        System.out.println("PolicyParityTest: " + total + " old-vs-new comparisons");
    }

    private static void count(int cases) {
        synchronized (PolicyParityTest.class) {
            total += cases;
        }
    }

    // ------------------------------------------------------------ entry grammar

    private static int legacyKind(CharSequence value) {
        switch (LegacyEntryIds.kind(value)) {
            case PROMOTED:
                return 2;
            case ORGANIC:
                return 1;
            default:
                return 0;
        }
    }

    private static void assertEntryParity(String value) {
        int expected = legacyKind(value);
        assertEquals(repr(value), expected, HostCore.kind(value));
        if (expected == 0) {
            assertEquals(repr(value), LegacyEntryIds.mentionsPromoted(value), HostCore.mentionsPromoted(value));
        }
    }

    @Test
    public void entryGrammarRandom() {
        Random random = new Random(0xE17);
        for (int index = 0; index < CASES; index++) {
            assertEntryParity(ParityInputs.text(random));
        }
        count(CASES);
    }

    /** Every BMP char alone and inside identifiers (case mapping, Kelvin sign, dotted I). */
    @Test
    public void entryGrammarEveryCharacter() {
        int cases = 0;
        for (int c = 0; c < 0x10000; c++) {
            String ch = String.valueOf((char) c);
            for (String value : new String[]{ch, "promoted-tweet-1-" + ch, "promo" + ch + "ted",
                    "conversationthread-1-" + ch + "weet-2"}) {
                assertEntryParity(value);
                cases++;
            }
        }
        count(cases);
    }

    // ------------------------------------------------------------ timeline classification

    private static Object randomAction(Random random) {
        switch (random.nextInt(7)) {
            case 0:
                return PostActionType.values()[random.nextInt(PostActionType.values().length)];
            case 1:
                return OtherActions.values()[random.nextInt(OtherActions.values().length)];
            case 2:
                return new String[]{"PromotedAdsInfo", "x#PromotedReportAd", "PromotedDismissAd#", "#",
                        "Promoted", "a#b#PromotedDismissAd", "Reply"}[random.nextInt(7)];
            case 3:
                return random.nextInt(1000);
            case 4:
                return ParityInputs.text(random);
            case 5:
                return new StringBuilder("PromotedReportAd");
            default:
                return null;
        }
    }

    private static WidePost randomPost(Random random) {
        WidePost post = new WidePost();
        post.a = random.nextInt(4) == 0 ? null : ParityInputs.text(random);
        post.b = random.nextInt(3) == 0 ? null : ParityInputs.text(random);
        post.c = random.nextInt(3) == 0 ? null : new StringBuilder(ParityInputs.text(random));
        post.d = random.nextInt(2) == 0 ? null : ParityInputs.text(random);
        post.e = random.nextInt(2) == 0 ? null : ParityInputs.text(random);
        int meta = random.nextInt(10);
        post.meta = meta == 0 ? new TimelinePromotedMetadata()
                : meta == 1 ? new ObfuscatedPromotedMetadata()
                : meta == 2 ? "PromotedMetadata" : null;
        post.other = random.nextInt(8) == 0 ? new ObfuscatedPromotedMetadata() : null;
        if (random.nextBoolean()) {
            post.actions = new ArrayList<>();
            int actions = random.nextInt(6);
            for (int index = 0; index < actions; index++) {
                post.actions.add(randomAction(random));
            }
        }
        if (random.nextInt(4) == 0) {
            post.nested = new WidePost.Nested();
            post.nested.value = randomAction(random);
            post.nested.more = Arrays.asList(randomAction(random), randomAction(random));
        }
        return post;
    }

    @Test
    public void timelineClassificationRandom() {
        Random random = new Random(0xC1A55);
        for (int index = 0; index < CASES; index++) {
            WidePost post = randomPost(random);
            String expectedModel = random.nextInt(10) == 0 ? WidePost.class.getName() : null;
            boolean fallback = random.nextInt(8) != 0;
            LegacyBundledAdPatterns.Classification expected =
                    LegacyBundledAdPatterns.classifyTimelinePost(post, expectedModel, fallback);
            BundledAdPatterns.Classification actual =
                    BundledAdPatterns.classifyTimelinePost(post, expectedModel, fallback);
            String context = describe(post);
            assertEquals(context, expected.verdict.name(), actual.verdict.name());
            assertEquals(context, expected.entryId, actual.entryId);
            assertEquals(context, new ArrayList<>(expected.signals), new ArrayList<>(actual.signals));
            assertEquals(context, expected.actionFallbackUsed, actual.actionFallbackUsed);

            int packed = BundledAdPatterns.classifyPacked(post, expectedModel, fallback);
            assertEquals(context, actual.promoted(), (packed & BundledAdPatterns.PROMOTED) != 0);
            assertEquals(context, actual.identifiesPost(), (packed & BundledAdPatterns.IDENTIFIES_POST) != 0);
        }
        count(CASES);
    }

    @Test
    public void actionScanRandom() {
        Random random = new Random(0xAC7);
        for (int index = 0; index < CASES / 2; index++) {
            WidePost post = randomPost(random);
            LegacyPromotedActionScanner.Match expected = LegacyPromotedActionScanner.inspect(post);
            PromotedActionScanner.Match actual = PromotedActionScanner.inspect(post);
            assertEquals(describe(post), expected.promoted, actual.promoted);
            assertEquals(describe(post), new ArrayList<>(expected.actions), new ArrayList<>(actual.actions));
            Object scalar = randomAction(random);
            if (scalar != null) {
                String legacy = LegacyPromotedActionScanner.promotedActionName(scalar);
                PromotedActionScanner.Match single = PromotedActionScanner.inspect(scalar);
                assertEquals(String.valueOf(scalar), legacy == null ? List.of() : List.of(legacy),
                        new ArrayList<>(single.actions));
            }
        }
        count(CASES);
    }

    // ------------------------------------------------------------ Video Tab

    private static Object randomItem(Random random) {
        switch (random.nextInt(8)) {
            case 0:
                return null;
            case 1:
                return "cursor-bottom-" + random.nextInt(9);
            case 2:
                return new VideoItem(null, random.nextBoolean() ? new ObfuscatedPromotedMetadata() : null);
            default:
                return new VideoItem(random.nextInt(6) == 0 ? null : ParityInputs.text(random),
                        random.nextInt(10) == 0 ? new TimelinePromotedMetadata() : null);
        }
    }

    @Test
    public void videoBatchRandom() {
        Random random = new Random(0x71DE0);
        for (int index = 0; index < CASES / 4; index++) {
            int size = random.nextInt(20) == 0 ? 130 + random.nextInt(120) : random.nextInt(12);
            Object container;
            List<Object> list = new ArrayList<>();
            for (int item = 0; item < size; item++) {
                list.add(randomItem(random));
            }
            switch (random.nextInt(4)) {
                case 0: {
                    Map<Object, Object> map = new LinkedHashMap<>();
                    for (Object item : list) {
                        map.put(random.nextInt(3) == 0 ? (Object) map.size() : ParityInputs.text(random) + map.size(),
                                item);
                    }
                    container = map;
                    break;
                }
                case 1:
                    container = list.toArray();
                    break;
                case 2:
                    container = new FakePersistentVector<>(list);
                    break;
                default:
                    container = list;
            }
            LegacyVideoDatasetClassifier.Batch expected = LegacyVideoDatasetClassifier.inspect(container);
            VideoDatasetClassifier.Batch actual = VideoDatasetClassifier.inspect(container);
            String context = "batch#" + index + " " + container.getClass().getSimpleName() + " "
                    + (container instanceof Map ? container.toString() : list.toString());
            assertEquals(context, expected.containerSize, actual.containerSize);
            assertEquals(context, expected.itemCount, actual.itemCount);
            assertEquals(context, expected.promotedCount, actual.promotedCount);
            assertEquals(context, expected.normalCount, actual.normalCount);
            assertEquals(context, expected.safeMixedVideoBatch(), actual.safeMixedVideoBatch());
            assertEquals(context, new ArrayList<>(expected.promotedIds), new ArrayList<>(actual.promotedIds));
            assertEquals(context, new ArrayList<>(expected.normalIds), new ArrayList<>(actual.normalIds));
            assertEquals(context, LegacyVideoDatasetClassifier.filteredElements(container),
                    VideoDatasetClassifier.filteredElements(container, actual));
            for (Object item : list.subList(0, Math.min(list.size(), 4))) {
                LegacyVideoDatasetClassifier.Classification e = LegacyVideoDatasetClassifier.classify(item, null);
                VideoDatasetClassifier.Classification a = VideoDatasetClassifier.classify(item, null);
                assertEquals(context, e.postItem, a.postItem);
                assertEquals(context, e.promoted, a.promoted);
                assertEquals(context, e.normal, a.normal);
                assertEquals(context, e.entryId, a.entryId);
            }
        }
        count(CASES / 4);
    }

    @Test
    public void videoDecisionsRandom() {
        Random random = new Random(0xDEC1DE);
        for (int index = 0; index < CASES; index++) {
            int size = random.nextInt(300);
            int filtered = random.nextInt(300);
            int normal = random.nextInt(129);
            assertEquals(LegacyDiscoveryRules.proceedToCopy(size, filtered, normal),
                    VideoDatasetFilter.proceedToCopy(size, filtered, normal));
            int vPromoted = random.nextInt(3) == 0 ? random.nextInt(5) : 0;
            int vNormal = random.nextInt(129);
            int vSize = random.nextBoolean() ? filtered : random.nextInt(300);
            assertEquals(LegacyDiscoveryRules.acceptVerified(filtered, normal, vPromoted, vNormal, vSize),
                    VideoDatasetFilter.acceptVerified(filtered, normal, vPromoted, vNormal, vSize));
            int count = random.nextInt(4);
            int best = random.nextInt(450);
            int second = count > 1 ? random.nextInt(best + 1) : -1;
            assertEquals(LegacyDiscoveryRules.videoSelect(count, best, second),
                    VideoDatasetResolver.selectDecision(count, best, second));
        }
        count(3 * CASES);
    }

    // ------------------------------------------------------------ discovery

    /** Every declared method of a broad set of real classes. */
    private static List<Method> realMethods() {
        List<Method> methods = new ArrayList<>();
        Class<?>[] owners = {
                com.x.urt.items.post.Boundaries.class, com.x.other.RelocatedBoundaries.class,
                VideoShapes.class, String.class, java.util.ArrayList.class, java.util.Collections.class,
                java.util.HashMap.class, java.lang.Math.class, java.util.Arrays.class, Integer.class,
                java.util.concurrent.ConcurrentHashMap.class, java.lang.reflect.Array.class, Character.class,
                java.util.stream.Collectors.class, java.lang.invoke.MethodHandles.class, Thread.class
        };
        for (Class<?> owner : owners) {
            methods.addAll(Arrays.asList(owner.getDeclaredMethods()));
        }
        return methods;
    }

    @Test
    public void renderBoundaryScoresOnRealMethods() {
        List<Method> methods = realMethods();
        int accepted = 0;
        for (Method method : methods) {
            int expected = LegacyRenderBoundaryShape.score(method);
            assertEquals(method.toString(), expected, RenderBoundaryShape.score(method));
            boolean legacyAccept = expected != LegacyRenderBoundaryShape.NOT_A_BOUNDARY
                    && expected >= LegacyDiscoveryRules.RENDER_ACTIVE_THRESHOLD;
            assertEquals(method.toString(), legacyAccept, RenderBoundaryShape.accepts(method));
            accepted += legacyAccept ? 1 : 0;
            assertEquals(method.toString(), LegacyDiscoveryRules.videoScore(method), videoScore(method));
        }
        assertTrue("fixtures must include accepted boundaries", accepted >= 2);
        count(3 * methods.size());
    }

    private static int videoScore(Method method) {
        VideoDatasetResolver.Candidate candidate = VideoDatasetResolver.evaluate(method, "test");
        return candidate == null ? -1 : candidate.score;
    }

    @Test
    public void videoBoundaryFixturesScore() {
        int eligible = 0;
        for (Method method : VideoShapes.class.getDeclaredMethods()) {
            int expected = LegacyDiscoveryRules.videoScore(method);
            assertEquals(method.toString(), expected, videoScore(method));
            VideoDatasetResolver.Candidate candidate = VideoDatasetResolver.evaluate(method, "test");
            if (candidate != null) {
                eligible++;
                assertEquals(method.toString(), expected >= LegacyDiscoveryRules.VIDEO_ACTIVE_THRESHOLD,
                        candidate.accepted);
            }
        }
        assertTrue(eligible >= 3);
    }

    @Test
    public void profileSelectionRandom() {
        Random random = new Random(0x9E1);
        String alphabet = "12.780 -+\tx\u0001 ";
        List<String> names = new ArrayList<>(Arrays.asList("12.8.0", "12.8.0-release.01", "12.7.1", " 12.7.1 ",
                "12.7.10", "12.28.0-prod.01", "12.8.0\tx", "\t12.8.0\t", "12.9.1", ""));
        for (int index = 0; index < CASES; index++) {
            StringBuilder name = new StringBuilder();
            if (random.nextBoolean()) {
                name.append(random.nextBoolean() ? "12.8.0" : "12.7.1");
            }
            int extra = random.nextInt(6);
            for (int c = 0; c < extra; c++) {
                name.insert(random.nextInt(name.length() + 1), alphabet.charAt(random.nextInt(alphabet.length())));
            }
            names.add(name.toString());
        }
        for (String name : names) {
            CompatibilityProfile.Profile profile =
                    CompatibilityProfile.selectExact(new CompatibilityProfile.DetectedVersion(name, 1));
            assertEquals(repr(name), LegacyDiscoveryRules.selectExact(name), profile == null ? null : profile.id);
        }
        count(names.size());
    }

    // ------------------------------------------------------------ helpers

    private static String describe(WidePost post) {
        return "WidePost{a=" + repr(post.a) + ", b=" + repr(post.b) + ", c=" + repr(post.c)
                + ", d=" + repr(post.d) + ", e=" + repr(post.e) + ", meta=" + post.meta
                + ", other=" + post.other + ", actions=" + post.actions + "}";
    }

    static String repr(CharSequence value) {
        if (value == null) {
            return "null";
        }
        StringBuilder out = new StringBuilder("\"");
        for (int index = 0; index < value.length() && index < 120; index++) {
            char c = value.charAt(index);
            if (c < 0x20 || c > 0x7e) {
                out.append(String.format("\\u%04x", (int) c));
            } else {
                out.append(c);
            }
        }
        return out.append(value.length() > 120 ? "...\"" : "\"").toString();
    }
}
