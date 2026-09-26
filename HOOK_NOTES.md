# Hook Notes

## Policy core (3.0.0)

Hooks, discovery and object handling are unchanged Java; every decision the
hooks act on comes from the Brainfuck programs in `brainfuck/src/`
(`docs/BRAINFUCK_ARCHITECTURE.md`). Per render the hook makes one JNI call
(`OP_CLASSIFY_POST`) with the model's text fields and a metadata flag; only a
`V_PROMOTED` verdict returns without `chain.proceed()`. Validated on X
12.28.0-prod.01 (Pixel 8 Pro, Vector 2.2): a cold DexKit rescan scored through
`discovery.bf` selected the same 9 boundaries with the same scores
(395 to 325) and the same Video Tab method (score 403) as the 2.1.0 Java
scorer.

## Home timeline

The adaptive pre-render resolver hooks every structural Compose boundary above the activation threshold, up to 16, and then witnesses each one: a boundary is confirmed by the first model carrying a timeline entry identifier or a promoted signal, and unhooked after 12 invocations without one.

On X 12.28.0 the Home timeline, post-detail replies and related posts all render through `com.x.mappers.module.a.a` and `.h` with model `com.x.urt.items.post.c5`. The post-detail focal post renders through `com.x.urt.items.post.r1.*`, whose `c5` carries the bare post id (`c5.a = "<id>"`) instead of an entry identifier; those boundaries do not render timeline entries and are expected to be rejected by the witness.

## Entry identifiers across surfaces

The post render boundaries are shared by every surface, so the hooks were already app-wide; classification was not. X 12.23.1 names a promoted entry differently depending on the module that owns it:

```text
Home         promoted-tweet-2094503130247709013-40f9576b8c0ef6a5
Post detail  conversationthread-2094503721761919196-promoted-tweet-2094503721761919196-40fa14b8ce8560f4
Search       search-conversation-<id>-promoted-tweet-<id>-<hash>
```

Normal entries on the same surfaces are `tweet-<id>`, `conversationthread-<id>-tweet-<id>`, `tweetdetailrelatedtweets-<id>-tweet-<id>` and `search-conversation-<id>-tweet-<id>`. The Video Tab uses `promoted-tweet-<id>` without a hash. `post.bf` (`docs/policy/post.md`) matches the whole value against this grammar: zero or more nesting modules that each end in a numeric id, then `tweet-<id>` or `promoted-<type>-<id>`. A confident organic match short-circuits the action-graph fallback. Java only maps each char to a small alphabet code.

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

The score, the 330 threshold and the 25-point margin are in `discovery.bf`
(`OP_VIDEO_BOUNDARY`, `OP_VIDEO_SELECT`); per-item classes, the safe-mixed-batch
rule and the copy/verification checks are in `post.bf` (`OP_VIDEO_BATCH`,
`OP_VIDEO_DECIDE`). On X 12.28.0 batches of 13, 24 and 35 entries were filtered
to 11, 22 and 31 with paging intact.

Runtime enforcement is additionally gated by a `com.x.video.tab.*` caller stack and a mixed batch containing both normal and promoted direct post items.

The original immutable list is never mutated. The copy is built through kotlinx.collections.immutable's own `builder()`/`build()` pair. R8 strips those members from the interface (`kotlinx.collections.immutable.b` declares nothing on X 12.28.0) and keeps them on the implementation classes (`immutableList.a#c()` returns the concrete builder `immutableList.d`), so `PersistentListCopier` resolves the pair by shape, once per runtime class. A runtime type without that shape fails open; no list interface is emulated. The replacement is inspected again and rejected unless promoted count is zero and all normal posts remain.

The caller stack is captured only after the batch is known to be a mixed batch that would be rewritten, so ordinary paging calls never pay for it.
