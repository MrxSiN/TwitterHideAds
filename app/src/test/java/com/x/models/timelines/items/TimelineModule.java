package com.x.models.timelines.items;

import java.util.List;

/** Stand-in for UrtTimelineModule and its UrtTimelineModuleItem wrapper. */
public final class TimelineModule {
    public final List<Item> innerContent;

    public TimelineModule(List<Item> innerContent) {
        this.innerContent = innerContent;
    }

    @Override
    public String toString() {
        return "UrtTimelineModule(innerContent=" + innerContent + ")";
    }

    public static final class Item {
        public final Object item;

        public Item(Object item) {
            this.item = item;
        }

        @Override
        public String toString() {
            return "UrtTimelineModuleItem(item=" + item + ")";
        }
    }
}
