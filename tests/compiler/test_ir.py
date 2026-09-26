import os
import random
import sys
import unittest

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "..", "tools"))

from bftool import ir, ref  # noqa: E402


def random_program(rng, depth=0, balanced=True):
    parts = []
    for _ in range(rng.randint(1, 8)):
        r = rng.random()
        if r < 0.35:
            parts.append(rng.choice("+-") * rng.randint(1, 4))
        elif r < 0.55:
            k = rng.randint(1, 3)
            parts.append(">" * k + rng.choice("+-") * rng.randint(1, 3) + "<" * k)
        elif r < 0.62:
            parts.append(",")
        elif r < 0.7:
            parts.append(".")
        elif depth < 3:
            body = random_program(rng, depth + 1, balanced)
            # Guarantee termination: every loop decrements its own cell.
            parts.append("[->" + body + "<]")
    return "".join(parts)


class IrTest(unittest.TestCase):

    def assert_same(self, code, data=b""):
        expected_tape = bytearray(64)
        actual_tape = bytearray(64)
        # Start near the middle so relative offsets stay inside the tape.
        prefix = ">" * 16
        expected = ref.run(prefix + code, data, tape=expected_tape, max_steps=2_000_000)
        ops = ir.compile_bf(prefix + code)
        actual = ir.execute(ops, data, tape=actual_tape)
        self.assertEqual(expected, actual, code)
        self.assertEqual(expected_tape, actual_tape, code)
        propagated_tape = bytearray(64)
        propagated = ir.execute(ir.compile_bf(prefix + code, zero_from=0), data, tape=propagated_tape)
        self.assertEqual(expected, propagated, code)
        self.assertEqual(expected_tape, propagated_tape, code)

    def test_idioms(self):
        self.assertEqual([("set", 0, 0)], ir.compile_bf("[-]"))
        self.assertEqual([("mul", 0, 1, 1), ("mul", 0, 2, 2), ("set", 0, 0)],
                         ir.compile_bf("[->+>++<<]"))
        ops = ir.compile_bf(">[<+>[-]]")
        self.assertEqual("if", ops[0][0])
        self.assertTrue(ir.is_static(ir.compile_bf("[->+<]>>[-]<<")))
        self.assertFalse(ir.is_static(ir.compile_bf("[>]")))

    def test_known_programs(self):
        self.assert_same("++++++++[->++++++++<]>+.")
        self.assert_same(",[.,]", b"hello")
        self.assert_same("+++[>+++[>++<-]<-]>>.")
        self.assert_same("+>+>+<<[>]")  # unbalanced scan

    def test_random_differential(self):
        rng = random.Random(1234)
        for _ in range(1500):
            code = random_program(rng)
            data = bytes(rng.randrange(256) for _ in range(8))
            try:
                ref.run(">" * 16 + code, data, tape=bytearray(64), max_steps=200_000)
            except ref.BfError:
                continue  # too slow for the reference interpreter
            self.assert_same(code, data)

    def test_c_emission_is_deterministic(self):
        ops = ir.compile_bf("+[->+<]>.")
        a = ir.emit_c(ops, "demo", 16)
        b = ir.emit_c(ops, "demo", 16)
        self.assertEqual(a, b)
        self.assertIn("p[1] += p[0];", a)
        with self.assertRaises(ValueError):
            ir.emit_c(ir.compile_bf(">" * 20 + "+"), "demo", 16)


if __name__ == "__main__":
    unittest.main()
