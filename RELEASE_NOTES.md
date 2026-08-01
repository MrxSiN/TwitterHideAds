# 1.2.6-test-app-icon

This test build adds a complete custom launcher icon set. Blocking logic is unchanged from the working 1.2.5 video dataset filter test.

# Twitter Hide Ads 1.2.6 Test

This test filters promoted Video Tab entries before the pager and playback session are created.

The video side installs one dynamically resolved URT dataset hook. It does not hook Compose rendering, autoplay, media playback or player objects.

Expected success marker:

```text
Filtered promoted videos before pager creation
```

Verify that video advertisements are skipped entirely, normal videos remain smooth, and no empty page or background ad audio remains.
