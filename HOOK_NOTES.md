# Hook Notes

## Home timeline

The existing adaptive pre-render resolver remains unchanged. It resolves a narrow seven-parameter Compose boundary and suppresses only models with direct promoted signals.

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
