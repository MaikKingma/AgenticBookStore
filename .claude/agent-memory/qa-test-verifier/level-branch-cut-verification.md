---
name: level-branch-cut-verification
description: Cut-and-pushed level branches - tree equality vs build-level, manifest vs diff, push side effects
metadata:
  type: feedback
---
Rule: compare TREES, never commit shas, and build the reference on a clone of the real `level-0-start` tag.
**Why:** commit shas embed the caller's timestamp, so a rebuilt chain never reproduces the pushed shas. `simulate-level-0.sh <tip>` rebuilds level 0 from the factory tip, and factory keeps moving after `main` is published, so the simulated tree can differ from the published tag (seen: one `.gitignore` line merged in with the guide after the publish). A literal "tree equals build-level on a fresh simulate from the tip" check then fails for all six branches, with the same one-file delta each time.
**How to apply:**
- Run both: (A) simulate from the tip, build-level per step, `git diff-tree -r --name-status <sandbox HEAD^{tree}> <pushed branch tree>` after fetching the pushed branch into the sandbox (expect only the explained file); (B) `git clone <full clone>` then `checkout --detach refs/tags/level-0-start`, `build-level.sh <step> <clone>`, `rev-parse HEAD^{tree}` equals the pushed branch tree, and `HEAD~1^{tree}` equals the parent branch tree (chain check, free).
- Find which factory ref reproduces the tag tree: simulate from the candidate refs (`--repo <worktree> <sha>`, scripts from the tip); the one that gives the tag tree is the explanation. Explain the delta with `git log --full-history -m -S<string> -- <file>` (plain `-S` shows only the first, non-merge commit).
- Manifest vs push: python reads `manifest.tsv` (6 tab fields, skip `#`), decodes `files/`/`patches/` names (`dot-` to `.`, `CLAUDE.staged.md` to `CLAUDE.md`), and compares with `git diff --raw --no-renames -z <parent> <branch>`: add=A, replace/patch=M, new mode = `100`+mode column; assert no extra and no missing paths.
- Push side effects: two `git ls-remote origin` listings (before and after the long runs) with `diff` of the full files; `gh run list --repo <r> --branch <b>` for each new branch (0 runs) with the spec branch as positive control; read the `on:` blocks of every workflow file on the branch (`ci.yml`, `no-agent-config.yml` trigger on main, factory, sdd specs, the tag).
- After the push, prove verbatim pack files and patch bases on the PUBLISHED branches, not on staging: fenced block vs `git show origin/<branch>:<path>`, each patch row's base sha vs `git rev-parse <pushed predecessor>:<target>`. A row that runs a generator needing `claude` on PATH (uvx `specify init`) gets a logging `claude` stub first on a PATH without the user bin folder, so no real Claude can start (D14).
Related: [[scaffold-verification-techniques]], [[shell-traps]], [[final-sweep-checks]]
