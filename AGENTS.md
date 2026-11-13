# TwitterHideAds: rules for agents

Read `README.md` and `docs/` for the project's architecture, host contracts and build. Add repository-specific rules above the shared block.

<!-- ecosystem-policy v1 (2026-11-13): identical in every MrxSiN first-party repository. Change it everywhere in one coordinated set of PRs, never in one repository alone. -->
## Ecosystem rules (v1)

Shared by MrxSiN's first-party repositories. Any other rule in this file that is stricter or more specific to this repository wins.

- **Who owns what.** XAX owns the semantic graph, verifier, compiler, ABIs, backends, targets and artifacts. BBC owns the Brainfuck frontend, BFX and Brainfuck-specific optimization. XAX-MCP owns MCP transport, tool schemas, sandbox and permissions. Dependencies point one way: consumers -> XAX-MCP / BBC -> XAX.
- **New production logic** is XAX, or Brainfuck + XAX, wherever the toolchains reach the target; this repository's own language rules say where that is. Existing code in other languages is tracked migration debt, not permission to add more. Declarative config, assets, fixtures and licenses are not code to convert.
- **Upstream first.** When a toolchain lacks something this repository needs: reproduce it on the toolchain's latest `main`, search its issues and PRs, then fix a small bounded gap with a PR in the owning repository, or open (or link) an issue for a large one and mark only the affected work blocked-upstream. No consumer-side shims, private toolchain forks or local toolchain patches.
- **Pins.** Build against exact toolchain commits, recorded in one machine-readable place. Check the toolchains' latest commits before nontrivial work; move a pin by reviewed PR once checks pass, and write down why when an older pin stays.
- **No hardcoding** of secrets, personal or machine paths, toolchain locations, obfuscated host names or version-specific offsets. Machine-local paths belong in your own `~/.claude/CLAUDE.md` or `CLAUDE.local.md`, never in the repository. Specification constants (opcodes, ABI layouts, protocol markers, security limits) are fine when named and documented.
- **Design.** SOLID, DRY, KISS, YAGNI, Law of Demeter, Boy Scout Rule and Separation of Concerns, applied with judgment. They never override correctness, safety, verified state or evidence quality.
- **Evidence.** Report only what actually ran: EXECUTED, MEASURED, NOT RUN or BLOCKED, with the reason. Emulator, Wine or qemu runs are not device runs. Never rewrite old evidence; add new.
- **Git.** Work on a branch from the default branch and land it by pull request; the owner reviews and merges. No force-push to the default branch, no history rewrites. No AI attribution in commits, PRs or comments; commits use the repository's git identity. PR titles say what changed in plain words.
- **Security.** Report vulnerabilities privately, never in a public issue. Never print or commit tokens.
<!-- /ecosystem-policy v1 -->
