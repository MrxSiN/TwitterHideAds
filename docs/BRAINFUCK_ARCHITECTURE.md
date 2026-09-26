# Brainfuck policy architecture

TwitterHideAds uses Brainfuck for its filtering and decision-policy core,
while Android/Xposed integration remains Java/native. Brainfuck decides; Java
and C carry out the decision.

The toolchain and conventions are taken from
[ThreadsHideAds](https://github.com/MrxSiN/ThreadsHideAds) at `main` `358373d`
(tag `v2.0.0`): the source checker (`lint.py`), the optimizer and C emitter
(`ir.py`), the reference interpreter, the generator, the host compiler driver
and replay harness, and the `.bf` house style (`%cell` declarations,
`@checkpoints`, `~N` run assertions, the equality-test idiom). TwitterHideAds
needs much less of it: its programs are stateless and never call back into
Java, so there are no capabilities, handles, nested `CAP_CALL` levels or
persistent cells. Each copied file names its origin; the changes are listed
at the end.

```text
X / Android
    |
libxposed hook (Application.attach guard, render boundaries, Video Tab dataset hook)
    |
Java host: DexKit, reflection, field reads, caches, witness, deoptimization
    |
normalized primitive facts (flags, roles, alphabet codes), one frame
    |
JNI: NativePolicy.nativeRun  (@FastNative, direct buffers, one call)
    |
AOT-compiled Brainfuck policy (libtwitterbf.so)
    |
KEEP / BLOCK / UNKNOWN (and scores, item classes, limits)
    |
Java host performs the actual object/list operation
```

## Why Brainfuck, and where

The goal is a real, auditable policy layer, not a language statistic. The
policy is small, pure and deterministic, which is exactly what a byte-in,
byte-out Brainfuck program can own; everything with side effects stays out.

| In Brainfuck | Program | Spec |
| --- | --- | --- |
| entry identifier grammar (`tweet-`, `promoted-`, nested module prefixes, segment boundaries, 256-char limit) | `post.bf` | [post](policy/post.md) |
| "mentions promoted" ambiguity rule | `post.bf` | [post](policy/post.md) |
| verdict precedence: entry ID, metadata, organic, validated model, fallback | `post.bf` | [post](policy/post.md) |
| Video Tab item class, tally window, safe-mixed-batch rule, copy and verification decisions | `post.bf` | [post](policy/post.md) |
| promoted action names (exact after `#`, suffix for the action enum) | `action.bf` | [action](policy/action.md) |
| render-boundary eligibility, score, threshold 320 | `discovery.bf` | [discovery](policy/discovery.md) |
| exact-profile boundary rule | `discovery.bf` | [discovery](policy/discovery.md) |
| Video resolver eligibility, score, threshold 330, margin 25 | `discovery.bf` | [discovery](policy/discovery.md) |
| known X versions → exact profile | `discovery.bf` | [discovery](policy/discovery.md) |
| discovery and traversal limits | `discovery.bf` | [discovery](policy/discovery.md) |

| Stays in Java or C | Why |
| --- | --- |
| hooks, `Application.attach` guard, unhooking, deoptimization | framework API calls |
| DexKit and `DexFile` scans, class loading, caller lookup | reflection and native discovery |
| field reads, class-name prefix facts (`com.x.urt.items.post.`, `*PromotedMetadata`, Compose type names) | Java objects; reduced to flags and roles before the call |
| text → alphabet codes (case folding, one table lookup per char) | cheapest normalization, measured below |
| action-graph walk (depth, visited set, queue) | reflection over live objects |
| witness state machine | atomic compare-and-set across render threads |
| Kotlin persistent-list rebuilding, argument replacement | Java collections |
| caches (APK fingerprint, SharedPreferences, identity cache), logging, redaction | I/O and Java state |

## ABI (version 1.0)

`brainfuck/constants.txt` is the single source of every number; the generator
exports it to `BfAbi.java` and `cpp/generated/bf_abi.h`.

Frame (request and response):

| Offset | Size | Request | Response |
| --- | --- | --- | --- |
| 0 | 1 | ABI major (1) | ABI major |
| 1 | 1 | ABI minor (0) | ABI minor |
| 2 | 1 | opcode | opcode \| 0x80 |
| 3 | 1 | flags (0) | status: 0 ok, 1 bad version, 2 bad opcode |
| 4 | 2 | payload length LE (informative, mod 65536) | payload length LE |
| 6 | 2 | request id LE | request id echoed |

Programs parse the payload by opcode layout, so a short request simply reads
zeros. Opcodes: `post.bf` 0x10–0x12, `action.bf` 0x20, `discovery.bf`
0x30–0x35; layouts are in the policy specs. Text is sent as chunks
(`u8 k`, k codes, …, `u8 0`) in one of three alphabets.

The host validates every response: runtime status, length exactly
`8 + expected`, major version, opcode echo, status 0, request id, and value
ranges (verdict ≤ 3, entry index inside the field count, known item classes).
Anything else is a failure.

## Runtime and memory semantics

- Cells are unsigned 8-bit and wrap on `+`/`-` (identical on every ABI: the
  generated C uses `uint8_t`).
- Tape: fixed per program (`programs.json`: post 144, action 56, discovery 112
  cells), zeroed before every request, allocated on the calling thread's C
  stack (`BF_TAPE_MAX` 256). The generator only accepts programs whose loops
  are balanced, so every cell address is a compile-time constant checked
  against the tape size; the pointer cannot leave the tape.
- `,` reads the next request byte and yields 0 at the end of the request (EOF
  = 0). Every loop in the programs terminates on a zero, so truncated or
  garbage input ends them.
- `.` appends to the response buffer (1024 bytes); overflow is an error.
- Request size limit 1 MiB (`RUNTIME_IN_CAP`); the host fails open above it.
- Execution budget: every `while` iteration of the generated C charges one
  tick; the budget is `65536 + 64 × request bytes`. Exhausting it returns
  `BF_ERR_LIMIT`. The randomized tests assert real requests use under a
  quarter of their budget.
- No allocation, no global mutable state, no locks in native code. The program
  table is immutable.

## Build pipeline (AOT)

```text
brainfuck/src/*.bf ── lint.py (comments free of commands, @checkpoints, ~N runs, tape bounds)
       │  parse → IR → optimize: runs, clear/transfer/multiply loops, run-once
       │  loops, value and copy propagation, equality tests → if / C switch
       ▼
app/src/main/cpp/generated/bf_programs.generated.c  (+ bf_abi.h, BfAbi.java, MEMORY_MAP.md)
       ▼
NDK clang -O2 -flto, -fvisibility=hidden, -fstack-protector-strong, _FORTIFY_SOURCE=2,
RELRO + BIND_NOW, non-executable stack, --gc-sections, --icf=all  ──►  libtwitterbf.so
```

Generated files are committed, so `./gradlew :app:assembleRelease` needs no
Python. `python tools/bftool/gen.py --check` (Gradle `checkBrainfuck`, part of
`check`, run by CI and before every host test build) fails when they are stale.
Generation is deterministic (byte-identical output, LF line endings enforced
by `.gitattributes`). The library exports only the two JNI entry points.

Optimization flags were measured on the Pixel 8 Pro (release instrumented
benchmark, organic post p50): `-O2` 2.16 µs, `-Os` 3.13 µs, `-O3` 3.54 µs.

Program sizes: post 108 565 BF commands, action 24 616, discovery 55 770; the
arm64 library is 30 KB.

## Threading

`nativeRun` keeps all state on the caller's stack. Java encodes into a
per-thread `PolicyFrame` (heap byte array staged into a direct buffer, a
direct response buffer, and a reference array naming the sent values). A
request started while the thread's frame is busy — a hook re-entered from app
code during field reads — gets a temporary frame
(`PolicyRobustnessTest.reentrantRequestsUseTheirOwnFrame`). Concurrency is
tested with 8 threads × 5000 classifications against sequential results.

## Failure behaviour (fail open)

| Failure | Result |
| --- | --- |
| library missing or ABI mismatch | bootstrap logs `fail-open mode active`; no hook is installed |
| request over 1 MiB, more than 255 fields/items, too many references | request not sent; content kept |
| native error, budget exhausted, bad version/opcode, malformed response | request fails; content kept |
| OP_LIMITS failure | all limits 0: no boundaries, no scans |
| discovery request failure | the method is not a candidate (no hook) |
| unknown verdict, incomplete evidence | UNKNOWN; only PROMOTED blocks |

Failures are counted in `PolicyStats` and logged at counts 1, 2, 3, 4, 8, 16, …

## Performance

Measured before (frozen v2.1.0 Java policy) and after (Brainfuck through JNI)
with `PolicyBenchmarkTest`: 20 000 warm-up and 20 000 timed calls each.

Pixel 8 Pro, Android 17, release build, non-debuggable instrumentation
process (`-PbenchmarkRelease`):

| Case | Legacy p50 / p95 / p99 | Brainfuck p50 / p95 / p99 |
| --- | --- | --- |
| organic post (hot path) | 14.9 / 31.3 / 85.1 µs | 2.4 / 2.5 / 2.6 µs |
| promoted post | 6.1 / 13.8 / 30.1 µs | 1.1 / 1.3 / 1.5 µs |
| nested promoted post (post detail) | 8.3 / 21.0 / 46.7 µs | 1.3 / 1.4 / 1.5 µs |
| fallback, cached model | 0.4 / 5.0 / 7.5 µs | 0.7 / 6.5 / 10.0 µs |
| fallback walk, uncached | 3.1 / 7.4 / 10.2 µs | 3.2 / 7.4 / 11.0 µs |
| boundary score (startup only) | 0.8 / 0.9 / 3.3 µs | 1.4 / 1.4 / 3.5 µs |
| Video Tab batch, 20 items | 274.8 / 395.6 / 621.8 µs | 6.8 / 10.5 / 23.8 µs |
| JNI round trip, empty request | — | 0.45 / 0.49 / 0.49 µs |

Inside the running X process during Home scrolling (release, `-PpolicyTiming`
steady state), the native call alone averaged about 6–7 µs for 185-byte
requests (measured with a temporary histogram: p50 4–8 µs, p99 ≤ 33 µs): between frames the caches are cold,
which the warm microbenchmark does not see. That is roughly 0.2 % of a 16 ms
frame for a screenful of posts.

Host JVM (Windows x86-64, MSVC `/O2` host build; indicative only): organic
0.9 µs vs 2.6 µs legacy, JNI round trip 0.2 µs. Allocation per call on the
JVM: 0.1 B for every Brainfuck hot-path case against 1.5–2.5 KB for the
legacy classifier (regex matchers, lower-cased copies, signal sets); ART does
not expose per-thread allocation counters, so the device column is not
reported. The fallback paths are slightly slower because a Brainfuck verdict
now precedes the cache lookup; they run only for models without an entry ID.

Why the text normalization is in Java: brainfuck has no indexed memory, so
classifying a raw byte into a character class costs a chain of comparisons
per character. One Java table lookup per char reduces each character to a
code the program dispatches on with a single C `switch`. The segment scanner
is skipped once a field can no longer be an identifier and the substring
machine once `promoted` has been seen, which halves the work for tweet text.

## Parity and testing

`app/src/test/java/my/MrxSiN/twitterhideads/legacy/` is the frozen v2.1.0 Java
policy. Totals of the current suite:

| Suite | Cases |
| --- | --- |
| `PolicyParityTest` (old Java result = new Brainfuck result) | 411 162 comparisons on the JVM, 411 324 on the Pixel 8 Pro |
| of which: every BMP char alone and in 3 identifier contexts | 262 144 |
| `tests/compiler` (Python: reference interpreter vs IR vs AOT C, known vectors, random boundary-fact vectors, video decisions, malformed frames) | about 62 000 requests, 15 tests |
| `PolicyRobustnessTest`: random bytes to every program, truncations, determinism | 20 000 + every prefix + 2 000 |
| concurrency: 8 threads × 5 000 classifications | 40 000 |
| invariant: organic content is never blocked without promoted evidence | 20 000 random models |

Parity covers the timeline classifier end to end (verdict, entry ID, signals,
fallback use), the action walk, Video batches (lists, arrays, maps, persistent
vectors, batches over 128 items), filter decisions, video selection, profile
selection, and render/video boundary scores on every declared method of 16
real classes. The whole JVM suite also runs on devices:
`adb shell am instrument -w io.github.mrxsin.twitterhideads.test/androidx.test.runner.AndroidJUnitRunner`
(70 tests pass on arm64).

`-PparityCases=N` scales the randomized counts (CI uses 40 000).

## Supported ABIs

| ABI | Built | Tested |
| --- | --- | --- |
| arm64-v8a | yes | full suite and live X on Pixel 8 Pro (Android 17) |
| x86-64 host (JVM tests, MSVC or cc) | test build | full suite, CI (Linux) and Windows |
| armeabi-v7a, x86, x86_64 | yes | experimental: same generated C, not run on a device |

## Observability

Release builds log initialization, the first 300 unique blocks and a summary
every 50 blocks, each with `bfCalls`, `bfFailures`, `malformed`,
`fallbackScans` and `blocked`. Debug builds, or release builds made with
`-PpolicyTiming`, add average and maximum request time; debug builds also log
every boundary observation. Organic posts are
never logged by release builds.

## Changing a policy safely

1. Edit the normative spec in `docs/policy/` first.
2. Edit the `.bf` source. Keep the house idioms: named cells with `%cell`,
   a `@CELL` checkpoint after moves, `~N` after long runs, the equality test
   `copy s→f via t; f -= v; e = 1; f[ e[-] … f[-] ] e[ … e[-] ]` with `f` below
   `t`, and every scratch cell back at zero. `python tools/bftool/lint.py
   brainfuck/src/post.bf` verifies pointer positions statically.
3. New numbers go into `brainfuck/constants.txt` (append only; a layout change
   bumps `ABI_MAJOR`).
4. `python tools/bftool/gen.py`, then `python -m unittest discover -s
   tests/compiler` and `./gradlew testDebugUnitTest`.
5. A deliberate behaviour change must update the parity test to state the new
   rule; the legacy oracle itself is never edited.

## Changes from the ThreadsHideAds toolchain

- `ref.py` and the IR executor: no capability refill or superseded input.
- `ir.py`: every emitted `while` loop charges `BF_TICK`.
- `gen.py`: stateless programs (no persistent region), output paths and
  package for this project.
- Runtime: `bf_run` over a stack tape replaces the engine with levels,
  capabilities and handles; `@FastNative` JNI over direct buffers.
