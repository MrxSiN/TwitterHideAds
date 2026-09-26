package com.x.models.timelines.items;

/** Video Tab batch item fixture. */
public final class VideoItem {
    public final String entryId;
    public final Object metadata;

    public VideoItem(String entryId, Object metadata) {
        this.entryId = entryId;
        this.metadata = metadata;
    }

    @Override
    public String toString() {
        return "VideoItem(" + entryId + ")";
    }
}
