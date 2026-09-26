package com.x.urt.items.post;

import java.util.Collections;
import java.util.List;

/**
 * Render model fixture shaped like X's obfuscated post model: a direct entry
 * string, free text, an optional metadata object and a nested action list.
 */
public final class TimelinePost implements PostModel {
    public String a;
    public String text;
    public Object metadata;
    public Menu menu;

    public TimelinePost(String entryId) {
        this.a = entryId;
    }

    public TimelinePost text(String value) {
        this.text = value;
        return this;
    }

    public TimelinePost metadata(Object value) {
        this.metadata = value;
        return this;
    }

    public TimelinePost actions(Object... actions) {
        this.menu = new Menu(java.util.Arrays.asList(actions));
        return this;
    }

    /** Nested holder so the action scanner has to walk the graph. */
    public static final class Menu {
        public final List<Object> actions;

        Menu(List<Object> actions) {
            this.actions = Collections.unmodifiableList(actions);
        }
    }
}
