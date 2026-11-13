---
name: repo-conventions
description: Branch model, merge rules, module vocabulary and gate decisions settled for the AgenticBookStore factory runs; verify against docs/spec-pack/orchestrator before relying on them
applies-to:
  - any
metadata:
  type: project
---

Settled by the main session on 2026-11-06 (source: `docs/spec-pack/orchestrator/progress.md` and `sdd-mapping.md`; re-read them, they win):

- Base branch is `factory`, never `main` (`main` is produced by the spec 09 publish script). Spec branch `sdd/<NN-slug>/spec`, task branches `sdd/<NN-slug>/<Tn>`; task PRs squash-merge into the spec branch, the spec PR merges into `factory` with a MERGE COMMIT (`gh pr merge --merge --delete-branch`), no tags by the orchestrator.
- Module vocabulary: `root`, `bookstore`, `distributor` (`frontend` from spec 12, `guide` from spec 11).
- No `.claude/settings.json` allowlist; everything runs in auto mode. A subagent stalling on a permission prompt is a needs-decision, not something to fix.
- The pack spec `docs/spec-pack/specs/<NN>-*.md` is the source of truth; shaping restates it and may regroup tasks but never changes requirements, scenarios, paths, names or versions. Config additions that a scenario needs but the property list omits (spec 01: `management.endpoint.health.show-components=always`) were self-accepted as cheap and reversible, logged and flagged.
- `progress.md` and `roadmap.md` are the main session's; the orchestrator reports the verify duration in its ledger and final response instead of editing them.
- `agent-os/product` is a symlink to `docs/spec-pack/product`; `agent-os/standards/architecture.md` is a pointer standard.

**How to apply:** derive nothing differently; put these into the ledger's conventions section at every launch.
