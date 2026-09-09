# Twitter Hide Ads 2.1.0

Promoted posts are now suppressed on every X surface instead of only the Home timeline.

The post render boundaries are shared by all surfaces, so the hooks were already app-wide; classification was not. Only the Home timeline names a promoted entry `promoted-tweet-<id>-<hash>`. Every surface that nests a post inside a module prefixes that module's own entry, so the same advertisement arrives as `conversationthread-<id>-promoted-tweet-<id>-<hash>` in a post detail and as `search-conversation-<id>-promoted-tweet-<id>-<hash>` in search. Those compound identifiers failed the previous prefix match and rendered.

The classifier now matches the `promoted-` token at any `-` segment boundary, and recognises a promoted-metadata field by any runtime class name ending in `PromotedMetadata` rather than only the validated `com.x.models.TimelinePromotedMetadata`, which X has obfuscated away since 12.22.0.

Expected success markers:

```text
enforcement=ACTIVE_ADAPTIVE
Blocked promoted post before Compose
```

Validated on X `12.23.1-prod.01` under Vector 2.2: 11 deoptimized boundaries installed, promoted entries blocked before render on both the Home timeline and post detail, with normal posts, replies and search results unaffected.

Video Tab dataset filtering is unchanged. It installs one dynamically resolved URT dataset hook and does not hook Compose rendering, autoplay, media playback or player objects.
