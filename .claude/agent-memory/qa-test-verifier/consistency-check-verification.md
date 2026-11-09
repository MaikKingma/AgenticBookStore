---
name: consistency-check-verification
description: Verifying a no-model consistency record and rehearsal checklist (token inventory, glob tests, row byte checks, probe cross-check in the implementer scratchpad)
metadata:
  type: feedback
---

Rule: build your own token inventory before reading the implementer's table, then diff the two; prove each zero-token claim with your own script and a negative control.

**Why:** a table that says "every token" is only as complete as its author's list. The first T3 round missed two tokens that sit in prose, not in backticks ("PostgreSQL in Docker", "React storefront").
**How to apply:**
- Inventory script (`python3 -I`): backtick spans, plain `docs/` paths and glob lines of each staged file with file:line, auto-resolved with `git ls-tree` and `git grep` on the built sandbox; then read the staged text once more for unquoted facts (versions, ports, library, stack words). Control: an in-memory copy with two misspelled tokens must report `grep-files 0` or `ABSENT`.
- Glob test: write your own (regex translator plus git `:(glob)` pathspec, frontmatter read by the script itself). Besides in-script widened globs, run a FILE-level control: `git clone` the sandbox into scratch, edit one rule's `paths`, and expect exit 1.
- Row check script: join the pack prompt lines (strip `> `, one space) and compare with the checklist literal and the guide line; copy the pack scenario block by line range and compare byte for byte; each GIVEN/WHEN/THEN fragment must appear in the checklist table. Typo controls must come out False (a first control that edits a line without the replaced word silently proves nothing: print which index you mutate).
- Cheap claim audit: list the implementer's scratchpad folder (`ls` only). A leftover `auth-probe.sh` and a jsonl under a probe HOME contradicted a record sentence "no transcript exists". Compare file times with the commit time of the decision (D14) before judging; wording defect, not a rerun.
- Evidence files: `diff` them against the raw scratchpad copies and grep two raw lines per file; `rg -c "[^\x00-\x7F]"` with a printf-built control; `grep -rn '<home-prefix>/|<owner-handle>|<scratchpad-prefix>'` with a `~/.claude` positive control.
- Re-verification round: carry old runs over by hash (staged files vs the old sandbox via a script with a DIFF control, untouched evidence files by `shasum -a 256`). When the base ref moved, `git diff --stat old new -- . ":(exclude)<stripped dir>"` empty plus the same diff on the stripped file as positive control proves the stand-in tree is unchanged. A pinned guide branch: `git diff --stat oldsha branch -- guide/docs`.
Related: [[shell-traps]], [[scaffold-verification-techniques]]
