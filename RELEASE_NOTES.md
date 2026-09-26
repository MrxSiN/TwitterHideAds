# Twitter Hide Ads 3.0.0

TwitterHideAds now uses Brainfuck for its filtering and decision-policy core, while Android/Xposed integration remains Java/native.

The decisions — whether an entry identifier is promoted, organic or neither (including nested `conversationthread-…-promoted-tweet-…` and `search-conversation-…` identifiers), how entry, metadata and fallback evidence rank, which Video Tab entries are removed and when a rebuilt batch is accepted, which action names mark a promoted post, how render boundaries and the Video Tab method are scored and accepted, and which X versions have an exact profile — are hand-written Brainfuck programs, compiled ahead of time to native code. Hooks, DexKit discovery, reflection, caching and list rebuilding are unchanged Java.

Behaviour is unchanged: the 2.1.0 Java policy is kept as a test oracle and the parity suite checks that old and new results are identical. Rendering got cheaper: one native call per post, no allocations, and on a Pixel 8 Pro about 2.4 µs per organic post instead of 14.9 µs.

If the native policy core cannot load or a request fails, the module keeps the content (fail open).

Expected success markers:

```text
policyCore=brainfuck-aot abi=1.0
enforcement=ACTIVE_ADAPTIVE
Blocked promoted post before Compose
```

Validated on X `12.28.0-prod.01` under Vector 2.2 (Pixel 8 Pro, Android 17): Home, post detail and Video Tab suppression, organic posts, replies, search and Video Tab paging.
