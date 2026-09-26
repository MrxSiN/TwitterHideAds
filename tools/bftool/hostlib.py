"""Builds libtwitterbf for the host JVM, so JVM unit tests run the same AOT C,
runtime and JNI glue the APK ships (compiled by the host C compiler instead of
the NDK).

    python tools/bftool/hostlib.py <java.home> <output library>
"""

import os
import sys

if __package__ in (None, ""):
    sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
    __package__ = "bftool"

from . import gen, hostcc  # noqa: E402

CPP = os.path.join(gen.ROOT, "app", "src", "main", "cpp")
SOURCES = [os.path.join(CPP, n) for n in ("bf_engine.c", "bf_jni.c")] + [gen.C_OUT]


def main(argv):
    java_home, output = argv[1], argv[2]
    include = os.path.join(java_home, "include")
    platform = "win32" if os.name == "nt" else ("darwin" if sys.platform == "darwin" else "linux")
    hostcc.build(SOURCES, output, include_dirs=[CPP, include, os.path.join(include, platform)],
                 shared=True)
    print("built " + output)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
