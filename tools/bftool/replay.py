"""Differential replay: the same inputs through the reference interpreter and
through the host-compiled AOT programs (tests/native/bf_replay.c).

Adapted from ThreadsHideAds (tools/bftool/replay.py at 358373d); TwitterHideAds
programs have no persistent region.
"""

import os
import struct
import subprocess
import tempfile

from . import gen, hostcc, ref

ROOT = gen.ROOT
CPP = os.path.join(ROOT, "app", "src", "main", "cpp")
HARNESS = os.path.join(ROOT, "tests", "native", "bf_replay.c")


def build_harness(c_source_path, output):
    return hostcc.build([HARNESS, os.path.join(CPP, "bf_engine.c"), c_source_path], output,
                        include_dirs=[CPP])


def encode_cases(cases):
    """cases: iterable of (program id, input bytes)."""
    blob = bytearray()
    for program, data in cases:
        blob += struct.pack("<II", program, len(data)) + data
    return bytes(blob)


def run_aot(binary, cases):
    """Returns [(status, output bytes, ticks used)] in case order."""
    with tempfile.TemporaryDirectory() as d:
        src, dst = os.path.join(d, "cases.bin"), os.path.join(d, "results.bin")
        with open(src, "wb") as f:
            f.write(encode_cases(cases))
        subprocess.run([binary, src, dst], check=True)
        with open(dst, "rb") as f:
            blob = f.read()
    results, i = [], 0
    while i < len(blob):
        status, ticks, n = struct.unpack_from("<III", blob, i)
        i += 12
        results.append((status, blob[i:i + n], ticks))
        i += n
    return results


def run_reference(code, tape_size, data):
    return ref.run(code, data, tape=bytearray(tape_size))
