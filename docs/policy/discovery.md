# discovery.bf — structural scoring, thresholds, profiles and limits

Normative specification of `brainfuck/src/discovery.bf` (program id 2). These
requests run at initialization (DexKit results, cache verification), not per
render. Booleans are normalized: any non-zero byte is true. Scores are u16
little endian.

## OP_RENDER_BOUNDARY (0x30) — Compose post render boundary

Request: `static abstract native synthetic returnsVoid declaringDirectPost
modelIsInterface shortName` (8 × u8), `u8 n`, then one role per parameter:
0 other, 1 `androidx.compose.runtime.Composer`, 2 `androidx.compose.ui.Modifier*`,
3 `androidx.compose.foundation.layout.*`, 4 `com.x.urt.items.post.*`, 5 `int`.

Eligible when static, not abstract/native/synthetic, returns void, parameter 0
has role 4, a Composer exists at index ≥ 1 (the first one counts), and 1 or 2
parameters follow it, all `int` (`$changed`, optional `$default`).

Score (eligible only): 215 (static 25, void 25, Composer 60, masks 25, model
package 80) + 80 if the declaring class sits directly in `com.x.urt.items.post`
else 25 + 15 model is an interface + 10 name ≤ 2 chars + 35 Modifier + 35
layout scope + 20 post dependency, the last three for roles found between
parameter 0 and the Composer.

Response: `eligible`, `score u16`, `accept` (eligible and score ≥ 320). The
host hooks every accepted boundary, best first, up to the boundary limit.

## OP_EXACT_BOUNDARY (0x31) — exact-profile boundary check

Request: `nameMatches abstract native synthetic returnsVoid firstIsPost`, `u8 n`,
roles (1 Composer, else 0). Accept when the name matches, not abstract, native
or synthetic, returns void, 1 ≤ n ≤ 10, first parameter is the post model and
some parameter is a Composer.

## OP_VIDEO_BOUNDARY (0x32) — Video Tab state-copy method

Request: `static abstract native synthetic bridge returnsVoid returnsPrimitive
firstEqualsReturn secondListLike secondKotlinx declaringDirectUrt returnsUrt
shortName public` (14 × u8), `u8 n`, roles (1 boolean, 2 int, 0 other).

Eligible when static, none of abstract/native/synthetic/bridge/void/primitive
return, 4 ≤ n ≤ 8, first parameter type equals the return type, second is
list-like, declaring class directly in `com.x.urt`, return type in `com.x.urt`,
and parameters from index 2 include a boolean and an int.

Score: 260 (static direct 110, state copy 100, boolean 25, int 25) + 95 kotlinx
immutable list else 70 + 30 when n = 5 else 10 + 10 short name + 8 public.
Response: `eligible`, `score u16`, `accept` (score ≥ 330).

## OP_VIDEO_SELECT (0x33)

Request: `hasCandidate`, `best u16`, `hasSecond`, `second u16`. Decision:
1 none (no candidate), else 2 below (best < 330), else 3 ambiguous (a second
exists and best − second < 25), else 0 accept.

## OP_PROFILE (0x34) — exact compatibility profiles

Request: chunks of version-alphabet codes (0 other, 1 control char < U+0020,
2 space, 3 `.`, 4 `-`, 5 `+`, 6–15 digits). The name is trimmed like
`String.trim()` and matches a profile version `V` when it equals `V` or starts
with `V` followed by `-`, `.`, `+` or space. Table: `12.8.0` → 2
(`x-12.8.0`), `12.7.1` → 1 (`x-12.7.1`), otherwise 0. The class and method
names of each profile stay in `CompatibilityProfile.java`.

## OP_LIMITS (0x35)

No payload. Response (12 bytes): witness window 12, max boundaries 16, max
inlined callers 64, scan depth 5, scan objects 160, scan items per collection
24, stack frames examined 48, candidates logged 5, reflection classes 2500
(u16), Video resolver classes 500 (u16). The host reads it once; if the
request fails every limit is 0 (witness window 1), so no hook is installed.
