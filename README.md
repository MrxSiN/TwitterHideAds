<div align="center">

<img src="docs/icon.svg" width="120" alt="Twitter Hide Ads">

# Twitter Hide Ads

**An Xposed module that removes promoted posts from X, with its ad policy written in Brainfuck. Yes, really.**

Promoted posts are stopped before X draws them, not hidden after.

<br>

[![Release](https://img.shields.io/github/v/release/MrxSiN/TwitterHideAds?include_prereleases&color=1D9BF0&label=release&style=for-the-badge)](https://github.com/MrxSiN/TwitterHideAds/releases)
[![Downloads](https://img.shields.io/github/downloads/MrxSiN/TwitterHideAds/total?color=3DDC84&logo=android&logoColor=fff&style=for-the-badge)](https://github.com/MrxSiN/TwitterHideAds/releases)
[![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=fff&style=for-the-badge)](#requirements)
[![libxposed](https://img.shields.io/badge/libxposed-API%20102-E8A33D?style=for-the-badge)](https://github.com/libxposed/api)

</div>

---

> [!NOTE]
> X renames its obfuscated classes with almost every update. The module finds its targets again by
> shape, caches them per X version and APK, and rescans after an update. When it cannot be sure it
> installs nothing rather than guess. See the [compatibility](#compatibility) table for what has
> actually been tested.

## Why Brainfuck?

Surely nobody would write the decision logic of an ad blocker in Brainfuck.

Brainfuck was selected for its rich ecosystem, mature package manager, excellent Android SDK
bindings, comprehensive type system, first-class coroutine support, and famously pleasant
debugging experience.

Just joking.

The real idea is the one [ThreadsHideAds](https://github.com/MrxSiN/ThreadsHideAds) proved out:
**keep every decision about X out of the Android code.** Whether an entry identifier is promoted,
how entry, metadata and fallback evidence rank, which Video Tab entries may go and when a rebuilt
batch is safe, how a Compose render boundary is recognised and scored — that is the part that has
to change when X changes, and it is the part that tends to leak into hook callbacks and reflection
helpers until nobody can say where the policy lives.

Brainfuck enforces that boundary, because it literally cannot call Android, Xposed, DexKit or JNI.
It reads bytes and writes bytes. Java reduces what it read to a handful of flags, roles and
character codes, sends them in one request, and does exactly what the answer says.
TwitterHideAds uses Brainfuck for its filtering and decision-policy core, while Android/Xposed
integration remains Java/native.

It is not slow either. The three programs are hand-written plain eight-command Brainfuck, compiled
ahead of time to C and then to native code with the NDK. There is no interpreter in the APK, and a
post is classified in about 2 µs on a Pixel 8 Pro — several times faster than the Java it replaced.

|  | |
|---|---|
| 🧹 **Stopped before render** | Promoted posts are declined at the Compose render boundary, so there is no blank gap and no flash of an ad. |
| 🌐 **Every surface** | Home, post detail, search and anything else that renders posts through the same boundaries; the Video Tab is filtered at its data layer. |
| 🔎 **Survives renames** | DexKit finds the render boundaries by structure, cached per X version and APK fingerprint; an X update triggers a rescan. |
| 🧠 **Policy in Brainfuck, compiled** | Every decision is hand-written Brainfuck, compiled ahead of time to native code. One native call per post, no allocations. |
| 🛟 **Fails open** | Anything the module cannot classify is left alone. A failed policy request keeps the post. |
| 🧪 **Proven identical** | The old Java policy is kept as a test oracle; 411 162 comparisons per run check that old and new answers match. |

---

## What it removes

A promoted post is recognised by its **URT entry identifier**, matched whole against a strict
grammar, or by a promoted-metadata model among its fields:

```text
Home         promoted-tweet-<id>-<hash>
Post detail  conversationthread-<id>-promoted-tweet-<id>-<hash>
Search       search-conversation-<id>-promoted-tweet-<id>-<hash>
Video Tab    promoted-tweet-<id>
```

Look-alikes such as `not-promoted-x`, `unpromoted-1`, `promoted-deal` or free text that mentions
"promoted" never count. A model with no recognisable identifier falls back to a bounded walk for the
promoted post actions (`PromotedDismissAd`, `PromotedAdsInfo`, `PromotedReportAd`). Organic posts,
replies and cursor, paging and control entries are never touched.

<details open>
<summary><b>🧱 The two layers</b></summary>
<br>

| Layer | Where | What it does |
|---|---|---|
| **Render boundaries** | the static Compose functions that draw a post, found by DexKit (9 on X 12.28.0) | Classifies the post model before the call; a promoted post returns without `chain.proceed()`. Each boundary is deoptimized so ART cannot run an inlined copy past the hook, and each must prove at runtime that it renders timeline entries or it is unhooked. |
| **Video Tab dataset** | the URT state-copy method that receives the tab's paging batch, found by structure | Rebuilds the Kotlin persistent list without promoted videos through the list's own builder, before the pager is created. Anything it cannot rebuild exactly is left alone. |

</details>

---

## Status

**v3.0.0.** The filtering and decision policy moved from Java into three hand-written Brainfuck
programs, compiled ahead of time. The 2.1.0 Java policy was frozen as an oracle first, and the new
programs answer identically over hundreds of thousands of randomized and exhaustive cases on the JVM
and on an arm64 phone ([`docs/BRAINFUCK_ARCHITECTURE.md`](docs/BRAINFUCK_ARCHITECTURE.md)).

The release version is `3.0.0` (`versionCode 34`).

### Compatibility

Each row is one setup somebody has actually run. If you try another, please open a pull request
adding a row.

| Device | Android | Framework | X | Module | Result | Tester | Date |
|---|---|---|---|---|---|---|---|
| Pixel 8 Pro | 17 | Vector 2.2 (API 102) | 12.28.0-prod.01 | 3.0.0 | 9 boundaries, 45+ promoted posts blocked on Home and post detail, 2–4 promoted videos removed per Video Tab batch, paging intact | @MrxSiN | 2026-09 |
| Pixel 8 Pro | 17 | Vector 2.2 (API 102) | 12.23.1-prod.01 | 2.1.0 | 11 boundaries, promoted posts blocked on Home and post detail | @MrxSiN | 2026-09 |

### Known limits

- **Search**: no promoted search entry was served during the 3.0.0 test session. Search uses the
  same render boundaries and the same identifier grammar, and the tests cover it, but it has not
  been observed on a device for 3.0.0.
- **LSPatch** needs a build that loads libxposed API 102 modules; 3.0.0 has not been tried under it.
- 32-bit and x86 devices ship the native library but have not been run.
- A batch with more than 255 items or a model with more than 255 text fields is left alone.

## Requirements

| | |
|---|---|
| **Android** | 8.0 (API 26) or newer |
| **App** | X (`com.twitter.android`) |
| **Framework** | [Vector](https://github.com/JingMatrix/Vector) or another libxposed API 101+ framework, or [LSPatch](https://github.com/JingMatrix/LSPatch) (see [Known limits](#known-limits)) |
| **Root** | Required by Vector; the module itself asks for none |

Built against the modern [libxposed API](https://github.com/libxposed/api)
(`io.github.libxposed:api`), not the legacy `de.robv.android.xposed` bridge.

## Install

```
1. Install the APK from Releases
2. Enable Twitter Hide Ads in Vector
3. Force-stop X once, then open it
```

The module declares a **static scope** — X only — so there is nothing to pick.

The framework log shows what happened, in lines beginning with `TwitterHideAds:`:

```text
Detected X version=12.28.0-prod.01, ..., policyCore=brainfuck-aot abi=1.0
Initialization complete: resolver=adaptive, boundaries=9, installedHooks=9, enforcement=ACTIVE_ADAPTIVE, ...
Blocked promoted post before Compose: boundary=adaptive-a.h, entryId=promoted-tweet-<n>-<n>, ...
Filtered promoted videos before pager creation: originalSize=24, filteredSize=22, removed=2, ...
```

Release builds redact entry identifiers to their shape, so an exported log carries no post ids.

<details>
<summary><b>Without root: LSPatch</b></summary>
<br>

The module has no root-only calls, so LSPatch can embed it into X:

1. Install the LSPatch manager ([JingMatrix fork](https://github.com/JingMatrix/LSPatch); the
   archived `LSPosed/LSPatch` predates the modern API).
2. Patch X with **Twitter Hide Ads** as an embedded module, base APK together with all splits.
3. Uninstall the store copy of X, then install the patched APKs as a set.

</details>

---

## How it works

```
X / Android
  → libxposed hook (Application.attach guard, render boundaries, Video Tab dataset hook)
  → Java host / DexKit (discovery, reflection, caches, witness, deoptimization)
  → normalized primitive facts: flags, parameter roles, one small code per character
  → JNI (one call)
  → AOT-compiled Brainfuck policy (libtwitterbf.so)
  → KEEP / BLOCK / UNKNOWN (and scores, item classes, limits)
  → Java host performs the actual object/list operation
```

One rule decides where code goes: **if it needs Android, Java, Xposed, DexKit or JNI, Java does
it; if it decides what to do with what Java read, Brainfuck decides.**

<details>
<summary><b>Starting at the right moment</b></summary>
<br>

- Loading into X installs only a small `Application.attach()` guard. Discovery waits until X has its
  real application context and final class loader.
- A validated X version uses an exact profile (which versions qualify is decided by `discovery.bf`).
  Otherwise DexKit lists the app's static `void` methods, keeps those taking a post model first, and
  `discovery.bf` scores each by role: a `Composer`, the compiler's `$changed`/`$default` masks, and
  any `Modifier`, layout-scope or post-dependency parameters. Every boundary scoring 320 or more is
  hooked, up to 16.
- Results are cached by X version and APK fingerprint; an X update forces a rescan. When DexKit
  cannot load, a bounded `DexFile` scan of `com.x.urt`, `com.x.jetfuel` and `com.x.mappers` runs
  instead.
- Boundaries are deoptimized, with their direct callers, and each adaptive boundary is witnessed:
  12 calls without a post-like model and it is unhooked.

</details>

<details>
<summary><b>The Brainfuck core</b></summary>
<br>

| Program | Decides |
|---|---|
| `post` | the entry-identifier grammar (segment boundaries, nested module prefixes, the 256-char limit), the "mentions promoted" ambiguity rule, verdict precedence, Video Tab item classes, the safe-mixed-batch rule and the copy/verification checks |
| `action` | whether a scalar found by the fallback walk names a promoted post action |
| `discovery` | render and Video Tab boundary eligibility, scores and thresholds, the exact-profile version table, discovery and traversal limits |

Each program is a `.bf` file in `brainfuck/src/` with a normative specification in
[`docs/policy/`](docs/policy/). Comments may not contain a command character, `%cell` comments name
tape cells, `@cell` checkpoints assert where the data pointer is, and `~N` asserts the length of a
run; `tools/bftool/lint.py` checks all of them. The optimizer turns the programs into C (clear and
transfer loops, value propagation, equality tests become `switch`), every loop charges an execution
budget, and the NDK builds `libtwitterbf.so`. The tape lives on the caller's stack, so concurrent
renders share nothing. Frame format, opcodes and memory semantics are in
[`docs/BRAINFUCK_ARCHITECTURE.md`](docs/BRAINFUCK_ARCHITECTURE.md).

</details>

<details>
<summary><b>Staying fast</b></summary>
<br>

DexKit and broad reflection run only during initialization. Per render the hook reads the model's
text fields, maps each character to a code with one table lookup, and makes one `@FastNative` call.
On a Pixel 8 Pro (release build):

| Case | Java 2.1.0 | Brainfuck 3.0.0 |
|---|---|---|
| organic post | 14.9 µs | 2.4 µs |
| promoted post | 6.1 µs | 1.1 µs |
| post-detail promoted reply | 8.3 µs | 1.3 µs |
| Video Tab batch, 20 items | 275 µs | 6.8 µs |

The hot path allocates nothing. Method and full percentiles are in
[`docs/BRAINFUCK_ARCHITECTURE.md`](docs/BRAINFUCK_ARCHITECTURE.md#performance).

</details>

---

## Build

```bash
python tools/bftool/gen.py                      # check brainfuck/src, regenerate C, ABI and memory map
python -m unittest discover -s tests/compiler   # optimizer, AOT and randomized program tests
./gradlew :app:testDebugUnitTest                # JVM parity against the frozen 2.1.0 policy
./gradlew :app:assembleRelease
```

The generated files are committed; the Gradle build checks them with `checkBrainfuck` and never
edits them, so building the APK needs no Python. `scripts/check-project.sh` runs the static project
checks.

You need JDK 17+, Android SDK 36 with NDK 28.2.13676358 and CMake 3.22.1, Python 3.10+, and for the
JVM tests a host C compiler (MSVC Build Tools on Windows, gcc or clang elsewhere).

CI checks the generated files, runs the Python and JVM tests and builds the APK with a check of every
native library on every push and pull request. A `v*` tag signs the release APK in a separate job,
the only one that sees the signing secrets, and attaches it to the GitHub Release.

## Design

```
brainfuck/src/      the three hand-written programs: every decision, word and limit
brainfuck/          programs.json, constants.txt (ABI numbers), generated memory map
docs/policy/        the normative specification of each program
tools/bftool/       source checker, optimizer, C emitter, reference interpreter (tests only)
app/src/main/cpp/   runtime, JNI glue and the generated C
PolicyFrame         per-thread request encoding, response validation
ModuleMain          libxposed entry and bootstrap
TwitterAdBlocker    render-boundary hooks, witness, deoptimization
*Resolver, *Source  DexKit and DexFile discovery, caches
VideoDataset*       the Video Tab dataset hook and persistent-list rebuilding
```

The layering, ABI, parity method and measurements are in
[`docs/BRAINFUCK_ARCHITECTURE.md`](docs/BRAINFUCK_ARCHITECTURE.md); X-specific findings are in
[`HOOK_NOTES.md`](HOOK_NOTES.md).

## Troubleshooting

| Problem | Try |
|---|---|
| Promoted posts still appear | Check the log for `enforcement=ACTIVE_ADAPTIVE`. `FAIL_OPEN_NO_BOUNDARY` means no boundary was resolved. |
| `Brainfuck policy core unavailable` | `libtwitterbf.so` could not load for the device ABI. Reinstall the module APK. |
| `Brainfuck policy request failed open` | A request was malformed or too large and the content was kept. Report the logged `op` and `kind`. |
| Hooks install but nothing is blocked | Check `deoptimized=` in the initialization line; `0` with `deoptimizeSupported=false` means the framework interface never attached. |
| `couldn't find "libdexkit.so"` | The APK was built with compressed native libraries. Keep `useLegacyPackaging = false`. |
| Broken after an X update | X may have changed its render boundaries. Report the `TwitterHideAds:` log lines (`DexKit structural query`, `Adaptive boundary`) with the X version. |

## Credits

| Project | Contribution |
|---|---|
| [DexKit](https://github.com/LuckyPray/DexKit) by LuckyPray | Runtime DEX parsing and discovery of obfuscated classes and methods (Apache-2.0). |
| [libxposed API](https://github.com/libxposed/api), [Vector](https://github.com/JingMatrix/Vector), [LSPatch](https://github.com/JingMatrix/LSPatch) | The hooking API, framework and deoptimization the module runs on, and rootless embedding. |
| [ThreadsHideAds](https://github.com/MrxSiN/ThreadsHideAds) | The Brainfuck toolchain (source checker, optimizer, AOT C emitter, reference interpreter) and conventions, adapted from `v2.0.0`. |

## Disclaimer

Not affiliated with, endorsed by or sponsored by X Corp. or Twitter. Provided for educational and
personal use. X updates may break the hooks without notice.
