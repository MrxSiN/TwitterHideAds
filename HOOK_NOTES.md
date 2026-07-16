# Hook notes

## Validated target

```text
Package: com.twitter.android
X version: 12.7.1
Compatibility profile: x-12.7.1
Concrete render model: com.x.urt.items.post.a6$a
```

The version guard runs after `android.app.Application.attach(Context)`. Unsupported or unknown X versions do not receive render hooks.

## Primary pre-render boundary

```text
public static final void com.x.urt.items.post.c7.e(
    com.x.urt.items.post.a6,
    androidx.compose.ui.Modifier$a,
    com.x.urt.items.post.c6,
    androidx.compose.foundation.layout.u3,
    androidx.compose.runtime.Composer,
    int,
    int
)
```

The first argument is the post render model. In the validated X release, promoted models were observed as `com.x.urt.items.post.a6$a` before the original Compose body executed.

Replacement behavior:

```java
if (BundledAdPatterns.classifyTimelinePost(param.args[0]).promoted) {
    param.setResult(null);
}
```

Because the target returns `void`, setting a null result skips the original renderer without fabricating an incompatible return object.

## Direct promoted signals

The validated model exposes both of these signals:

```text
field a / entryId = promoted-tweet-<post ID>
field m = com.x.models.TimelinePromotedMetadata
```

The classifier accepts either direct signal:

```text
entryId starts with "promoted-"
OR
runtime field value is exactly com.x.models.TimelinePromotedMetadata
```

Observed normal entries used `tweet-<post ID>` and did not carry `TimelinePromotedMetadata`.

The module does not classify ordinary post text, captions, usernames, or fixed timeline positions.

## Bundled action fallback

Earlier menu-structure probes confirmed that ad post-option models contain:

```text
com.x.models.PostActionType#PromotedDismissAd
com.x.models.PostActionType#PromotedAdsInfo
com.x.models.PostActionType#PromotedReportAd
```

These enum names are retained as a bounded fallback for an unfamiliar render-model class. The known `a6$a` path returns after direct-field inspection, so normal X 12.7.1 posts do not incur a recursive object-graph scan.

## Fallback render boundaries

Fallback hooks are installed only when no primary `c7.e(...)` method matches:

```text
com.x.urt.items.post.e.a(...): void
com.x.urt.items.post.c7.a(...): void
```

Installing all three paths simultaneously was rejected because they are nested in the same render sequence and would classify each normal post repeatedly.

## Method selection requirements

A candidate render boundary must satisfy all of the following:

1. Exact expected method name for the selected compatibility profile.
2. Non-abstract, non-native, non-synthetic method.
3. `void` return type.
4. Between 1 and 10 parameters.
5. First parameter assignable to `com.x.urt.items.post.a6`, or an equivalent `a6*` class when the interface cannot be resolved.
6. At least one `androidx.compose.runtime.Composer` parameter.

No arbitrary method is hooked when these structural requirements are not met.

## Runtime and logging policy

- Primary hook only during normal operation.
- First three distinct promoted entries receive detailed logs.
- Aggregate output is emitted every 50 blocked render attempts.
- Render attempts and unique promoted entry IDs use separate counters.
- Normal posts are not logged.
- Advertiser and post identifiers are not persisted.
- No DexKit scanning, deoptimization, global view hooks, accessibility scanning, or network requests are used.

## Fail-open conditions

The original X behavior remains intact when:

- the installed X version is unsupported;
- package-version detection fails;
- the post interface cannot be resolved and no structurally valid boundary exists;
- hook installation throws an exception;
- the model does not contain a bundled promoted signal.

## App-update maintenance

When a future X update changes the obfuscated path, collect these LSPosed lines first:

```text
Detected X version=...
Render boundary class not found: ...
Installed pre-render boundary [...]
Initialization complete: ...
Hook failed for ...
```

A new compatibility profile should be added only after the promoted model and safe pre-render boundary are confirmed on-device.
