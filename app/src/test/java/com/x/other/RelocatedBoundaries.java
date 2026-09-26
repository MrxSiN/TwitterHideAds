package com.x.other;

import androidx.compose.runtime.Composer;
import androidx.compose.ui.Modifier;

import com.x.urt.items.post.PostModel;

/** A boundary declared outside the post package, like com.x.jetfuel or com.x.mappers. */
public final class RelocatedBoundaries {
    private RelocatedBoundaries() {
    }

    public static void a(PostModel model, Modifier modifier, Composer composer, int changed) {
    }
}
