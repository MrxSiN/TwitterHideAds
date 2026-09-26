# post.bf — promoted-post and Video Tab item policy

Normative specification of `brainfuck/src/post.bf` (program id 0). The
Brainfuck program must implement exactly this; the parity tests compare it
with the frozen v2.1.0 Java policy in `app/src/test/.../legacy/`. Framing,
cell semantics and failure handling are in
[`../BRAINFUCK_ARCHITECTURE.md`](../BRAINFUCK_ARCHITECTURE.md).

## Text fields

Every text value is sent as a *text field*:

```text
u16 length (Java char count, saturated at 65535)
chunks: u8 k (1..255) then k alphabet codes, repeated; u8 0 ends the field
```

Entry alphabet (host normalization in `PolicyFrame`, `EC_*` in `constants.txt`):

| Code | Characters |
| --- | --- |
| 0 `EC_OTHER` | anything else (space, punctuation, every non-ASCII char except U+212A) |
| 1 `EC_HYPHEN` | `-` |
| 2 `EC_DIGIT` | `0`–`9` |
| 3 `EC_UNDERSCORE` | `_` |
| 4 `EC_LETTER` | any other ASCII letter, either case; U+212A KELVIN SIGN |
| 5–12 | `p r o m t e d w`, either case |

Case folding matches `String.toLowerCase(Locale.ROOT)`: U+212A is the only
non-ASCII char whose lower case is an ASCII letter (verified for all 65536
BMP chars by `PolicyParityTest.entryGrammarEveryCharacter`).

### Field facts

For one field the program derives:

- **kind**: `PROMOTED` (2), `ORGANIC` (1) or `NONE` (0) by this grammar on the
  whole value, which must be 1–256 chars long:

  ```text
  MODULE   = [a-z]+ ( -[a-z]+ )* -[0-9]+ -        (zero or more)
  PROMOTED = MODULE* promoted -[a-z]+ ( -[a-z]+ )* -[0-9]+ ( -[a-z0-9_]+ )*
  ORGANIC  = MODULE* tweet -[0-9]+ ( -[a-z0-9_]+ )*
  ```

  PROMOTED is tested first. Implemented as a segment scanner (segment state
  `S`: empty, word prefixes of `promoted`/`tweet`, other letters, digits,
  mixed) feeding seven grammar state bits (`B0 BM BP1 BP2 BPA BO1 BOA`) one
  token per hyphen-separated segment. An `EC_OTHER` char or an empty segment
  makes the kind NONE.
- **mentions**: the lower-cased value contains `promoted` anywhere
  (substring machine `MS`, sticky flag `MENT`).

## OP_CLASSIFY_POST (0x10)

Request payload:

```text
u8 validated   1 when the model's class is the exact profile's render model
u8 allowFallback
u8 metadata    1 when an object field holds a *PromotedMetadata model
u8 n           number of text fields (non-null CharSequence fields, declaration order)
n text fields
```

Response payload (3 bytes): `verdict`, `entry index` (0..n-1, 255 none),
`signals` (bit 0 entry, bit 1 metadata).

Rules, in order:

1. Walk the fields in order. The first field of kind PROMOTED sets the entry
   index, entry kind PROMOTED, and ends the walk (later fields are ignored).
   A field of kind ORGANIC sets the entry index only if none is set yet. A
   field of kind NONE that *mentions* `promoted` marks the model ambiguous.
2. `signals.entry` = entry kind is PROMOTED. `signals.metadata` = metadata and
   not `signals.entry`.
3. Verdict:
   - PROMOTED (2) when either signal is set;
   - else ORGANIC (1) when (entry kind ORGANIC and not ambiguous) or validated;
   - else UNKNOWN (0) when not allowFallback;
   - else NEED_FALLBACK (3).

On NEED_FALLBACK the host runs the bounded action-graph walk and asks
`action.bf`; its `any` result makes the verdict PROMOTED, otherwise UNKNOWN.
Only PROMOTED suppresses a post. Any failure keeps the post.

## OP_VIDEO_BATCH (0x11)

Request payload:

```text
u16 container size (saturated)
u8  n items (the host fails open above 255)
per item:
  u8 possiblePost           0: nothing else follows for this item
  u8 metadata               (only when possiblePost != 0)
  u8 hasKey                 1 when a text map key precedes the fields
  u8 fields                 fields + hasKey <= 255
  [key text field] fields × text field
```

Response payload (2n + 4 bytes): per item `class` and `entry index`, then
`items`, `promoted`, `normal`, `safe`.

Per item (rules of `VideoDatasetClassifier.classify`):

1. possiblePost 0 → class 0, index 255.
2. The entry starts as the key if its kind is not NONE (index 254). Each field
   of kind not NONE replaces the entry when no entry is set or the field is
   PROMOTED (index = field position, key excluded).
3. post = entry kind ≠ NONE or metadata; promoted = entry kind PROMOTED or
   metadata; normal = entry kind ORGANIC and not metadata.
   class = post | promoted << 1 | normal << 2 (so 0, 1, 3 or 5).

Tally over the first 128 items only: items += post; promoted += promoted;
normal += normal. `safe` = size ≥ 3 and items ≥ 2 and promoted > 0 and
normal > 0. The host removes exactly the items whose class has the promoted
bit; non-post entries (cursors, paging and control entries) always stay.

## OP_VIDEO_DECIDE (0x12)

```text
stage 0 (copy):   u16 size, u16 filtered, u8 normal     -> 1 when size > filtered and filtered >= normal
stage 1 (verify): u16 filtered, u8 normal, u8 verifiedPromoted, u8 verifiedNormal,
                  u16 verifiedSize                      -> 1 when verifiedPromoted == 0,
                                                           verifiedNormal >= normal and verifiedSize == filtered
other stages                                            -> 0
```

Response payload: 1 byte. 0 means the host keeps the original batch.
