"""Python mirror of the host request encoding (PolicyFrame.java) and of the
structural boundary rules, whose random fact vectors the Java suite cannot
build from real methods. Parity with the frozen legacy Java policy is
PolicyParityTest."""

import os
import struct
import sys

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "..", "tools"))

from bftool import gen  # noqa: E402

C = gen.abi_constants()

ENTRY_LETTERS = {"p": C["EC_P"], "r": C["EC_R"], "o": C["EC_O"], "m": C["EC_M"],
                 "t": C["EC_T"], "e": C["EC_E"], "d": C["EC_D"], "w": C["EC_W"]}
ACTION_LETTERS = {"P": "AC_UP_P", "r": "AC_R", "o": "AC_O", "m": "AC_M", "t": "AC_T", "e": "AC_E",
                  "d": "AC_D", "D": "AC_UP_D", "i": "AC_I", "s": "AC_S", "A": "AC_UP_A", "I": "AC_UP_I",
                  "n": "AC_N", "f": "AC_F", "R": "AC_UP_R", "p": "AC_P"}


def entry_code(ch):
    if ch == "-":
        return C["EC_HYPHEN"]
    if "0" <= ch <= "9":
        return C["EC_DIGIT"]
    if ch == "_":
        return C["EC_UNDERSCORE"]
    if ch == "K":  # KELVIN SIGN lower-cases to ASCII k
        ch = "k"
    if "A" <= ch <= "Z":
        ch = ch.lower()
    if "a" <= ch <= "z":
        return ENTRY_LETTERS.get(ch, C["EC_LETTER"])
    return C["EC_OTHER"]


def action_code(ch):
    if ch == "#":
        return C["AC_HASH"]
    name = ACTION_LETTERS.get(ch)
    return C[name] if name else C["AC_OTHER"]


def version_code(ch):
    if ch == " ":
        return C["VC_SPACE"]
    if ord(ch) < 0x20:
        return C["VC_CONTROL"]
    if ch == ".":
        return C["VC_DOT"]
    if ch == "-":
        return C["VC_HYPHEN"]
    if ch == "+":
        return C["VC_PLUS"]
    if "0" <= ch <= "9":
        return C["VC_DIGIT_0"] + ord(ch) - 48
    return C["VC_OTHER"]


def chunks(codes):
    out = bytearray()
    for i in range(0, len(codes), 255):
        part = codes[i:i + 255]
        out.append(len(part))
        out += bytes(part)
    out.append(0)
    return bytes(out)


def text_field(value):
    """Java strings are UTF-16; the tests only use BMP characters."""
    n = len(value)
    return struct.pack("<H", min(n, 0xFFFF)) + chunks([entry_code(c) for c in value])


def frame(op, payload, request_id=0x1234, major=C["ABI_MAJOR"]):
    return struct.pack("<BBBBHH", major, C["ABI_MINOR"], op, 0, len(payload) & 0xFFFF, request_id) + payload


def classify_post(fields, validated=False, allow_fallback=True, metadata=False):
    payload = bytes([int(validated), int(allow_fallback), int(metadata), len(fields)])
    payload += b"".join(text_field(f) for f in fields)
    return frame(C["OP_CLASSIFY_POST"], payload)


def parse(response, op, expected_len):
    assert len(response) >= 8, response
    major, minor, rop, status, n, rid = struct.unpack_from("<BBBBHH", response)
    assert (major, rop, status, rid) == (C["ABI_MAJOR"], op | 0x80, C["ST_OK"], 0x1234), response[:8]
    assert n == expected_len == len(response) - 8, (n, expected_len, len(response))
    return response[8:]









def action_names(values):
    """values: [(text, is_enum, is_action_enum_class)]."""
    payload = bytes([len(values)])
    for text, is_enum, is_action in values:
        payload += bytes([int(is_enum), int(is_action)]) + chunks([action_code(c) for c in text])
    return frame(C["OP_ACTION_NAMES"], payload)




def video_batch(size, items):
    """items: None (class cannot hold a post) or (metadata, map_key or None, [fields])."""
    payload = struct.pack("<HB", min(size, 0xFFFF), len(items))
    for item in items:
        if item is None:
            payload += b"\0"
            continue
        meta, key, fields = item
        payload += bytes([1, int(meta), int(key is not None), len(fields)])
        if key is not None:
            payload += text_field(key)
        payload += b"".join(text_field(f) for f in fields)
    return frame(C["OP_VIDEO_BATCH"], payload)




def render_boundary(flags, roles):
    """flags: static, abstract, native, synthetic, void, direct package, model interface, short name."""
    return frame(C["OP_RENDER_BOUNDARY"], bytes(flags) + bytes([len(roles)]) + bytes(roles))


def legacy_render(flags, roles):
    """RenderBoundaryShape.score over normalized facts: (eligible, score)."""
    static, abstract, native, synthetic, void, direct, iface, short = [bool(f) for f in flags]
    if not static or abstract or native or synthetic or not void:
        return 0, 0
    ci = roles.index(C["R_COMPOSER"]) if C["R_COMPOSER"] in roles else -1
    if ci < 1 or roles[0] != C["R_POST"]:
        return 0, 0
    trailing = len(roles) - ci - 1
    if trailing < 1 or trailing > 2 or any(r != C["R_INT"] for r in roles[ci + 1:]):
        return 0, 0
    score = 25 + 25 + 60 + 25 + 80 + (80 if direct else 25) + (15 if iface else 0) + (10 if short else 0)
    between = roles[1:ci]
    score += 35 if C["R_MODIFIER"] in between else 0
    score += 35 if C["R_LAYOUT"] in between else 0
    score += 20 if C["R_POST"] in between else 0
    return 1, score


def exact_boundary(flags, roles):
    """flags: name equal, abstract, native, synthetic, void, first is post."""
    return frame(C["OP_EXACT_BOUNDARY"], bytes(flags) + bytes([len(roles)]) + bytes(roles))


def legacy_exact(flags, roles):
    name, abstract, native, synthetic, void, first = [bool(f) for f in flags]
    if not name or abstract or native or synthetic or not void:
        return 0
    if len(roles) == 0 or len(roles) > 10 or not first:
        return 0
    return int(C["R_COMPOSER"] in roles)


def video_boundary(flags, roles):
    """flags: static abstract native synthetic bridge retVoid retPrimitive firstEqualsReturn
    listLike kotlinx directUrt returnUrt short public."""
    return frame(C["OP_VIDEO_BOUNDARY"], bytes(flags) + bytes([len(roles)]) + bytes(roles))


def legacy_video_boundary(flags, roles):
    (static, abstract, native, synthetic, bridge, rvoid, rprim, feq, listlike, kx, durt, rurt,
     short, public) = [bool(f) for f in flags]
    n = len(roles)
    if (not static or abstract or native or synthetic or bridge or rvoid or rprim or n < 4 or n > 8
            or not feq or not listlike or not durt or not rurt):
        return 0, 0
    has_b = C["VR_BOOLEAN"] in roles[2:]
    has_i = C["VR_INT"] in roles[2:]
    if not has_b or not has_i:
        return 0, 0
    score = 110 + 100 + (95 if kx else 70) + 25 + 25 + (30 if n == 5 else 10)
    score += (10 if short else 0) + (8 if public else 0)
    return 1, score




def profile(name):
    return frame(C["OP_PROFILE"], chunks([version_code(c) for c in name]))




def video_decide_copy(size, filtered, normal):
    return frame(C["OP_VIDEO_DECIDE"], bytes([C["VD_COPY"]]) + struct.pack("<HHB", size, filtered, normal))


def video_decide_verify(filtered, normal, vpromoted, vnormal, vsize):
    return frame(C["OP_VIDEO_DECIDE"], bytes([C["VD_VERIFY"]])
                 + struct.pack("<HBBBH", filtered, normal, vpromoted, vnormal, vsize))
