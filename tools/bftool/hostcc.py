"""Finds and runs a host C compiler for the AOT differential tests.

Linux/macOS: $CC or cc. Windows: MSVC through vcvars64.bat (Visual Studio or
Build Tools). The Android library itself is always built by the NDK.
"""

import functools
import glob
import os
import shutil
import subprocess


class NoCompiler(Exception):
    pass


@functools.lru_cache(maxsize=1)
def _msvc_env():
    roots = [os.environ.get("ProgramFiles(x86)", r"C:\Program Files (x86)"),
             os.environ.get("ProgramFiles", r"C:\Program Files")]
    for root in roots:
        for vcvars in sorted(glob.glob(os.path.join(
                root, "Microsoft Visual Studio", "*", "*", "VC", "Auxiliary", "Build", "vcvars64.bat"))):
            out = subprocess.run('cmd /c ""%s" >nul && set"' % vcvars, capture_output=True,
                                 text=True, shell=True)
            env = {}
            for line in out.stdout.splitlines():
                if "=" in line:
                    k, v = line.split("=", 1)
                    env[k] = v
            if shutil.which("cl", path=env.get("Path") or env.get("PATH")):
                return env
    return None


def compiler():
    """Returns ("msvc", env) or ("cc", path)."""
    if os.name == "nt":
        env = _msvc_env()
        if env is None:
            raise NoCompiler("MSVC (vcvars64.bat) not found")
        return "msvc", env
    cc = os.environ.get("CC") or shutil.which("cc") or shutil.which("gcc") or shutil.which("clang")
    if not cc:
        raise NoCompiler("no C compiler on PATH")
    return "cc", cc


def build(sources, output, include_dirs=(), shared=False, defines=()):
    kind, value = compiler()
    output = os.path.abspath(output)
    os.makedirs(os.path.dirname(output), exist_ok=True)
    if kind == "msvc":
        cl = shutil.which("cl", path=value.get("Path") or value.get("PATH"))
        args = [cl, "/nologo", "/O2", "/W3", "/std:c11", "/utf-8"]
        args += ["/I" + os.path.abspath(d) for d in include_dirs] + ["/D" + d for d in defines]
        args += [os.path.abspath(s) for s in sources]
        if shared:
            args += ["/LD", "/Fe" + output]
        else:
            args += ["/Fe" + output]
        workdir = os.path.dirname(output)
        result = subprocess.run(args, env=value, cwd=workdir, capture_output=True, text=True)
    else:
        args = [value, "-O2", "-std=c11", "-Wall"]
        args += ["-I" + d for d in include_dirs] + ["-D" + d for d in defines]
        args += list(sources) + ["-o", output]
        if shared:
            args += ["-shared", "-fPIC"]
        result = subprocess.run(args, capture_output=True, text=True)
    if result.returncode != 0:
        raise RuntimeError("host compile failed:\n" + result.stdout + result.stderr)
    return output
