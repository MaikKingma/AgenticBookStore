---
name: qa-directive-traps
description: Core traps in verifier directives (common block C0): clean-tree snapshots, path-ban greps, scope arguments, scratch evidence names, grep syntax (macOS, git grep, rg), gate greps hitting test text, .orig copies, timestamps, backslash-u escapes
metadata:
  type: feedback
---

Write these into every qaDirective that touches them. Siblings: [[qa-probe-design-traps]], [[qa-test-infra-guards]], [[qa-warnings-and-logging]], [[qa-archunit-vacuity]], [[qa-final-brief-integrity]], [[qa-request-collection-smokes]], [[qa-bugfix-and-live-runs]]; data witnesses: [[qa-test-data-falsifiers]].

1. **"Clean working tree" before the gate.** Nothing is committed before the gate, so the tree always holds the impl's uncommitted changes. Never ask for an empty `git status`: ask for `git status --porcelain` plus `shasum -a 256` of the probe-touched files, identical before the first probe and after the last restore. Verifiers and impl agents have read-only git: they restore with Edit, `cp` or rm, never checkout or stash (flag plan criteria like "restored with `git checkout`" at Gate 1). The orchestrator's plan.md `pr:` bookkeeping commit breaks "no file outside <scope> changed": write "no product file outside <scope>" (diff minus `agent-os/`, `.claude/`).

2. **Path-reference ban vs `.gitignore`.** The rule "no file references docs/spec-pack, agent-os, .claude, CLAUDE.md" (spec 09 strips them from `main`) collides with the root `.gitignore`, which keeps `.claude/...` entries: every grep gate spells out that exemption. Agent files ride along in the spec PR diff on `factory` (`.claude/agent-memory/**`, `agent-os/specs/**`), so whole-diff greps for qa-final run on product files only (diff minus `.claude/`, `agent-os/`); agent-file hits go to the orchestrator.

3. **Frontmatter timestamps are hand-written** and have run minutes ahead of the real write. Prove "the referee ran after the impl finished" with `stat` mtimes (impl artifact, last product file, Surefire XML) and direct verifiers to do the same.

4. **Scope argument instead of a directive item.** Verifiers skip global greps as "untouched by this diff". Accept only a quoted scope proof plus my own cheap confirmation; direct them to execute every global item literally. A task block naming a subset of the common items ("Greps on the file: C0-5a, 5b") reads as a replacement: write "in addition to all of C0-5".

5. **Verifiers of one session share one scratchpad.** Generic evidence names (`referee.log`, `snap-before.txt`) are overwritten by the next task's verifier. Put "task-prefixed scratch names, cited by absolute path" into C0, prefixed with the spec number too (`s03-t1-qa-...`), also for helper scripts in `.claude/comms/tmp`. In reviews check the evidence file's mtime against the verdict. If the orchestrator's verifier prompt overrides C0-1 with a short prefix, copy the hash baseline into the ledger and ask for the spec-prefixed form next time.
   `rg` is not on PATH inside `bash <script>` runs (exit 127): direct `rg` items to run in the Bash tool itself. Never write `rg -E`/`-niE` (`-E` is `--encoding`: exit 2, empty stdout); alternation needs no flag; "nothing" means exit 1 with the control exit 0, exit 2 is a failed gate.

6. **Grep syntax traps.**
   - Annotation greps match Javadoc (`{@code ...}`): use `grep -nE '^\s*@|^\s*import '` and read the files. A class-file check (`javap -v`, no `Runtime*Annotations`) misses SOURCE-retention annotations.
   - `\s` and `\b` work in macOS `grep -E` but silently match nothing in `git grep -E`: write `[[:space:]]` there (`-P` for `\b`), with a control that bites. Use single quotes (in double quotes `[&\\]` collapses to `[&\]`).
   - macOS `grep` has no `-P`: a script's non-ASCII check is `LC_ALL=C grep -c $'[\x80-\xff]'` with a positive control (a handed-over `grep -P` exited 2 and the dry run read the usage error as "nothing"). Typed directly into the Bash tool `$'...'` arrives unexpanded: use `git grep --no-index -c -P '[^\x00-\x7F]' -- <files>` (also reads ignored files).
   - Log and source greps: no bare numbers (`401` matched Maven metadata and timestamps), no short unanchored words (`topp` matched "stopping"): `-w` on every term, dry-run the literal C0 text on the base log and tree, and make each script grep print `rc=$?` next to its expected rc.

7. **Literal gate greps scan test text too.** A C0 grep that must print nothing over `src` also hits test titles, comments and fixtures (an inline-style fixture and its comment, a package name in a test title, "document title" matching the unescaped dot of `document.title` cost two rework rounds). At strategy time escape dots, put "run the literal gate grep set before handoff; build fixtures that must hold a banned form from string parts" into the implDirective, and list every exempted file:line in the amendment, never "test files".

8. **Rework diffs need the gated bytes on disk.** Snapshots hold only hashes and untracked files have no git copy. Put into C0: "at snap-before, `cp` each fileScope test file to SP as `<prefix>-<File>.orig`; a rework round diffs the current file against that copy".

9. **Backslash-u escapes do not survive tool parameters.** A Java escape for U+2003 typed into a Write or Edit parameter lands on disk as the literal character, a double backslash as two backslashes. Writing the backslash as its own escape (`u005C`, then `u2003`) lands the ASCII form. Read output shows U+2003 as a plain space: only `od -c` or `rg '\x{2003}'` tells the forms apart.
   **How to apply:** state a required form as an imperative in the plan text, not a parenthetical; decided form a code-point constant (`Character.toString(0x2003)`), checked by `rg -n '[^\x00-\x7F]' <file>` printing nothing plus a positive control, and in reviews by `od -c`. Edit cannot match a line holding literal U+2003 in `old_string`: when a plan replaces literal non-ASCII text, name an allowed method (Write of the whole file after Read, or a sanctioned one-line script plus a diff check).

