"""Checks a hand-written Brainfuck source. It never changes or emits code.

House style of brainfuck/src/*.bf (docs/BRAINFUCK_ABI.md, "Sources"):

- `#` starts a comment that runs to the end of the line. A comment may not
  contain any of the eight commands, so prose can never turn into code.
- Outside comments a line holds only commands, whitespace and checkpoints.
- `%cell NAME ADDRESS [SIZE]` in a comment names tape cells.
- `@NAME`, `@NAME:K` (byte K of NAME) or `@ADDRESS` in code asserts where the
  data pointer is at that point.
- `~N` in code asserts that the run of one command (`+`, `-`, `>` or `<`)
  just before it on the same line, spaces allowed inside the run, is exactly
  N long, so long constants and moves can be checked.
- Every loop leaves the pointer where it found it, so the pointer position
  is known everywhere and every checkpoint can be verified statically.

Usage: python -m tools.bftool.lint brainfuck/src/core.bf [--tape N]
"""

import re
import sys

COMMANDS = set("+-<>[].,")
RUN = re.compile(r"~([0-9]+)")
CHECKPOINT = re.compile(r"@([A-Za-z_][A-Za-z0-9_]*|[0-9]+)(?::([0-9]+))?")
DECL = re.compile(r"%cell\s+([A-Za-z_][A-Za-z0-9_]*)\s+([0-9]+)(?:\s+([0-9]+))?")


class LintError(Exception):
    pass


class Source:
    """A parsed source: the code (commands only) and its cell map."""

    def __init__(self, code, cells, high_water):
        self.code = code
        self.cells = cells              # name -> (address, size), declaration order
        self.high_water = high_water    # highest address the pointer reaches


def lint(text, tape_size=None, name="<source>"):
    errors = []
    cells = {}
    # First pass: declarations, so a checkpoint may name a cell declared later.
    for number, line in enumerate(text.splitlines(), 1):
        _, _, comment = line.partition("#")
        for m in DECL.finditer(comment):
            cell, address, size = m.group(1), int(m.group(2)), int(m.group(3) or 1)
            if cell in cells:
                errors.append("%s:%d: cell %s declared twice" % (name, number, cell))
            cells[cell] = (address, size)
    code = []
    pos = 0
    high = 0
    stack = []
    run_char, run_len = None, 0
    for number, line in enumerate(text.splitlines(), 1):
        body, hash_, comment = line.partition("#")
        run_char, run_len = None, 0
        bad = sorted(set(comment) & COMMANDS)
        if hash_ and bad:
            errors.append("%s:%d: command characters in a comment: %s" % (name, number, " ".join(bad)))
        i = 0
        while i < len(body):
            c = body[i]
            if c in COMMANDS:
                code.append(c)
                if c in "+-<>":
                    run_len = run_len + 1 if c == run_char else 1
                    run_char = c
                else:
                    run_char, run_len = None, 0
                if c == ">":
                    pos += 1
                    high = max(high, pos)
                    if tape_size is not None and pos >= tape_size:
                        errors.append("%s:%d: pointer leaves the %d-cell tape" % (name, number, tape_size))
                elif c == "<":
                    pos -= 1
                    if pos < 0:
                        errors.append("%s:%d: pointer below cell 0" % (name, number))
                elif c == "[":
                    stack.append((pos, number))
                elif c == "]":
                    if not stack:
                        errors.append("%s:%d: unmatched ]" % (name, number))
                    else:
                        start, opened = stack.pop()
                        if start != pos:
                            errors.append("%s:%d: loop opened on line %d at cell %d closes at cell %d"
                                          % (name, number, opened, start, pos))
                            pos = start
                i += 1
            elif c.isspace():
                i += 1
            elif c == "~":
                m = RUN.match(body, i)
                if not m:
                    errors.append("%s:%d: malformed run assertion" % (name, number))
                    i += 1
                    continue
                if int(m.group(1)) != run_len:
                    errors.append("%s:%d: run of %d %s, assertion %s"
                                  % (name, number, run_len, run_char or "commands", m.group(0)))
                run_char, run_len = None, 0
                i = m.end()
            elif c == "@":
                m = CHECKPOINT.match(body, i)
                if not m:
                    errors.append("%s:%d: malformed checkpoint" % (name, number))
                    i += 1
                    continue
                target, offset = m.group(1), int(m.group(2) or 0)
                run_char, run_len = None, 0
                if target.isdigit():
                    expected = int(target) + offset
                elif target in cells:
                    address, size = cells[target]
                    if offset >= size:
                        errors.append("%s:%d: %s has no byte %d" % (name, number, target, offset))
                    expected = address + offset
                else:
                    errors.append("%s:%d: unknown cell %s" % (name, number, target))
                    expected = pos
                if expected != pos:
                    errors.append("%s:%d: pointer is at cell %d, checkpoint %s expects %d"
                                  % (name, number, pos, m.group(0), expected))
                i = m.end()
            else:
                errors.append("%s:%d: unexpected character %r outside a comment" % (name, number, c))
                i += 1
    for start, opened in stack:
        errors.append("%s:%d: unmatched [" % (name, opened))
    if errors:
        raise LintError("\n".join(errors))
    return Source("".join(code), cells, high)


def main(argv=None):
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument("paths", nargs="+")
    parser.add_argument("--tape", type=int)
    args = parser.parse_args(argv)
    failed = False
    for path in args.paths:
        with open(path, encoding="utf-8") as f:
            text = f.read()
        try:
            src = lint(text, args.tape, path)
            print("%s: ok, %d commands, high-water cell %d" % (path, len(src.code), src.high_water))
        except LintError as e:
            print(e)
            failed = True
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
