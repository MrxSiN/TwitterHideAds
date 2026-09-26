package my.MrxSiN.twitterhideads;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.x.models.ObfuscatedPromotedMetadata;
import com.x.models.timelines.items.VideoItem;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import kotlinx.collections.immutable.BuilderlessVector;
import kotlinx.collections.immutable.FakePersistentVector;
import kotlinx.collections.immutable.ImmutableListMarker;

/** Video Tab batch classification and persistent-list copying. */
public class VideoDatasetTest {
    @org.junit.BeforeClass
    public static void load() {
        HostCore.ensure();
    }

    private static VideoItem organic(int id) {
        return new VideoItem("tweet-" + id, null);
    }

    private static VideoItem promoted(int id) {
        return new VideoItem("promoted-tweet-" + id, null);
    }

    private static List<Object> mixed() {
        return Arrays.<Object>asList(organic(1), promoted(2), organic(3), "cursor-bottom-1", organic(4));
    }

    @Test
    public void mixedBatchIsSafeToFilter() {
        VideoDatasetClassifier.Batch batch = VideoDatasetClassifier.inspect(mixed());
        assertEquals(5, batch.containerSize);
        assertEquals(4, batch.itemCount);
        assertEquals(1, batch.promotedCount);
        assertEquals(3, batch.normalCount);
        assertTrue(batch.safeMixedVideoBatch());
        assertEquals(Collections.singleton("promoted-tweet-2"), batch.promotedIds);
    }

    @Test
    public void homogeneousOrTinyBatchesAreLeftAlone() {
        assertFalse(VideoDatasetClassifier.inspect(
                Arrays.asList(organic(1), organic(2), organic(3))).safeMixedVideoBatch());
        assertFalse(VideoDatasetClassifier.inspect(
                Arrays.asList(promoted(1), promoted(2), promoted(3))).safeMixedVideoBatch());
        assertFalse(VideoDatasetClassifier.inspect(
                Arrays.asList(organic(1), promoted(2))).safeMixedVideoBatch());
        assertFalse(VideoDatasetClassifier.inspect(null).safeMixedVideoBatch());
        assertFalse(VideoDatasetClassifier.inspect("not a container").safeMixedVideoBatch());
    }

    @Test
    public void metadataOnlyItemIsPromoted() {
        VideoDatasetClassifier.Classification result = VideoDatasetClassifier.classify(
                new VideoItem(null, new ObfuscatedPromotedMetadata()), null);
        assertTrue(result.postItem);
        assertTrue(result.promoted);
        assertFalse(result.normal);
        assertTrue(result.displayId().startsWith("metadata-only:"));
    }

    @Test
    public void lookalikeIdsAreNotPostItems() {
        for (String value : new String[]{"not-promoted-x", "unpromoted-1", "promoted-deal", "tweet-x"}) {
            VideoDatasetClassifier.Classification result =
                    VideoDatasetClassifier.classify(new VideoItem(value, null), null);
            assertFalse(value, result.postItem);
        }
    }

    @Test
    public void nonPostClassesAreIgnored() {
        assertSame(VideoDatasetClassifier.Classification.NONE,
                VideoDatasetClassifier.classify("promoted-tweet-1", null));
        assertSame(VideoDatasetClassifier.Classification.NONE,
                VideoDatasetClassifier.classify(null, null));
    }

    @Test
    public void mapBatchesUseKeysAsEntryIds() {
        Map<String, Object> batch = new LinkedHashMap<>();
        batch.put("tweet-1", new VideoItem(null, null));
        batch.put("promoted-tweet-2", new VideoItem(null, null));
        batch.put("tweet-3", new VideoItem(null, null));
        VideoDatasetClassifier.Batch inspected = VideoDatasetClassifier.inspect(batch);
        assertEquals(1, inspected.promotedCount);
        assertEquals(2, inspected.normalCount);
    }

    @Test
    public void filteringKeepsEverythingButPromotedItemsInOrder() {
        List<Object> original = mixed();
        List<Object> filtered = VideoDatasetClassifier.filteredElements(original, VideoDatasetClassifier.inspect(original));
        assertEquals(Arrays.asList(original.get(0), original.get(2), original.get(3), original.get(4)),
                filtered);
        assertEquals(0, VideoDatasetClassifier.inspect(filtered).promotedCount);
    }

    @Test
    public void copierUsesTheRenamedBuilderApi() {
        FakePersistentVector<Object> original = new FakePersistentVector<>(mixed());
        List<Object> filtered = VideoDatasetClassifier.filteredElements(original, VideoDatasetClassifier.inspect(original));

        Object copy = PersistentListCopier.copy(original, ImmutableListMarker.class, filtered);

        assertNotNull(copy);
        assertTrue(copy instanceof FakePersistentVector);
        assertEquals(filtered, new ArrayList<>((List<?>) copy));
        assertEquals("original is never mutated", 5, original.size());
    }

    @Test
    public void copierFailsOpenWithoutBuilderShapeOrOnTypeMismatch() {
        assertNull(PersistentListCopier.copy(
                new BuilderlessVector<>(), ImmutableListMarker.class, new ArrayList<>()));
        assertNull(PersistentListCopier.copy(
                new ArrayList<>(mixed()), List.class, new ArrayList<>()));
        assertNull(PersistentListCopier.copy(
                new FakePersistentVector<>(mixed()), Map.class, new ArrayList<>()));
        assertNull(PersistentListCopier.copy(null, List.class, new ArrayList<>()));
    }
}
