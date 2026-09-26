package my.MrxSiN.twitterhideads;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.compose.foundation.layout.ColumnScope;
import androidx.compose.runtime.Composer;
import androidx.compose.ui.Modifier;

import com.x.other.RelocatedBoundaries;
import com.x.urt.items.post.Boundaries;
import com.x.urt.items.post.Dependency;
import com.x.urt.items.post.PostModel;
import com.x.urt.items.post.TimelinePost;

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

/** RenderBoundaryShape scoring, resolver ranking, cache signatures and fallback order. */
public class AdaptiveResolutionTest {
    @org.junit.BeforeClass
    public static void load() {
        HostCore.ensure();
    }

    private static Method method(Class<?> owner, String name, Class<?>... parameters) throws Exception {
        return owner.getDeclaredMethod(name, parameters);
    }

    private static Method primary() throws Exception {
        return method(Boundaries.class, "e", PostModel.class, Modifier.class,
                ColumnScope.class, Dependency.class, Composer.class, int.class);
    }

    private static Method primaryWithDefaults() throws Exception {
        return method(Boundaries.class, "e", PostModel.class, Modifier.class,
                ColumnScope.class, Dependency.class, Composer.class, int.class, int.class);
    }

    private static Method relocated() throws Exception {
        return method(RelocatedBoundaries.class, "a", PostModel.class, Modifier.class,
                Composer.class, int.class);
    }

    @Test
    public void fullBoundaryScoresAboveThreshold() throws Exception {
        int score = RenderBoundaryShape.score(primary());
        assertTrue("score=" + score, score >= 320);
        assertTrue(RenderBoundaryShape.accepts(primary()));
        assertEquals(score, RenderBoundaryShape.score(primaryWithDefaults()));
    }

    @Test
    public void relocatedBoundaryIsEligibleButRanksBelowDirectOne() throws Exception {
        int relocated = RenderBoundaryShape.score(relocated());
        assertTrue(relocated > RenderBoundaryShape.NOT_A_BOUNDARY);
        assertTrue(relocated < RenderBoundaryShape.score(primary()));
    }

    @Test
    public void nonBoundaryShapesAreRejected() throws Exception {
        Method[] rejected = {
                method(Boundaries.class, "noMask", PostModel.class, Composer.class),
                method(Boundaries.class, "extraTrailing", PostModel.class, Composer.class,
                        int.class, int.class, int.class),
                method(Boundaries.class, "wrongMask", PostModel.class, Composer.class, long.class),
                method(Boundaries.class, "returnsValue", PostModel.class, Composer.class, int.class),
                method(Boundaries.class, "otherModel", String.class, Composer.class, int.class),
                method(Boundaries.class, "instance", PostModel.class, Composer.class, int.class),
        };
        for (Method candidate : rejected) {
            assertEquals(candidate.toString(), RenderBoundaryShape.NOT_A_BOUNDARY,
                    RenderBoundaryShape.score(candidate));
        }
    }

    @Test
    public void minimalBoundaryIsEligible() throws Exception {
        Method minimal = method(Boundaries.class, "minimal", TimelinePost.class,
                Composer.class, int.class);
        assertTrue(RenderBoundaryShape.score(minimal) > RenderBoundaryShape.NOT_A_BOUNDARY);
    }

    @Test
    public void selectRanksByScoreThenSignatureAndDeduplicates() throws Exception {
        List<AdaptiveHookResolver.Candidate> candidates = new ArrayList<>();
        candidates.add(new AdaptiveHookResolver.Candidate(relocated(), 330, "dexkit"));
        candidates.add(new AdaptiveHookResolver.Candidate(primaryWithDefaults(), 395, "dexkit"));
        candidates.add(new AdaptiveHookResolver.Candidate(primary(), 395, "dexkit"));
        candidates.add(new AdaptiveHookResolver.Candidate(primary(), 395, "dexfile"));

        AdaptiveHookResolver.Resolution resolution =
                AdaptiveHookResolver.select(candidates, "ok", "not-needed");

        assertEquals(3, resolution.methods.size());
        assertEquals(3, resolution.candidateCount);
        assertEquals(395, resolution.score);
        // Ties on score are broken by signature, so ordering is deterministic.
        String first = AdaptiveBoundaryCache.signature(resolution.methods.get(0));
        String second = AdaptiveBoundaryCache.signature(resolution.methods.get(1));
        assertTrue(first.compareTo(second) < 0);
        assertEquals(relocated(), resolution.methods.get(2));
    }

    @Test
    public void selectCapsTheNumberOfBoundaries() throws Exception {
        List<AdaptiveHookResolver.Candidate> candidates = new ArrayList<>();
        for (Method method : Boundaries.class.getDeclaredMethods()) {
            candidates.add(new AdaptiveHookResolver.Candidate(method, 400, "dexkit"));
        }
        for (Method method : AdaptiveResolutionTest.class.getDeclaredMethods()) {
            candidates.add(new AdaptiveHookResolver.Candidate(method, 350, "dexkit"));
        }
        assertTrue(candidates.size() > 16);
        AdaptiveHookResolver.Resolution resolution =
                AdaptiveHookResolver.select(candidates, "ok", "not-needed");
        assertEquals(16, resolution.methods.size());
        assertEquals(candidates.size(), resolution.candidateCount);
    }

    @Test
    public void selectFailsOpenWithoutCandidates() {
        AdaptiveHookResolver.Resolution resolution = AdaptiveHookResolver.select(
                Collections.<AdaptiveHookResolver.Candidate>emptyList(), "failed:x", "ok");
        assertNull(resolution.method());
        assertTrue(resolution.reason.startsWith("no-structural-candidate"));
    }

    @Test
    public void cacheSignatureRoundTrips() throws Exception {
        ClassLoader loader = getClass().getClassLoader();
        for (Method method : new Method[]{primary(), primaryWithDefaults(), relocated()}) {
            String signature = AdaptiveBoundaryCache.signature(method);
            assertEquals(method, AdaptiveBoundaryCache.resolveSignature(signature, loader));
        }
        assertEquals(
                "com.x.other.RelocatedBoundaries#a(com.x.urt.items.post.PostModel,"
                        + "androidx.compose.ui.Modifier,androidx.compose.runtime.Composer,int)",
                AdaptiveBoundaryCache.signature(relocated())
        );
    }

    @Test
    public void staleOrMalformedCacheSignaturesDoNotResolve() throws Exception {
        ClassLoader loader = getClass().getClassLoader();
        assertNull(AdaptiveBoundaryCache.resolveSignature(
                "com.x.urt.items.post.Boundaries#e(com.x.urt.items.post.PostModel)", loader));
        assertNull(AdaptiveBoundaryCache.resolveSignature("no-hash-or-parens", loader));
        assertNull(AdaptiveBoundaryCache.resolveSignature(
                "com.x.urt.items.post.Boundaries#e(", loader));
    }

    @Test(expected = ClassNotFoundException.class)
    public void cacheSignatureForMissingClassThrows() throws Exception {
        AdaptiveBoundaryCache.resolveSignature("com.x.Gone#a(int)", getClass().getClassLoader());
    }

    @Test
    public void fallbackScansPostPackageFirstAndOnlyKnownNamespaces() {
        Vector<String> entries = new Vector<>(Arrays.asList(
                "com.x.jetfuel.v2.element.attribute.a",
                "com.x.payments.a",
                "com.x.urt.items.post.c5",
                "com.x.mappers.module.a",
                "com.x.urt.x1",
                "androidx.compose.runtime.Composer",
                "com.x.urt.items.post.d"
        ));
        assertEquals(Arrays.asList(
                "com.x.urt.items.post.c5",
                "com.x.urt.items.post.d",
                "com.x.jetfuel.v2.element.attribute.a",
                "com.x.mappers.module.a",
                "com.x.urt.x1"
        ), BoundaryCandidateSource.fallbackScanOrder(entries.elements()));
    }
}
