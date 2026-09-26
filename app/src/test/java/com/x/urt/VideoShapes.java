package com.x.urt;

import java.util.List;

import kotlinx.collections.immutable.ImmutableListMarker;

/** Video Tab batch-copy method shapes for the resolver parity tests. */
public final class VideoShapes {
    private VideoShapes() {
    }

    public static VideoState a(VideoState state, ImmutableListMarker<Object> items, boolean refresh, int page,
                               Object cursor) {
        return state;
    }

    static VideoState copy(VideoState state, List<Object> items, boolean refresh, int page) {
        return state;
    }

    public static VideoState b(VideoState state, ImmutableListMarker<Object> items, int page, boolean refresh,
                               Object a, Object b, Object c, Object d) {
        return state;
    }

    public static VideoState tooMany(VideoState state, List<Object> items, int page, boolean refresh,
                                     Object a, Object b, Object c, Object d, Object e) {
        return state;
    }

    public static VideoState noInt(VideoState state, List<Object> items, boolean refresh, Object page) {
        return state;
    }

    public static VideoState flagsInList(VideoState state, List<Object> items, Object a, Object b) {
        return state;
    }

    public static Object otherReturn(VideoState state, List<Object> items, boolean refresh, int page) {
        return state;
    }

    public VideoState instance(VideoState state, List<Object> items, boolean refresh, int page) {
        return state;
    }

    public static VideoState c(VideoState state, String items, boolean refresh, int page) {
        return state;
    }
}
