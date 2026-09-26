package com.x.urt.items.post;

import java.util.List;

/** Randomized parity fixture: several text fields, metadata slots and an action graph. */
public final class WidePost implements PostModel {
    public String a;
    public String b;
    public CharSequence c;
    public String d;
    public String e;
    public Object meta;
    public Object other;
    public List<Object> actions;
    public Nested nested;

    /** A second level for the fallback walk. */
    public static final class Nested {
        public Object value;
        public List<Object> more;
    }
}
