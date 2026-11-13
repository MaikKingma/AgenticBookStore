---
name: framework-docs-reading-technique
description: How to read pinned-version docs of OpenSpec, Spec Kit and Agent OS exactly, and traps in the install routes
applies-to:
  - agent-os/**
  - docs/spec-pack/**
  - .claude/**
metadata:
  type: reference
---

- Read docs at the pinned tag with `gh api "repos/<org>/<repo>/contents/docs/<file>?ref=vX.Y.Z" --jq ".content | @base64d"` (quote the URL, zsh globs `?`). Without `--jq` the base64 blob is unreadable. WebFetch of github.com pages summarises with a small model and misses detail; use it only for website pages with no repo source (buildermethods.com).
- Spec Kit and OpenSpec docs live in `docs/` of their repos; Agent OS keeps only command files (`commands/agent-os/*.md`) and CHANGELOG in the repo, its docs are on buildermethods.com.
- `npm view @fission-ai/openspec@<ver> version engines` and `gh release view v<ver> --repo Fission-AI/OpenSpec --json assets` verify a pin without installing.
- Trap: `VAR=x curl ... | sh` sets VAR for curl, not sh. Pin the install script as `curl ... | OPENSPEC_VERSION=v1.16.0 sh`.
- Worktree guard: a command that sets `HOME=` is refused; executing a downloaded install script was refused by the auto-mode classifier ("Unauthorized Persistence"). Read scripts via `gh api ... .content | @base64d`, do not retry.
- Claude Code docs: `curl -sSL -o <file> https://code.claude.com/docs/en/<page>.md` returns raw markdown (first line `> ## Documentation Index`); a connect error can happen on one page, so retry that file alone. Put fetches in a script file under `.claude/comms/tmp/` (guard refuses backslash-escaped spaces and compound commands). The CHANGELOG raw file on GitHub main has `## <version>` headings, newest first; find the heading above a line with awk.
- Prior pack records (`docs/spec-pack/level-branches/records/level-4-*.md`) already hold per-branch evidence and findings (PF-T11-*, PF-T12-*, PF-T13-*); grep them before re-deriving.
