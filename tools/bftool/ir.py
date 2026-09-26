"""Brainfuck -> optimized IR -> C.

IR operations use offsets relative to the current pointer `p`:

    ("add", off, n)         p[off] += n
    ("set", off, v)         p[off] = v
    ("mul", src, dst, k)    p[dst] += p[src] * k
    ("move", n)             p += n
    ("in", off)             p[off] = read()
    ("out", off)            write(p[off])
    ("loop", off, body)     while (p[off]) body      (body has net move 0)
    ("uloop", body)         while (*p) body          (body moves the pointer)
    ("if", off, body)       if (p[off]) body         (body leaves p[off] == 0)

Recognized idioms: operation runs, pointer-motion elimination inside
balanced code, clear loops, transfer/multiply-add loops and run-once loops.
Every rewrite preserves the reference semantics in ref.py; the optimizer tests
check that differentially.

Copied from ThreadsHideAds (tools/bftool/ir.py at 358373d). TwitterHideAds
changes: no capability refill in the IR executor, and every emitted `while`
loop charges the per-request execution budget (BF_TICK), so a policy program
can never spin forever on a render thread.
"""

from .ref import match_brackets, strip


# ---------------------------------------------------------------- parsing

def parse(code):
    code = strip(code)
    match_brackets(code)
    ops, _, _, _ = _parse_seq(code, 0)
    # The final pointer position is unobservable, so a trailing move is dropped.
    return ops


def _parse_seq(code, i):
    """Parses until `]` or end. Returns (ops, next index, pending pointer
    delta, whether the pointer was materialized by a nested unbalanced loop)."""
    ops = []
    delta = 0
    moved = False
    n = len(code)
    while i < n:
        c = code[i]
        if c in "+-":
            ops.append(("add", delta, 1 if c == "+" else -1))
        elif c in "<>":
            delta += 1 if c == ">" else -1
        elif c == ".":
            ops.append(("out", delta))
        elif c == ",":
            ops.append(("in", delta))
        elif c == "[":
            inner, i, inner_delta, inner_moved = _parse_seq(code, i + 1)
            if inner_delta == 0 and not inner_moved:
                ops.append(("loop", delta, _shift(inner, delta)))
            else:
                if delta:
                    ops.append(("move", delta))
                    delta = 0
                if inner_delta:
                    inner = inner + [("move", inner_delta)]
                ops.append(("uloop", inner))
                moved = True
            continue
        elif c == "]":
            return ops, i + 1, delta, moved
        i += 1
    return ops, i, delta, moved


def _shift(ops, d):
    if d == 0:
        return ops
    out = []
    for op in ops:
        k = op[0]
        if k in ("add", "set"):
            out.append((k, op[1] + d, op[2]))
        elif k == "mul":
            out.append(("mul", op[1] + d, op[2] + d, op[3]))
        elif k in ("in", "out"):
            out.append((k, op[1] + d))
        elif k in ("loop", "if"):
            out.append((k, op[1] + d, _shift(op[2], d)))
        else:
            raise ValueError("cannot shift %r" % (op,))
    return out


# ---------------------------------------------------------------- optimizing

def optimize(ops):
    out = []
    for op in ops:
        k = op[0]
        if k == "loop":
            out.append(_optimize_loop(op[1], optimize(op[2])))
        elif k == "uloop":
            out.append(("uloop", optimize(op[1])))
        elif k == "if":
            out.append(("if", op[1], optimize(op[2])))
        else:
            out.append(op)
    return _peephole(out)


def _writes(ops, cell):
    for op in ops:
        k = op[0]
        if k in ("add", "set", "in") and op[1] == cell:
            return True
        if k in ("mul", "mov") and op[2] == cell:
            return True
        if k in ("loop", "if") and _writes(op[2], cell):
            return True
        if k == "ifeq" and (_writes(op[3], cell) or _writes(op[4], cell)):
            return True
        if k in ("uloop", "move"):
            return True
    return False


def _optimize_loop(off, body):
    # Clear, transfer and multiply-add loops: only adds, condition cell -1/+1.
    if body and all(op[0] == "add" for op in body):
        cond = sum(op[2] for op in body if op[1] == off) & 255
        if cond in (1, 255):
            if cond == 1:
                # Counting up from x reaches 0 after 256-x steps; only a pure
                # clear loop is rewritten in that direction.
                if all(op[1] == off for op in body):
                    return ("set", off, 0)
            else:
                others = {}
                for op in body:
                    if op[1] != off:
                        others[op[1]] = (others.get(op[1], 0) + op[2]) & 255
                result = [("mul", off, dst, k) for dst, k in sorted(others.items()) if k]
                result.append(("set", off, 0))
                return ("block", result)
    # Run-once loop: the body ends by clearing the condition cell and does not
    # otherwise write it, so the loop is an `if`.
    if body and body[-1] == ("set", off, 0) and not _writes(body[:-1], off):
        return ("if", off, body[:-1] + [("set", off, 0)])
    return ("loop", off, body)


def _peephole(ops):
    flat = []
    for op in ops:
        if op[0] == "block":
            flat.extend(op[1])
        else:
            flat.append(op)
    out = []
    for op in flat:
        if out:
            prev = out[-1]
            if op[0] == "add" and prev[0] == "add" and prev[1] == op[1]:
                n = (prev[2] + op[2]) & 255
                out[-1] = ("add", op[1], n)
                if n == 0:
                    out.pop()
                continue
            if op[0] == "add" and prev[0] == "set" and prev[1] == op[1]:
                out[-1] = ("set", op[1], (prev[2] + op[2]) & 255)
                continue
            if op[0] == "set" and prev[0] in ("add", "set") and prev[1] == op[1]:
                out[-1] = op
                continue
        if op[0] == "add":
            op = ("add", op[1], op[2] & 255)
            if op[2] == 0:
                continue
        out.append(op)
    return out


def compile_bf(code, zero_from=None):
    """Parses and optimizes. `zero_from`: when given, every cell at or above
    this offset is known to be zero when the program starts (the runtime
    guarantees it for the scratch region), which enables value propagation."""
    ops = optimize(parse(code))
    if zero_from is not None and is_static(ops):
        ops = _peephole(_propagate(ops, _Known(zero_from, {})))
    return ops


# ---------------------------------------------------------------- value propagation


def _touches(ops, cells):
    """True when any op reads or writes one of `cells`."""
    for op in ops:
        k = op[0]
        if k in ("add", "set", "in", "out") and op[1] in cells:
            return True
        if k in ("mul", "mov") and (op[1] in cells or op[2] in cells):
            return True
        if k in ("loop", "if") and (op[1] in cells or _touches(op[2], cells)):
            return True
        if k in ("ifeq",) and (op[1] in cells or _touches(op[3], cells) or _touches(op[4], cells)):
            return True
        if k in ("uloop", "move"):
            return True
    return False


def _match_copy(ops, i, known):
    """copy s -> f through temp t (both known zero beforehand):
    mul(s,f,1) mul(s,t,1) set(s,0) mul(t,s,1) set(t,0)  =>  f := s."""
    if i + 5 > len(ops):
        return None
    a, b, c, d, e = ops[i:i + 5]
    if (a[0] == "mul" and b[0] == "mul" and a[3] == 1 and b[3] == 1 and a[1] == b[1]
            and c == ("set", a[1], 0) and d == ("mul", b[2], a[1], 1)
            and e[0] == "set" and e[1] == b[2]
            and len({a[1], a[2], b[2]}) == 3 and known.get(b[2]) == 0):
        # The peephole may have folded a later `t += x` into the final clear.
        return a[1], a[2], b[2], e[2]
    return None


def _match_ifeq(ops, i, known):
    """The equality test on a copy that the hand-written sources use for case
    switches (docs/BRAINFUCK_ABI.md, "Sources"):
    f := s; [f += n]; e += 1; if f {e = 0; OTHER; f = 0}; if e {THEN; e = 0}
    with f and e zero beforehand  =>  if (s == -n) THEN else OTHER."""
    copy = _match_copy(ops, i, known)
    if copy is None or known.get(copy[1]) != 0:
        return None
    s, f, t, t_after = copy
    if t_after:
        return None
    j = i + 5
    n = 0
    if j < len(ops) and ops[j][0] == "add" and ops[j][1] == f:
        n = ops[j][2]
        j += 1
    if j + 3 > len(ops):
        return None
    inc, iff, ife = ops[j:j + 3]
    if inc[0] != "add" or inc[2] != 1:
        return None
    e = inc[1]
    if e in (s, f) or known.get(e) != 0:
        return None
    if iff[0] != "if" or iff[1] != f or ife[0] != "if" or ife[1] != e:
        return None
    fb, eb = iff[2], ife[2]
    if len(fb) < 2 or fb[0] != ("set", e, 0) or fb[-1] != ("set", f, 0):
        return None
    if not eb or eb[-1] != ("set", e, 0):
        return None
    other, then = fb[1:-1], eb[:-1]
    if _touches(other, {f, e}) or _touches(then, {f, e}):
        return None
    t = ops[i + 1][2]
    return j + 3, s, (-n) & 255, then, other, (f, e, t)

class _Known:
    """Cell values known at a program point: explicit entries, else zero for
    offsets >= zero_from, else unknown (None)."""

    def __init__(self, zero_from, over):
        self.zero_from, self.over = zero_from, over

    def get(self, off):
        if off in self.over:
            return self.over[off]
        if self.zero_from is not None and off >= self.zero_from:
            return 0
        return None

    def put(self, off, value):
        self.over[off] = value

    def copy(self):
        return _Known(self.zero_from, dict(self.over))

    def forget(self, cells):
        for off in cells:
            self.over[off] = None

    def same(self, other):
        keys = set(self.over) | set(other.over)
        return all(self.get(k) == other.get(k) for k in keys)

    @staticmethod
    def merge(a, b):
        out = _Known(a.zero_from, {})
        for off in set(a.over) | set(b.over):
            va, vb = a.get(off), b.get(off)
            out.over[off] = va if va == vb else None
        return out


def _written(ops, acc=None):
    acc = set() if acc is None else acc
    for op in ops:
        k = op[0]
        if k in ("add", "set", "in"):
            acc.add(op[1])
        elif k in ("mul", "mov"):
            acc.add(op[2])
        elif k in ("loop", "if"):
            acc.add(op[1])
            _written(op[2], acc)
        elif k == "ifeq":
            _written(op[3], acc)
            _written(op[4], acc)
    return acc


def _propagate(ops, known):
    out = []
    i = 0
    while i < len(ops):
        m = _match_ifeq(ops, i, known)
        if m is not None:
            nxt, s, value, then, other, temps = m
            sv = known.get(s)
            if sv is not None:
                out.extend(_propagate(then if sv == value else other, known))
            else:
                taken = known.copy()
                taken.put(s, value)
                then_ops = _propagate(then, taken)
                rest = known.copy()
                other_ops = _propagate(other, rest)
                if then_ops or other_ops:
                    out.append(("ifeq", s, value, then_ops, other_ops))
                known.over = _Known.merge(taken, rest).over
            i = nxt
            continue
        c = _match_copy(ops, i, known)
        if c is not None and known.get(c[1]) == 0:
            s, f, t, t_after = c
            sv = known.get(s)
            if sv is None:
                out.append(("mov", s, f, 1, 0))
            elif sv:
                out.append(("set", f, sv))
            known.put(f, sv)
            if t_after:
                out.append(("set", t, t_after))
                known.put(t, t_after)
            i += 5
            continue
        op = ops[i]
        i += 1
        k = op[0]
        if k == "add":
            v = known.get(op[1])
            if v is None:
                out.append(op)
            else:
                v = (v + op[2]) & 255
                out.append(("set", op[1], v))
                known.put(op[1], v)
        elif k == "set":
            if known.get(op[1]) != op[2]:
                out.append(op)
                known.put(op[1], op[2])
        elif k == "mul":
            src, dst, factor = op[1], op[2], op[3]
            sv, dv = known.get(src), known.get(dst)
            if sv is not None:
                delta = (sv * factor) & 255
                if delta:
                    if dv is None:
                        out.append(("add", dst, delta))
                    else:
                        out.append(("set", dst, (dv + delta) & 255))
                        known.put(dst, (dv + delta) & 255)
            elif dv is not None:
                out.append(("mov", src, dst, factor & 255, dv))
                known.put(dst, None)
            else:
                out.append(op)
                known.put(dst, None)
        elif k == "in":
            out.append(op)
            known.put(op[1], None)
        elif k == "out":
            out.append(op)
        elif k == "loop":
            if known.get(op[1]) == 0:
                continue
            # Fixpoint: the state at the loop head is the merge of the state
            # before the loop and the state at the end of every iteration.
            head = known.copy()
            for _ in range(8):
                end = head.copy()
                _propagate(op[2], end)
                merged = _Known.merge(known, end)
                if merged.same(head):
                    break
                head = merged
            else:
                head = known.copy()
                head.forget(_written(op[2]))
            body = _propagate(op[2], head.copy())
            out.append(("loop", op[1], body))
            known.over = head.over
            known.put(op[1], 0)
        elif k == "if":
            v = known.get(op[1])
            if v == 0:
                continue
            taken = known.copy()
            body = _propagate(op[2], taken)
            if v is not None:
                out.extend(body)
                known.over = taken.over
            else:
                out.append(("if", op[1], body))
                known.over = _Known.merge(known, taken).over
            known.put(op[1], 0)
        else:
            raise ValueError("unexpected op in static program: %r" % (op,))
    return out


def is_static(ops):
    """True when the pointer never moves (every loop balanced)."""
    for op in ops:
        if op[0] in ("move", "uloop"):
            return False
        if op[0] in ("loop", "if") and not is_static(op[2]):
            return False
        if op[0] == "ifeq" and not (is_static(op[3]) and is_static(op[4])):
            return False
    return True


def offset_range(ops, lo=0, hi=0):
    for op in ops:
        k = op[0]
        if k in ("add", "set", "in", "out"):
            lo, hi = min(lo, op[1]), max(hi, op[1])
        elif k in ("mul", "mov"):
            lo, hi = min(lo, op[1], op[2]), max(hi, op[1], op[2])
        elif k in ("loop", "if"):
            lo, hi = min(lo, op[1]), max(hi, op[1])
            lo, hi = offset_range(op[2], lo, hi)
        elif k == "ifeq":
            lo, hi = min(lo, op[1]), max(hi, op[1])
            lo, hi = offset_range(op[3], lo, hi)
            lo, hi = offset_range(op[4], lo, hi)
    return lo, hi


def count_ops(ops):
    total = 0
    for op in ops:
        total += 1
        if op[0] in ("loop", "if"):
            total += count_ops(op[2])
        elif op[0] == "ifeq":
            total += count_ops(op[3]) + count_ops(op[4])
        elif op[0] == "uloop":
            total += count_ops(op[1])
    return total


# ---------------------------------------------------------------- IR execution (tests)

class _Io:
    def __init__(self, data):
        self.data = bytes(data)
        self.pos = 0
        self.out = bytearray()

    def write(self, value):
        self.out.append(value)

    def read(self):
        if self.pos < len(self.data):
            self.pos += 1
            return self.data[self.pos - 1]
        return 0


def execute(ops, data=b"", tape=None, tape_size=4096):
    if tape is None:
        tape = bytearray(tape_size)
    io = _Io(data)
    _exec(ops, tape, 0, io)
    return bytes(io.out)


def _exec(ops, t, p, io):
    for op in ops:
        k = op[0]
        if k == "add":
            t[p + op[1]] = (t[p + op[1]] + op[2]) & 255
        elif k == "set":
            t[p + op[1]] = op[2]
        elif k == "mul":
            t[p + op[2]] = (t[p + op[2]] + t[p + op[1]] * op[3]) & 255
        elif k == "mov":
            t[p + op[2]] = (op[4] + t[p + op[1]] * op[3]) & 255
        elif k == "move":
            p += op[1]
            if p < 0 or p >= len(t):
                raise IndexError("pointer out of tape")
        elif k == "in":
            t[p + op[1]] = io.read()
        elif k == "out":
            io.write(t[p + op[1]])
        elif k == "loop":
            while t[p + op[1]]:
                p = _exec(op[2], t, p, io)
        elif k == "if":
            if t[p + op[1]]:
                p = _exec(op[2], t, p, io)
        elif k == "ifeq":
            p = _exec(op[3] if t[p + op[1]] == op[2] else op[4], t, p, io)
        elif k == "uloop":
            while t[p]:
                p = _exec(op[1], t, p, io)
    return p


# ---------------------------------------------------------------- C generation

def emit_c(ops, name, tape_size):
    static = is_static(ops)
    lo, hi = offset_range(ops)
    if static and (lo < 0 or hi >= tape_size):
        raise ValueError("%s addresses cells %d..%d outside tape %d" % (name, lo, hi, tape_size))
    lines = [
        "/* Generated from %s.bf by tools/bftool. Do not edit. */" % name,
        "BF_PROGRAM(%s)" % name,
        "{",
        "    uint8_t *restrict p = t;",
    ]
    _emit_block(ops, lines, 1, static, tape_size)
    lines += ["    return BF_OK;", "}", ""]
    return "\n".join(lines)


def _switch_run(ops, i):
    """Length of the run of `if (p[c] == v)` tests starting at i that can be
    one C switch: same cell, distinct values, no else, bodies leave c alone."""
    first = ops[i]
    if first[0] != "ifeq" or first[4]:
        return 0
    cell = first[1]
    seen = set()
    j = i
    while j < len(ops):
        op = ops[j]
        if op[0] != "ifeq" or op[1] != cell or op[4] or op[2] in seen or _writes(op[3], cell):
            break
        seen.add(op[2])
        j += 1
    return j - i


def _emit_block(ops, lines, depth, static, tape_size):
    pad = "    " * depth
    i = 0
    while i < len(ops):
        run = _switch_run(ops, i)
        if run >= 3:
            lines.append("%sswitch (p[%d]) {" % (pad, ops[i][1]))
            for op in ops[i:i + run]:
                lines.append("%scase %d: {" % (pad, op[2]))
                _emit_block(op[3], lines, depth + 1, static, tape_size)
                lines.append("%s    break;" % pad)
                lines.append(pad + "}")
            lines.append(pad + "}")
            i += run
            continue
        op = ops[i]
        i += 1
        k = op[0]
        if k == "add":
            lines.append("%sp[%d] += %d;" % (pad, op[1], op[2]))
        elif k == "set":
            lines.append("%sp[%d] = %d;" % (pad, op[1], op[2]))
        elif k == "mul":
            if op[3] == 1:
                lines.append("%sp[%d] += p[%d];" % (pad, op[2], op[1]))
            else:
                lines.append("%sp[%d] += (uint8_t)(p[%d] * %d);" % (pad, op[2], op[1], op[3]))
        elif k == "mov":
            if op[3] == 1 and op[4] == 0:
                lines.append("%sp[%d] = p[%d];" % (pad, op[2], op[1]))
            else:
                lines.append("%sp[%d] = (uint8_t)(%d + p[%d] * %d);" % (pad, op[2], op[4], op[1], op[3]))
        elif k == "move":
            lines.append("%sp += %d;" % (pad, op[1]))
            lines.append("%sif (p < t || p >= t + %d) return BF_ERR_TAPE;" % (pad, tape_size))
        elif k == "in":
            lines.append("%sp[%d] = BF_IN(io);" % (pad, op[1]))
        elif k == "out":
            lines.append("%sBF_OUT(io, p[%d]);" % (pad, op[1]))
        elif k in ("loop", "if"):
            keyword = "while" if k == "loop" else "if"
            lines.append("%s%s (p[%d]) {" % (pad, keyword, op[1]))
            if k == "loop":
                lines.append("%s    BF_TICK(io);" % pad)
            _emit_block(op[2], lines, depth + 1, static, tape_size)
            lines.append(pad + "}")
        elif k == "ifeq":
            lines.append("%sif (p[%d] == %d) {" % (pad, op[1], op[2]))
            _emit_block(op[3], lines, depth + 1, static, tape_size)
            if op[4]:
                lines.append(pad + "} else {")
                _emit_block(op[4], lines, depth + 1, static, tape_size)
            lines.append(pad + "}")
        elif k == "uloop":
            lines.append("%swhile (*p) {" % pad)
            lines.append("%s    BF_TICK(io);" % pad)
            _emit_block(op[1], lines, depth + 1, static, tape_size)
            lines.append(pad + "}")
        else:
            raise ValueError(op)
