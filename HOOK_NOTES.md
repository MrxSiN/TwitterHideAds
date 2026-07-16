# Hook Notes

This document records the validated hook profiles used by Twitter Hide Ads v1.1.0.

## Enforcement model

The module detects the installed X version and selects one exact compatibility profile. Each profile defines:

- the post interface or base model;
- the expected concrete render model;
- one primary `void` Compose render boundary;
- two fallback `void` Compose boundaries.

Only the primary boundary is installed under normal conditions. Fallback boundaries are installed only when the primary method cannot be found.

## Profile table

| X version | Model base | Render model | Primary boundary | Secondary | Tertiary |
|---|---|---|---|---|---|
| 12.7.1 | `a6` | `a6$a` | `c7.e` | `e.a` | `c7.a` |
| 12.8.0 | `w5` | `w5$a` | `d7.e` | `e.a` | `d7.a` |

Suffix versions such as `12.8.0-release.0` match the corresponding profile prefix.

## X 12.8.0 mapping

Runtime diagnostics on X `12.8.0-release.0` identified this post-render sequence:

```text
UrtTimelinePost
  -> com.x.urt.items.post.n4.u(Composer)
  -> com.x.urt.items.post.w5$a
  -> com.x.urt.items.post.d7.e(...)
  -> com.x.urt.items.post.e.a(...)
```

The selected primary boundary is:

```text
public static final void com.x.urt.items.post.d7.e(
    com.x.urt.items.post.w5,
    androidx.compose.ui.Modifier$a,
    com.x.urt.items.post.y5,
    androidx.compose.foundation.layout.u3,
    androidx.compose.runtime.Composer,
    int,
    int
)
```

At method entry, argument 0 resolves to `com.x.urt.items.post.w5$a`. Promoted samples contained:

```text
field a = promoted-tweet-...
field m = com.x.models.TimelinePromotedMetadata
```

Because the boundary returns `void`, the hook can suppress only the promoted item by calling `param.setResult(null)` before the original Compose body runs.

## Classification

The direct classifier checks cached fields on the render model.

Strong signals:

1. A String field, prioritizing obfuscated field `a`, starts with `promoted-`.
2. A direct field contains `com.x.models.TimelinePromotedMetadata`.
3. Obfuscated field `m` contains `TimelinePromotedMetadata` when its declared type is generalized.

Fallback action signals remain available for unfamiliar model shapes:

- `PromotedDismissAd`
- `PromotedAdsInfo`
- `PromotedReportAd`

For validated `a6$a` and `w5$a` models, a normal post returns after direct-field inspection. It does not incur a recursive object-graph scan.

## Boundaries intentionally not used

`com.x.urt.items.post.n4.u(Composer)` returns a `w5` model. Returning `null` there could violate caller assumptions and is therefore not used for enforcement.

Serializer, database, constructor, menu, and promoted-action initialization paths identify ad-related data but do not represent the final post-render boundary. They are not hooked in the release build.

## Runtime policy

- Unsupported X versions fail open.
- Missing boundaries fail open.
- No DEX scanning in the release build.
- No global View hooks.
- No deoptimization.
- No database or learned advertiser list.
- Normal-post logging is disabled.
- The first three unique blocked entries are logged; aggregate counts are logged every 50 block attempts.
