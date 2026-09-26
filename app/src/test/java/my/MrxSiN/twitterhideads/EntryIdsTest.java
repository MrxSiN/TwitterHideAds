package my.MrxSiN.twitterhideads;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Random;

/** The entry grammar, now implemented by post.bf, against the v2.1.0 vectors. */
public class EntryIdsTest {
    @org.junit.BeforeClass
    public static void load() {
        HostCore.ensure();
    }

    private static final String[] PROMOTED = {
            "promoted-tweet-2102692433305718851-6707f8b5db57b19e",
            "promoted-tweet-2100869123605758320",
            "PROMOTED-TWEET-1-ABC",
            "conversationthread-2102634587474268204-promoted-tweet-2094154228436742320-6707349df11a283f",
            "search-conversation-17-promoted-tweet-42-deadbeef",
            "tweetdetailrelatedtweets-1-promoted-tweet-2-a",
            "promoted-trend-12",
    };

    private static final String[] ORGANIC = {
            "tweet-2102692236387627106",
            "conversationthread-2102634587474268204-tweet-2102634587474268204",
            "tweetdetailrelatedtweets-2102634587474268204-tweet-99",
            "search-conversation-1-tweet-2",
            "Tweet-1",
    };

    private static final String[] NEITHER = {
            "",
            "promoted",
            "promoted-",
            "promoted-deal",
            "promoted-tweet-",
            "promoted-tweet-abc",
            "promoted-tweet-1-",
            "not-promoted-x",
            "not-promoted-tweet-1",
            "unpromoted-1",
            "unpromoted-tweet-1",
            "xpromoted-tweet-1",
            "-promoted-tweet-1",
            "conversationthread-promoted-tweet-1-a",
            "conversationthread-1--promoted-tweet-2",
            "conversationthread-1-promoted-tweet",
            "promoted_tweet_1",
            "promoted-tweet-1 ",
            " promoted-tweet-1",
            "promoted-tweet-1\n",
            "I got promoted-tweet-1 today",
            "https://x.com/promoted-tweet-1",
            "This post was promoted by nobody",
            "tweet-",
            "tweet-abc",
            "retweet-1",
            "xtweet-1",
            "tweet 1",
            "2102634587474268204",
            "cursor-top-2102634587474268204",
            "who-to-follow-123",
    };

    @Test
    public void recognisesPromotedEntries() {
        for (String value : PROMOTED) {
            assertEquals(value, 2, HostCore.kind(value));
        }
    }

    @Test
    public void recognisesOrganicEntries() {
        for (String value : ORGANIC) {
            assertEquals(value, 1, HostCore.kind(value));
        }
    }

    @Test
    public void rejectsLookalikesAndFreeText() {
        for (String value : NEITHER) {
            assertEquals("[" + value + "]", 0, HostCore.kind(value));
        }
    }

    @Test
    public void rejectsOverlongValues() {
        StringBuilder value = new StringBuilder("promoted-tweet-1");
        while (value.length() <= 256) {
            value.append("-a");
        }
        assertEquals(0, HostCore.kind(value));
    }

    /**
     * Mutating a valid promoted entry with a single inserted, replaced or
     * deleted character must never produce an ORGANIC verdict, and any value
     * still judged PROMOTED must contain the promoted token at a boundary.
     */
    @Test
    public void fuzzedMutationsNeverFlipToOrganic() {
        String alphabet = "abcdefghijklmnopqrstuvwxyz0123456789-_ .PT";
        Random random = new Random(0x5EED);
        for (String seed : PROMOTED) {
            for (int round = 0; round < 2000; round++) {
                StringBuilder mutated = new StringBuilder(seed);
                int position = random.nextInt(mutated.length());
                char replacement = alphabet.charAt(random.nextInt(alphabet.length()));
                switch (random.nextInt(3)) {
                    case 0:
                        mutated.insert(position, replacement);
                        break;
                    case 1:
                        mutated.setCharAt(position, replacement);
                        break;
                    default:
                        mutated.deleteCharAt(position);
                        break;
                }
                String value = mutated.toString();
                int kind = HostCore.kind(value);
                assertNotEquals(value, 1, kind);
                if (kind == 2) {
                    String lower = value.toLowerCase(java.util.Locale.ROOT);
                    assertTrue(value, lower.startsWith("promoted-") || lower.contains("-promoted-"));
                    assertFalse(value, lower.contains(" "));
                }
            }
        }
    }

    @Test
    public void mentionsPromotedFlagsAnyOccurrence() {
        assertTrue(HostCore.mentionsPromoted("not-promoted-x"));
        assertTrue(HostCore.mentionsPromoted("Promoted by"));
        assertFalse(HostCore.mentionsPromoted("tweet 1"));
    }

    @Test
    public void redactionKeepsGrammarAndDropsIds() {
        assertEquals(
                "conversationthread-<n>-promoted-tweet-<n>-<n>",
                LogPrivacy.redact("conversationthread-2102634587474268204-promoted-tweet-2094154228436742320-6707349df11a283f")
        );
        assertEquals("tweet-<n>", LogPrivacy.redact("tweet-2102692236387627106"));
        assertEquals("unknown", LogPrivacy.redact(null));
        assertEquals("unknown", LogPrivacy.redact(""));
    }
}
