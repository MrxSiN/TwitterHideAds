"""The shipped policy programs: reference interpreter vs IR vs host-compiled
AOT C, known vectors, randomized boundary facts, malformed frames and the
execution budget. Parity with the legacy Java policy is PolicyParityTest."""

import os
import random
import struct
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.dirname(__file__))
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "..", "tools"))

import frames  # noqa: E402
from bftool import gen, hostcc, ir, ref, replay  # noqa: E402

C = frames.C
POST, ACTION, DISCOVERY = C["PROG_POST"], C["PROG_ACTION"], C["PROG_DISCOVERY"]
TAPES = {p["id"]: p["tape"] for p in gen.load_manifest()["programs"]}
NAMES = {p["id"]: p["name"] for p in gen.load_manifest()["programs"]}

# Tokens that exercise every branch of the entry grammar.
TOKENS = ["promoted", "tweet", "promote", "tweets", "prom", "twee", "conversationthread", "search",
          "conversation", "trend", "x", "p", "t", "1", "42", "2102692433305718851", "6707f8b5db57b19e",
          "a_b", "_", "", "PROMOTED", "Tweet", "not", "un", "deadbeef", "cursor", "top"]
NOISE = "-_ .#aZ0\téK"


def grammar_entry(rng):
    """Near-valid identifiers: module prefixes, a promoted or organic core and a trailer."""
    word = lambda: rng.choice(["conversationthread", "search", "conversation", "x", "promoted", "tweet", "abc"])
    digits = lambda: str(rng.randint(0, 10 ** rng.randint(1, 19)))
    parts = []
    for _ in range(rng.randint(0, 2)):
        parts += [word() for _ in range(rng.randint(1, 2))] + [digits()]
    if rng.random() < 0.5:
        parts += ["promoted"] + [rng.choice(["tweet", "trend", "x"]) for _ in range(rng.randint(1, 2))]
    else:
        parts += ["tweet"]
    parts.append(digits())
    for _ in range(rng.randint(0, 3)):
        parts.append(rng.choice(["abc", "6707f8b5db57b19e", "1", "a_b", "_"]))
    text = "-".join(parts)
    if rng.random() < 0.3:
        text = text.upper() if rng.random() < 0.3 else text
    if rng.random() < 0.4:
        i = rng.randint(0, len(text))
        op = rng.randrange(3)
        ch = rng.choice(NOISE + "p-1")
        text = text[:i] + ch + text[i:] if op == 0 else (text[:i] + ch + text[i + 1:] if op == 1 else text[:i] + text[i + 1:])
    return text


def random_entry(rng):
    if rng.random() < 0.5:
        return grammar_entry(rng)
    if rng.random() < 0.1:
        return "".join(rng.choice("promtedw-_0123aZ") for _ in range(rng.randint(0, 12)))
    parts = [rng.choice(TOKENS) for _ in range(rng.randint(1, 8))]
    text = "-".join(parts)
    if rng.random() < 0.2:
        i = rng.randint(0, len(text))
        text = text[:i] + rng.choice(NOISE) + text[i:]
    if rng.random() < 0.05:
        text = text * rng.randint(20, 40)  # long values cross the 256 limit and chunk borders
    return text


class ProgramsTest(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        try:
            hostcc.compiler()
        except hostcc.NoCompiler as e:
            raise unittest.SkipTest(str(e))
        cls.dir = tempfile.mkdtemp()
        exe = os.path.join(cls.dir, "replay.exe" if os.name == "nt" else "replay")
        cls.binary = replay.build_harness(gen.C_OUT, exe)
        cls.codes = {pid: gen.program_code(name) for pid, name in NAMES.items()}
        cls.ops = {pid: ir.compile_bf(code, zero_from=0) for pid, code in cls.codes.items()}

    def aot(self, cases):
        return replay.run_aot(self.binary, cases)

    def aot_ok(self, program, requests):
        results = self.aot([(program, r) for r in requests])
        for (status, _, ticks), req in zip(results, requests):
            self.assertEqual(0, status, req[:16])
            budget = C["BUDGET_BASE"] + len(req) * C["BUDGET_PER_BYTE"]
            self.assertLess(ticks, budget // 4, "a policy request should stay far below its budget")
        return [r[1] for r in results]

    # ------------------------------------------------------------ backends agree

    def test_reference_ir_and_aot_agree(self):
        rng = random.Random(7)
        requests = []
        for _ in range(12):
            requests.append((POST, frames.classify_post([random_entry(rng)[:40] for _ in range(rng.randint(0, 3))],
                                                        *[rng.random() < 0.5 for _ in range(3)])))
            requests.append((ACTION, frames.action_names([("x#PromotedAdsInfo", False, False),
                                                          ("ReportPromotedReportAd", False, True)])))
            requests.append((DISCOVERY, frames.render_boundary([1, 0, 0, 0, 1, rng.randint(0, 1), 1, 0],
                                                               [4, 2, 3, 1, 5])))
        requests.append((POST, frames.video_batch(5, [(False, None, ["tweet-1"]), None,
                                                      (True, "promoted-tweet-2", [])])))
        requests.append((DISCOVERY, frames.profile("12.8.0-release")))
        requests.append((DISCOVERY, frames.frame(C["OP_LIMITS"], b"")))
        aot = self.aot(requests)
        for (program, req), (status, out, _) in zip(requests, aot):
            self.assertEqual(0, status)
            expected = ref.run(self.codes[program], req, tape=bytearray(TAPES[program]))
            self.assertEqual(expected, ir.execute(self.ops[program], req, tape_size=TAPES[program]))
            self.assertEqual(expected, out)

    # ------------------------------------------------------------ post.bf

    def test_known_vectors(self):
        promoted = ["promoted-tweet-2102692433305718851-6707f8b5db57b19e", "PROMOTED-TWEET-1-ABC",
                    "conversationthread-2102634587474268204-promoted-tweet-2094154228436742320-6707349df11a283f",
                    "search-conversation-17-promoted-tweet-42-deadbeef", "promoted-trend-12"]
        organic = ["tweet-2102692236387627106", "search-conversation-1-tweet-2", "Tweet-1"]
        neither = ["", "promoted", "promoted-deal", "promoted-tweet-", "not-promoted-tweet-1",
                   "xpromoted-tweet-1", "-promoted-tweet-1", "conversationthread-promoted-tweet-1-a",
                   "promoted-tweet-1 ", "I got promoted-tweet-1 today", "retweet-1", "cursor-top-1"]
        for values, verdict in ((promoted, C["V_PROMOTED"]), (organic, C["V_ORGANIC"]),
                                (neither, None)):
            outs = self.aot_ok(POST, [frames.classify_post([v]) for v in values])
            for v, out in zip(values, outs):
                got = frames.parse(out, C["OP_CLASSIFY_POST"], 3)[0]
                if verdict is None:
                    self.assertNotIn(got, (C["V_PROMOTED"], C["V_ORGANIC"]), v)
                else:
                    self.assertEqual(verdict, got, v)

    def test_video_decide(self):
        rng = random.Random(5)
        copies = [(rng.randint(0, 600), rng.randint(0, 600), rng.randint(0, 128)) for _ in range(3000)]
        copies += [(5, 4, 3), (5, 5, 3), (5, 2, 3), (300, 299, 128)]
        outs = self.aot_ok(POST, [frames.video_decide_copy(*c) for c in copies])
        for (size, filtered, normal), out in zip(copies, outs):
            expected = int(size - filtered > 0 and filtered >= normal)
            self.assertEqual([expected], list(frames.parse(out, C["OP_VIDEO_DECIDE"], 1)), (size, filtered, normal))
        verifies = [(rng.randint(0, 300), rng.randint(0, 128), rng.choice([0, 0, 1, 2]), rng.randint(0, 128),
                     rng.randint(0, 300)) for _ in range(3000)]
        verifies += [(4, 3, 0, 3, 4), (260, 3, 0, 3, 4), (4, 3, 0, 2, 4)]
        outs = self.aot_ok(POST, [frames.video_decide_verify(*v) for v in verifies])
        for (filtered, normal, vp, vn, vs), out in zip(verifies, outs):
            expected = int(vp == 0 and vn >= normal and vs == filtered)
            self.assertEqual([expected], list(frames.parse(out, C["OP_VIDEO_DECIDE"], 1)))

    # ------------------------------------------------------------ action.bf

    def test_action_names_max_batch(self):
        values = [("a#PromotedReportAd", False, False)] * 255
        out = self.aot_ok(ACTION, [frames.action_names(values)])[0]
        self.assertEqual([3] * 255 + [1], list(frames.parse(out, C["OP_ACTION_NAMES"], 256)))

    # ------------------------------------------------------------ discovery.bf

    def test_render_boundary_random(self):
        rng = random.Random(8)
        cases = []
        for _ in range(20000):
            flags = [int(rng.random() < p) for p in (0.9, 0.1, 0.1, 0.1, 0.9, 0.5, 0.5, 0.5)]
            roles = [rng.choice([0, 1, 2, 3, 4, 5, 4, 5, 1]) for _ in range(rng.randint(0, 9))]
            if rng.random() < 0.5 and roles:
                roles[0] = C["R_POST"]
            cases.append((flags, roles))
        outs = self.aot_ok(DISCOVERY, [frames.render_boundary(*c) for c in cases])
        for c, out in zip(cases, outs):
            elig, lo, hi, acc = frames.parse(out, C["OP_RENDER_BOUNDARY"], 4)
            e, score = frames.legacy_render(*c)
            self.assertEqual((e, score, int(e and score >= 320)), (elig, lo + 256 * hi, acc), c)

    def test_exact_and_video_boundary_random(self):
        rng = random.Random(9)
        exact = [([int(rng.random() < 0.8) for _ in range(6)],
                  [rng.choice([0, 1, 4, 5]) for _ in range(rng.randint(0, 12))]) for _ in range(10000)]
        outs = self.aot_ok(DISCOVERY, [frames.exact_boundary(*c) for c in exact])
        for c, out in zip(exact, outs):
            self.assertEqual([frames.legacy_exact(*c)], list(frames.parse(out, C["OP_EXACT_BOUNDARY"], 1)), c)
        video = []
        for _ in range(20000):
            flags = [int(rng.random() < p) for p in (0.9, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.9, 0.9, 0.5, 0.9,
                                                     0.9, 0.5, 0.5)]
            video.append((flags, [rng.choice([0, 1, 2]) for _ in range(rng.randint(0, 10))]))
        outs = self.aot_ok(DISCOVERY, [frames.video_boundary(*c) for c in video])
        for c, out in zip(video, outs):
            elig, lo, hi, acc = frames.parse(out, C["OP_VIDEO_BOUNDARY"], 4)
            e, score = frames.legacy_video_boundary(*c)
            self.assertEqual((e, score, int(e and score >= 330)), (elig, lo + 256 * hi, acc), c)

    def test_limits(self):
        out = self.aot_ok(DISCOVERY, [frames.frame(C["OP_LIMITS"], b"")])[0]
        body = frames.parse(out, C["OP_LIMITS"], C["LIMITS_SIZE"])
        self.assertEqual([12, 16, 64, 5, 160, 24, 48, 5], list(body[:8]))
        self.assertEqual((2500, 500), struct.unpack_from("<HH", body, 8))

    # ------------------------------------------------------------ robustness

    def test_bad_version_and_opcode(self):
        for program in (POST, ACTION, DISCOVERY):
            out = self.aot([(program, frames.frame(C["OP_LIMITS"], b"", major=2)),
                            (program, frames.frame(0x7F, b"\x01\x02"))])
            self.assertEqual(C["ST_BAD_VERSION"], out[0][1][3])
            self.assertEqual(C["ST_BAD_OPCODE"], out[1][1][3])
            self.assertEqual(8, len(out[1][1]))

    def test_malformed_frames_terminate(self):
        rng = random.Random(12)
        cases = []
        for _ in range(6000):
            program = rng.choice([POST, ACTION, DISCOVERY])
            op = rng.choice([0x10, 0x11, 0x12, 0x20, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, rng.randrange(256)])
            payload = bytes(rng.randrange(256) for _ in range(rng.randint(0, 600)))
            data = frames.frame(op, payload) if rng.random() < 0.8 else payload
            cases.append((program, data))
        for (program, data), (status, out, ticks) in zip(cases, self.aot(cases)):
            self.assertIn(status, (0, 3), data[:16])  # OK or budget exhausted, never a tape fault
            self.assertLessEqual(len(out), C["RUNTIME_OUT_CAP"])

    def test_truncated_requests_are_answered(self):
        full = frames.classify_post(["promoted-tweet-1-a", "tweet-2"])
        results = self.aot([(POST, full[:n]) for n in range(len(full))])
        for status, out, _ in results:
            self.assertEqual(0, status)

    def test_budget_stops_runaway_input(self):
        # 255 fields of maximal chunks: the budget grows with the request, so it is still answered
        field = struct.pack("<H", 0xFFFF) + (b"\xff" + b"\x04" * 255) * 4 + b"\0"
        req = frames.frame(C["OP_CLASSIFY_POST"], b"\0\1\0\xff" + field * 255)
        status, out, ticks = self.aot([(POST, req)])[0]
        self.assertEqual(0, status)


if __name__ == "__main__":
    unittest.main()
