# Hook Notes

## Home timeline

The existing adaptive pre-render resolver remains unchanged. It resolves a narrow seven-parameter Compose boundary and suppresses only models with direct promoted signals.

## Entry identifiers across surfaces

The post render boundaries are shared by every surface, so the hooks were already app-wide; classification was not. X 12.23.1 names a promoted entry differently depending on the module that owns it:

```text
Home         promoted-tweet-2094503130247709013-40f9576b8c0ef6a5
Post detail  conversationthread-2094503721761919196-promoted-tweet-2094503721761919196-40fa14b8ce8560f4
Search       search-conversation-<id>-promoted-tweet-<id>-<hash>
```

Normal entries on the same surfaces are `tweet-<id>`, `conversationthread-<id>-tweet-<id>` and `search-conversation-<id>-tweet-<id>`. The promoted token therefore appears either at the start of the identifier or immediately after a `-`, and never inside a normal entry, which is what the classifier matches on.

The promoted display location also reaches the model through the scribe association, observed on X 12.23.1 at:

```text
com.x.urt.items.post.b5#n0 -> com.x.cards.impl.unified.n#d
                           -> com.x.scribing.post.a#m = "for_you_promoted"
```

This is not used for classification. A graph walk wide enough to reach it also reaches injected singletons such as `com.x.promoted.u` and `PromotedContentDatabase_Impl`, which are present on normal posts as well, so the entry identifier remains the discriminating signal.

## Video Tab

The diagnostic log for X 12.10.1 identified this upstream structure:

```text
static PagingState copy(
    PagingState original,
    KotlinImmutableList items,
    PagingMutation mutation,
    boolean flag,
    int mask
)
```

The concrete X 12.10.1 reference was:

```text
com.x.urt.o0$d.a(com.x.urt.o0$d, kotlinx.collections.immutable.c,
                 com.x.urt.o0$d$a, boolean, int)
```

The active module does not depend on these obfuscated names. It resolves by structure:

- direct `com.x.urt.*` declaring class;
- static method;
- first parameter equals return type;
- second parameter is Iterable or `kotlinx.collections.immutable.*`;
- boolean and int parameters are present;
- high-confidence winner with cache revalidation.

Runtime enforcement is additionally gated by a `com.x.video.tab.*` caller stack and a mixed batch containing both normal and promoted direct post items.

The original immutable list is never mutated. A builder-based compatible copy is preferred; an interface proxy is a final fallback. The replacement is inspected again and rejected unless promoted count is zero and all normal posts remain.
