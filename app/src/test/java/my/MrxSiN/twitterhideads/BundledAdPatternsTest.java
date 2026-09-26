package my.MrxSiN.twitterhideads;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.x.models.ObfuscatedPromotedMetadata;
import com.x.models.PostActionType;
import com.x.models.TimelinePromotedMetadata;
import com.x.urt.items.post.TimelinePost;

import org.junit.Test;

import my.MrxSiN.twitterhideads.BundledAdPatterns.Classification;
import my.MrxSiN.twitterhideads.BundledAdPatterns.Verdict;

public class BundledAdPatternsTest {
    @org.junit.BeforeClass
    public static void load() {
        HostCore.ensure();
    }

    private static Classification classify(Object model) {
        return BundledAdPatterns.classifyTimelinePost(model, null, true);
    }

    @Test
    public void directPromotedEntryIsPromoted() {
        Classification result = classify(new TimelinePost("promoted-tweet-1-abc"));
        assertEquals(Verdict.PROMOTED, result.verdict);
        assertEquals("promoted-tweet-1-abc", result.entryId);
        assertFalse(result.actionFallbackUsed);
    }

    @Test
    public void nestedPromotedEntryIsPromoted() {
        Classification result = classify(new TimelinePost("conversationthread-9-promoted-tweet-1-abc"));
        assertEquals(Verdict.PROMOTED, result.verdict);
    }

    @Test
    public void promotedEntryInAnyDirectStringFieldCounts() {
        Classification result = classify(new TimelinePost("tweet-5").text("promoted-tweet-1-abc"));
        assertEquals(Verdict.PROMOTED, result.verdict);
        assertEquals("promoted-tweet-1-abc", result.entryId);
    }

    @Test
    public void validatedAndObfuscatedMetadataArePromoted() {
        assertEquals(Verdict.PROMOTED,
                classify(new TimelinePost("tweet-1").metadata(new TimelinePromotedMetadata())).verdict);
        assertEquals(Verdict.PROMOTED,
                classify(new TimelinePost(null).metadata(new ObfuscatedPromotedMetadata())).verdict);
    }

    @Test
    public void confidentOrganicEntrySkipsTheActionScan() {
        TimelinePost post = new TimelinePost("tweet-2102692236387627106")
                .actions(PostActionType.PromotedDismissAd);
        Classification result = classify(post);
        assertEquals(Verdict.ORGANIC, result.verdict);
        assertFalse(result.actionFallbackUsed);
        assertTrue(result.identifiesPost());
    }

    @Test
    public void nestedOrganicEntryIsOrganic() {
        Classification result = classify(new TimelinePost("conversationthread-1-tweet-2"));
        assertEquals(Verdict.ORGANIC, result.verdict);
    }

    @Test
    public void lookalikeEntryIsNeverPromotedByTheEntryRule() {
        for (String value : new String[]{"not-promoted-x", "unpromoted-1", "promoted-deal"}) {
            Classification result = classify(new TimelinePost(value));
            assertEquals(value, Verdict.UNKNOWN, result.verdict);
            assertNull(value, result.entryId);
        }
    }

    @Test
    public void organicEntryWithPromotedTextFallsBackToTheScan() {
        TimelinePost post = new TimelinePost("tweet-1")
                .text("I was promoted today")
                .actions(PostActionType.PromotedAdsInfo);
        Classification result = classify(post);
        assertEquals(Verdict.PROMOTED, result.verdict);
        assertTrue(result.actionFallbackUsed);
        assertTrue(result.signals.contains("PromotedAdsInfo"));
    }

    @Test
    public void unknownModelUsesTheActionScan() {
        Classification promoted = classify(new TimelinePost("2102634587474268204")
                .actions(PostActionType.Reply, PostActionType.PromotedDismissAd));
        assertEquals(Verdict.PROMOTED, promoted.verdict);
        assertTrue(promoted.actionFallbackUsed);

        Classification unknown = classify(new TimelinePost("2102634587474268204")
                .actions(PostActionType.Reply, PostActionType.Like));
        assertEquals(Verdict.UNKNOWN, unknown.verdict);
        assertTrue(unknown.actionFallbackUsed);
        assertFalse(unknown.identifiesPost());
    }

    @Test
    public void actionScanCanBeDisabled() {
        Classification result = BundledAdPatterns.classifyTimelinePost(
                new TimelinePost(null).actions(PostActionType.PromotedDismissAd),
                null,
                false
        );
        assertEquals(Verdict.UNKNOWN, result.verdict);
        assertFalse(result.actionFallbackUsed);
    }

    @Test
    public void validatedRenderModelIsOrganicWithoutAScan() {
        Classification result = BundledAdPatterns.classifyTimelinePost(
                new TimelinePost(null).actions(PostActionType.PromotedDismissAd),
                TimelinePost.class.getName(),
                true
        );
        assertEquals(Verdict.ORGANIC, result.verdict);
        assertFalse(result.actionFallbackUsed);
    }

    @Test
    public void actionScanResultIsCachedPerModelInstance() {
        TimelinePost post = new TimelinePost(null).actions(PostActionType.PromotedDismissAd);
        Classification first = classify(post);
        post.actions(PostActionType.Reply);
        assertSame(first, classify(post));

        TimelinePost other = new TimelinePost(null).actions(PostActionType.Reply);
        assertEquals(Verdict.UNKNOWN, classify(other).verdict);
    }

    @Test
    public void nullModelIsUnknown() {
        assertSame(Classification.UNKNOWN, classify(null));
        assertFalse(Classification.UNKNOWN.promoted());
    }

    @Test
    public void unrelatedObjectIsUnknown() {
        assertEquals(Verdict.UNKNOWN, classify("promoted-tweet-1-a").verdict);
    }
}
