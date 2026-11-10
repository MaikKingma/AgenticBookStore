---
name: agent-os-written-files-techniques
description: Proving hand-written framework output (Agent OS product docs and standards) against the pack and code with zero model runs, and Agent OS installer traps
metadata:
  type: reference
---

- **Trace table with needles in both texts**: one TSV row per range of a written file (written lines, source and source
  lines, needles). The checker fails when a needle is missing from the written lines OR the cited source lines, or when a
  content line is in no row. A `LINE` needle (every written line is a substring of the source range) fits verbatim tables.
  Write it red first with an empty trace; keep negative controls per check. Wrapped source prose breaks a needle across
  lines: use needles inside one line.
- **Universal claims need counterexample searches on two trees** (`git archive` of the real tag, built sandbox), each with a
  control (found: five full-application tests not named `*IntegrationTest`).
- **Agent OS installer** (`project-install.sh`) copies only `profiles/<p>/standards/**`, but the default profile ships
  `global/tech-stack.md`, so no standards are installed and `/plan-product` step 4 never finds it. It writes
  `.claude/commands/agent-os/*.md` and `agent-os/standards/index.yml` only; bare `mktemp` ignores TMPDIR on macOS, so run it
  with a `mktemp` shim on PATH. `--commands-only` rebuilds `index.yml` from the files (a zero-token check of a hand-written
  index; stage it with the trailing blank line the builder writes). A clone of the start branch prompts "Existing standards
  folder detected" (answer n or y both leave it unchanged); use a `solution/level-3` clone for the no-prompt run.
- **Command names**: a file in a subdirectory of `.claude/commands/` is `/<subdir>:<name>`, so the installed files are
  `/agent-os:plan-product`; the pack and guide write the short form.
