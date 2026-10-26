package my.MrxSiN.twitterhideads;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.x.models.timelines.items.RtbImageAd;
import com.x.models.timelines.items.TimelineModule;
import com.x.models.timelines.items.VideoItem;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class RtbAdFilterTest {
    @Test
    public void dropsOnlyRtbItemsAndKeepsOrder() {
        VideoItem first = new VideoItem("tweet-1", null);
        VideoItem second = new VideoItem("tweet-2", null);
        List<Object> items = Arrays.asList(first, new RtbImageAd(), second, new RtbImageAd());
        assertEquals(Arrays.asList(first, second), RtbAdFilter.withoutRtbAds(items, List.class));
    }

    @Test
    public void listsWithoutRtbItemsAreLeftAlone() {
        assertNull(RtbAdFilter.withoutRtbAds(Arrays.asList(new VideoItem("tweet-1", null)), List.class));
        assertNull(RtbAdFilter.withoutRtbAds("not a list", List.class));
    }

    @Test
    public void rtbNameOutsideTheItemPackageIsNotAnAd() {
        Object lookalike = new Object() {
            @Override
            public String toString() {
                return "UrtTimelineRtbImageAd(";
            }
        };
        assertFalse(RtbAdFilter.isRtbAd(lookalike));
        assertTrue(RtbAdFilter.isRtbAd(new RtbImageAd()));
        assertFalse(RtbAdFilter.isRtbAd(null));
    }

    @Test
    public void dropsModulesThatOnlyWrapRtbAds() {
        VideoItem post = new VideoItem("tweet-1", null);
        TimelineModule adModule = new TimelineModule(Arrays.asList(new TimelineModule.Item(new RtbImageAd())));
        TimelineModule mixed = new TimelineModule(Arrays.asList(
                new TimelineModule.Item(new RtbImageAd()), new TimelineModule.Item(post)));
        TimelineModule thread = new TimelineModule(Arrays.asList(new TimelineModule.Item(post)));
        assertTrue(RtbAdFilter.isRtbOnlyModule(adModule));
        assertFalse(RtbAdFilter.isRtbOnlyModule(mixed));
        assertFalse(RtbAdFilter.isRtbOnlyModule(thread));
        assertEquals(Arrays.asList(post, mixed, thread),
                RtbAdFilter.withoutRtbAds(Arrays.asList(post, adModule, mixed, thread), List.class));
    }
}
