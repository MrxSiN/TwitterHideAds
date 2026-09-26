package my.MrxSiN.twitterhideads;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.x.models.ObfuscatedPromotedMetadata;
import com.x.models.TimelinePromotedMetadata;

import org.junit.Test;

/** PromotedMetadata, IdentityCache, BoundaryWitness and BlockRecorder rules. */
public class SmallRulesTest {
    @Test
    public void promotedMetadataMatchesValidatedNameAndSuffix() {
        assertTrue(PromotedMetadata.isType("com.x.models.TimelinePromotedMetadata"));
        assertTrue(PromotedMetadata.isType("com.x.a.b.PromotedMetadata"));
        assertTrue(PromotedMetadata.isInstance(new TimelinePromotedMetadata()));
        assertTrue(PromotedMetadata.isInstance(new ObfuscatedPromotedMetadata()));
    }

    @Test
    public void promotedMetadataRejectsLookalikes() {
        assertFalse(PromotedMetadata.isType(null));
        assertFalse(PromotedMetadata.isType("com.x.models.PromotedMetadataFactory"));
        assertFalse(PromotedMetadata.isType("com.x.models.TimelineMetadata"));
        assertFalse(PromotedMetadata.isInstance(null));
        assertFalse(PromotedMetadata.isInstance("PromotedMetadata"));
    }

    @Test
    public void identityCacheUsesIdentityNotEquality() {
        IdentityCache<String> cache = new IdentityCache<>(8);
        String key = new String("same");
        cache.put(key, "value");
        assertEquals("value", cache.get(key));
        assertNull(cache.get(new String("same")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void identityCacheRequiresPowerOfTwo() {
        new IdentityCache<String>(6);
    }

    @Test
    public void witnessConfirmsOnFirstPostLikeModel() {
        BoundaryWitness witness = new BoundaryWitness(3);
        assertNull(witness.record(false));
        assertSame(BoundaryWitness.State.CONFIRMED, witness.record(true));
        assertSame(BoundaryWitness.State.CONFIRMED, witness.state());
        for (int index = 0; index < 10; index++) {
            assertNull(witness.record(false));
        }
        assertSame(BoundaryWitness.State.CONFIRMED, witness.state());
    }

    @Test
    public void witnessRejectsAfterWindowWithoutPosts() {
        BoundaryWitness witness = new BoundaryWitness(3);
        assertNull(witness.record(false));
        assertNull(witness.record(false));
        assertSame(BoundaryWitness.State.REJECTED, witness.record(false));
        assertNull(witness.record(true));
        assertSame(BoundaryWitness.State.REJECTED, witness.state());
        assertEquals(3, witness.observed());
    }

    @Test(expected = IllegalArgumentException.class)
    public void witnessRequiresPositiveWindow() {
        new BoundaryWitness(0);
    }

    @Test
    public void blockRecorderKeyTrackingIsBounded() {
        BlockRecorder recorder = new BlockRecorder();
        assertTrue(recorder.markSeen("promoted-tweet-0-a"));
        assertFalse(recorder.markSeen("promoted-tweet-0-a"));
        for (int index = 1; index < BlockRecorder.MAX_TRACKED_KEYS * 4; index++) {
            recorder.markSeen("promoted-tweet-" + index + "-a");
        }
        assertEquals(BlockRecorder.MAX_TRACKED_KEYS, recorder.trackedKeys());
    }
}
