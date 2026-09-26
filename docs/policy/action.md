# action.bf — promoted post-action names

Normative specification of `brainfuck/src/action.bf` (program id 1), used by
the action-graph fallback (`PromotedActionScanner`). The graph walk itself
(reflection, depth 5, 160 objects, 24 items per collection) stays in Java;
every scalar it meets is sent here in batches.

## OP_ACTION_NAMES (0x20)

Request payload:

```text
u8 n (1..255; the host sends larger walks in several requests)
per value:
  u8 isEnum          the value is an enum constant (text = name())
  u8 isActionClass   the value's class is com.x.models.PostActionType
  chunks of action-alphabet codes (text = name() or String.valueOf(value)), 0 ends
```

Action alphabet (case sensitive, `AC_*`): 0 any other char, 1 `#`, 2–17 the
letters `P r o m t e d D i s A I n f R p` that occur in the three words.

Response payload (n + 1 bytes): per value a match code
(0 none, 1 `PromotedDismissAd`, 2 `PromotedAdsInfo`, 3 `PromotedReportAd`),
then `any` (1 when some value matched).

Rules per value:

1. **Exact**: the text after the last `#` (the whole text when there is no
   `#`, when the `#` is the last char, or when isEnum) equals one of the three
   words.
2. **Suffix**, only when rule 1 did not match and isActionClass: the whole text
   ends with one of the words.

Implementation: one trie of the three words (states 0–32, word ends 17, 24,
32). `TS` follows the exact rule (a `#` restarts it unless isEnum; a mismatch
parks it in the dead state 99). `SS` follows every suffix (on a mismatch it
restarts at 1 for `P`, else 0; no word contains an inner `P`).
