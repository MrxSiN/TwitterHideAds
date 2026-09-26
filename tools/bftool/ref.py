"""Test-only Brainfuck reference interpreter.

Semantics (normative for every backend, see docs/BRAINFUCK_ARCHITECTURE.md):
- unsigned 8-bit cells, wrapping on `+` and `-`;
- the tape has a fixed size and moving outside it is an error;
- `,` reads the next input byte, or 0 once input is exhausted;
- `.` appends the current cell to the output.

This interpreter executes the canonical program one command at a time and is
never shipped. Production code is the AOT-compiled C in app/src/main/cpp.
Adapted from ThreadsHideAds (tools/bftool/ref.py at 358373d), without the
capability refill that TwitterHideAds does not use.
"""


class BfError(Exception):
    pass


def match_brackets(code):
    stack, pairs = [], {}
    for i, c in enumerate(code):
        if c == "[":
            stack.append(i)
        elif c == "]":
            if not stack:
                raise BfError("unmatched ] at %d" % i)
            j = stack.pop()
            pairs[i], pairs[j] = j, i
    if stack:
        raise BfError("unmatched [ at %d" % stack[-1])
    return pairs


def strip(code):
    return "".join(c for c in code if c in "+-<>[].,")


def run(code, data=b"", tape=None, tape_size=4096, max_steps=500_000_000):
    code = strip(code)
    pairs = match_brackets(code)
    if tape is None:
        tape = bytearray(tape_size)
    pos = 0
    out = bytearray()
    ptr = pc = steps = 0
    n = len(code)
    size = len(tape)
    while pc < n:
        c = code[pc]
        if c == "+":
            tape[ptr] = (tape[ptr] + 1) & 255
        elif c == "-":
            tape[ptr] = (tape[ptr] - 1) & 255
        elif c == ">":
            ptr += 1
            if ptr >= size:
                raise BfError("tape overflow at pc %d" % pc)
        elif c == "<":
            ptr -= 1
            if ptr < 0:
                raise BfError("tape underflow at pc %d" % pc)
        elif c == "[":
            if tape[ptr] == 0:
                pc = pairs[pc]
        elif c == "]":
            if tape[ptr] != 0:
                pc = pairs[pc]
        elif c == ".":
            out.append(tape[ptr])
        elif c == ",":
            if pos < len(data):
                tape[ptr] = data[pos]
                pos += 1
            else:
                tape[ptr] = 0
        pc += 1
        steps += 1
        if steps > max_steps:
            raise BfError("step limit exceeded")
    return bytes(out)
