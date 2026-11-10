# Level branches staging (spec 10)

This folder holds every file that the six level branches of spec 10 add on top of `level-0-start`, plus the scripts
that build those branches. It exists only on `factory`. The branches themselves are cut later, from the real
`level-0-start`, with `scripts/cut-level-branches.sh`.

Why staged and not committed at the real paths: Claude Code loads `CLAUDE.md`, `.claude/rules/` and `.claude/skills/`
from subfolders as soon as a session touches a file there. A literal copy of the Level 1 rules or the Level 3 skill
would load into every factory session that reads this folder. So every path is encoded (see below). The folder is
inert for `./mvnw clean verify` and CI, and `docs/spec-pack/` is on the spec 09 strip list, so nothing here reaches
`main`.

## Layout

```
docs/spec-pack/level-branches/
  README.md
  .gitattributes            line endings of the staged files
  scripts/
    simulate-level-0.sh     build a stand-in for level-0-start from any factory ref
    apply-level.sh          apply one step to a repository, uncommitted
    build-level.sh          apply a whole chain, one commit per step
    cut-level-branches.sh   create the six branches locally from level-0-start
    lib-steps.sh            step names, chain, messages and branch names (sourced)
  tests/
    test-staging.sh         offline self-test of the four scripts
    test-hooks.sh           offline harness of the Level 3 hooks (stub mvnw and stdin fixtures in tests/hooks-fixtures/)
  level-1/                  one folder per step
    manifest.tsv
    files/                  staged copies of added or replaced files
    patches/                optional: git diff patches of files that already exist on the base
  level-2/
  level-3/
  level-4-openspec/
  level-4-speckit/
  level-4-agentos/
```

## Path encoding rule

A staged path is the target path with two changes, applied to every path component:

- a leading dot becomes `dot-`: `.claude/rules/domain.md` is staged as `dot-claude/rules/domain.md`, `.mcp.json` as
  `dot-mcp.json`, `.githooks/pre-commit` as `dot-githooks/pre-commit`;
- `CLAUDE.md` becomes `CLAUDE.staged.md`.

No staged path component may start with a dot or be named `CLAUDE.md`, `CLAUDE.local.md` or `AGENTS.md`.
`apply-level.sh` decodes the staged path, refuses a row whose decoded path differs from its target column, and
refuses every staged path that breaks the rule. Consequences: a target named `AGENTS.md` or `CLAUDE.local.md` cannot
be staged, and a target component that really starts with `dot-` cannot be staged either (it would decode to a dot).
`CLAUDE.local.md.example` is fine.

## Manifest

`<step>/manifest.tsv` has one row per staged file, six fields separated by exactly one tab, LF line endings. Lines
that start with `#` and empty lines are ignored. Line numbers in error messages count every physical line.

| field | values | meaning |
|---|---|---|
| op | `add`, `replace`, `patch` | `add` creates a file that must not exist; `replace` overwrites a file that must exist; `patch` applies a git diff to a file that must exist |
| group | `config`, `exercise` | `config` is harness configuration; `exercise` is the reference solution of the level exercise (the ACL contract test on level 2, cancel order on level 3) |
| staged | path relative to the step folder | `files/<encoded target>` for `add` and `replace`; `patches/<encoded target>.patch` for `patch` |
| target | path relative to the repository root | where the file lands; never absolute, no `..`, `.` or empty component, no `.git` component |
| mode | `644`, `755` | file mode set on the target after writing (hooks and scripts are `755`) |
| base | 40-hex blob sha for `patch`, `-` otherwise | blob of the target on the chain predecessor tree: `git rev-parse HEAD:<target>` in a sandbox built up to the previous step |

Example (the gaps are single tabs):

```
# op	group	staged	target	mode	base
add	config	files/CLAUDE.staged.md	CLAUDE.md	644	-
add	config	files/dot-claude/hooks/tests-before-commit.sh	.claude/hooks/tests-before-commit.sh	755	-
replace	config	files/dot-claude/settings.json	.claude/settings.json	644	-
patch	exercise	patches/bookstore/src/main/java/Example.java.patch	bookstore/src/main/java/Example.java	644	0123456789abcdef0123456789abcdef01234567
```

Every file under `files/` and `patches/` must be named by exactly one row, and no target may appear twice.

### Patch rows

A patch is the `git diff` of one file, made in a sandbox built up to the previous step:

```
git -C "$SBX" diff --full-index --no-ext-diff --src-prefix=a/ --dst-prefix=b/ -- <target> > <step>/patches/<encoded target>.patch
git -C "$SBX" rev-parse HEAD:<target>     # the base column
```

`apply-level.sh` checks that the patch touches exactly the row's target and that its `index` line starts with the
base sha. When the target blob equals the base, the patch must apply as is. When the target differs from the base,
the base blob must be present in the target repository (a sandbox made by `simulate-level-0.sh` holds only the
level-0 blobs plus the blobs of the steps built on it) and the 3-way merge must be clean; otherwise the row is
refused. Patches are applied with `git apply --3way`, which also stages the patched file in the index.

## Applying a step

`apply-level.sh [--group config|exercise] <step> <target-repo>` applies one step to the top level of a git work tree:

- every row is checked before the first write; a write that fails afterwards (an unclean 3-way merge, a read-only
  file or folder) rolls back every file, created folder and the index, so on any error the target tree is unchanged
  and the apply prints `rolled back; the target tree is unchanged`;
- if the rollback itself cannot restore something, it still does every other restore step and removes its backup
  folder, then prints `rollback incomplete: could not restore <path>` for each such path instead of the line above;
- `--group` applies only the rows of that group; without it, all rows;
- the changes are left uncommitted;
- an error exits non-zero and names the step, the manifest line and the reason, for example
  `apply-level: error: level-2/manifest.tsv line 4: patch: the 3-way merge is not clean for bookstore/Foo.java`.

## Chain, commits and branches

| step | chain on top of level-0 | commit message | branch |
|---|---|---|---|
| `level-1` | level-1 | `Level 1 solution: context engineering` | `solution/level-1` |
| `level-2` | level-1, level-2 | `Level 2 solution: give your agent eyes (LSP + MCP)` | `solution/level-2` |
| `level-3` | level-1, level-2, level-3 | `Level 3 solution: behavioral control` | `solution/level-3` |
| `level-4-openspec` | level-1, level-2, level-3, level-4-openspec | `Level 4 start: OpenSpec installed and initialised` | `start/level-4-openspec` |
| `level-4-speckit` | level-1, level-2, level-3, level-4-speckit | `Level 4 start: Spec Kit installed and initialised` | `start/level-4-speckit` |
| `level-4-agentos` | level-1, level-2, level-3, level-4-agentos | `Level 4 start: Agent OS installed and initialised` | `start/level-4-agentos` |

`build-level.sh <step> <target-repo>` applies the chain to a level-0 repository with a clean working tree and makes
one commit per step with the message above, using the caller's git identity. After each commit it checks that the
commit changes exactly the step's manifest targets (a git-ignored target fails here) and that the tree is clean. A
failing step stops the build; the earlier step commits stay.

## Sandboxes on factory

Run from the repository root. The out folder must not exist yet (its parent must) or be empty, and must lie outside
the repository. Every temp folder in this README, the scripts and the self-test is made from a template under
`${TMPDIR:-/tmp}`, so a run with `TMPDIR` set writes nothing outside it (without a template, macOS ignores `TMPDIR`).

```
S=docs/spec-pack/level-branches
SBX="$(mktemp -d "${TMPDIR:-/tmp}/sbx.XXXXXX")/level-3"
bash "$S/scripts/simulate-level-0.sh" origin/factory "$SBX"
bash "$S/scripts/build-level.sh" level-3 "$SBX"
(cd "$SBX" && ./mvnw clean verify)
```

`simulate-level-0.sh [--repo <source-repo>] <ref> <out-dir>` reads the source repository only (`git rev-parse`,
`git archive`) and makes a new repository in `<out-dir>` with one commit: the tree of `<ref>` minus the spec 09 strip
list, with a fixed author, committer and date, so the same `<ref>` always gives the same commit. When `<ref>` holds
`scripts/publish-workshop-main.sh` and `publish-workshop-main.sh --list-paths` exits 0 with one repo-relative path per
line, that list is used; when the script is absent or the call fails, the built-in copy of the list in
`docs/spec-pack/specs/09-workshop-baseline.md` ("Publishing main", step 2) is used. An unsafe path (absolute, a
`.`, `..` or empty component, a carriage return) or an empty list from `--list-paths` is refused. The output names
the list used (`list: ...`) and every path in it (`strip-path: ...`).

For a level-3 tree without the cancel order exercise: build `level-2`, then
`bash "$S/scripts/apply-level.sh" --group config level-3 "$SBX"`.

## Self-test

```
bash docs/spec-pack/level-branches/tests/test-staging.sh
```

Offline, about a minute. It builds its own repositories and fixture steps in a temp folder (never the real level
content), covers every success path and every refusal of the four scripts, each refusal with its message and, for
`apply-level.sh`, the manifest line and an unchanged target tree, the line endings and modes on a fresh clone, and
checks that no ref of a source repository changes. Failures that a fixture cannot cause (an internal git command, a
mode change, a restore or cleanup step that fails) are forced with a small shim of that command, first on `PATH` for
one run. It finds the scripts relative to its own path, so a copy of this folder can be tested anywhere; outside the
repository set `SPEC09_FILE` to the spec 09 file. `KEEP_TMP=1` keeps the temp folder.

The Level 3 hooks have their own harness. It runs the staged `level-3/files/dot-claude/hooks/*.sh` and
`level-3/files/dot-githooks/pre-commit` against a stub `mvnw` and stdin fixtures, and checks the exit code, the stub's
arguments, the first stderr line, the tail and the temp files of each case:

```
bash docs/spec-pack/level-branches/tests/test-hooks.sh
```

Offline, about 15 seconds, needs bash 3.2 or later and git; the last line is `PASSED: <n> of <n>` and the exit code is 0
when every case passes. Run it on bash 5 and on macOS `/bin/bash` 3.2 with
`env PATH=/bin:/usr/bin:/usr/sbin:/sbin /bin/bash docs/spec-pack/level-branches/tests/test-hooks.sh`. It finds the staged
files relative to its own path, and `KEEP_TMP=1` keeps its temp folder. `test-staging.sh` does not run it.

## Line endings

`.gitattributes` in this folder keeps `*.sh` and `dot-githooks/*` LF, checks `*.ps1` out with CRLF, keeps
`manifest.tsv` LF and leaves `*.patch` unconverted. On the target, the root `.gitattributes` of `level-0-start`
takes over for the decoded paths (`*.sh`, `.githooks/*` LF, `*.ps1` CRLF).

## Cut and verify procedure (resumed run)

Precondition: spec 09 has published `main` and tag `level-0-start`, and every step folder is complete. Run from the
root of a clean `factory` checkout. Every step is local (step 1 only fetches). The cut script creates local branches
only; it never pushes and never creates, moves or deletes a tag. Publishing is a separate step, after this procedure
(next section).

1. Fetch `main` and the tag, and check that the tag is the tip of `main`:

   ```
   git fetch origin main
   git fetch origin tag level-0-start
   test "$(git rev-parse 'level-0-start^{commit}')" = "$(git rev-parse origin/main)" && echo "level-0-start is origin/main"
   ```

2. Run the self-test, then the dry run, and read every printed command:

   ```
   bash docs/spec-pack/level-branches/tests/test-staging.sh
   bash docs/spec-pack/level-branches/scripts/cut-level-branches.sh level-0-start
   ```

3. Create the six local branches:

   ```
   bash docs/spec-pack/level-branches/scripts/cut-level-branches.sh --execute level-0-start
   ```

4. Check the chain (expected counts 1, 2, 3, 4, 4, 4):

   ```
   for b in solution/level-1 solution/level-2 solution/level-3 start/level-4-openspec start/level-4-speckit start/level-4-agentos; do
     echo "$b $(git rev-list --count "level-0-start..$b")"
   done
   ```

5. Verify every branch in a clean checkout:

   ```
   VERIFY="$(mktemp -d "${TMPDIR:-/tmp}/verify.XXXXXX")"
   for b in solution/level-1 solution/level-2 solution/level-3 start/level-4-openspec start/level-4-speckit start/level-4-agentos; do
     git clone --quiet --single-branch --branch "$b" . "$VERIFY/$(echo "$b" | tr / -)"
     (cd "$VERIFY/$(echo "$b" | tr / -)" && ./mvnw clean verify) || echo "VERIFY FAILED: $b"
   done
   ```

To start over before anything is pushed, delete the six local branches with `git branch -D <branch>` and repeat
from step 2.

## Publishing the branches (orchestrator only, after every verify is green)

Not part of the cut. Only the orchestrator publishes, and only after every verify of step 5 above is green and the
publish gate is approved. No script pushes. A plain push fails when the remote branch already exists: investigate,
never force.

```
git push origin solution/level-1
git push origin solution/level-2
git push origin solution/level-3
git push origin start/level-4-openspec
git push origin start/level-4-speckit
git push origin start/level-4-agentos
```
