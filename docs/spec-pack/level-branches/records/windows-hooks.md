# Windows and the Level 3 hooks

Input for the spec 11 guide (Level 3 page and setup check). Written for spec 10 task T8 without a Windows machine and
without running Claude Code (decision D14). It says what is known, from the Claude Code docs, the pack, the staged
files and the hook harness, and lists what needs a Windows machine as manual checks for Maik.

Every factual sentence carries its source in brackets. The keys are listed under "Sources" at the end; the
sentence-by-sentence trace is `evidence/level-3/t8/windows-hooks-trace.txt`.

## 1. Git for Windows (Git Bash) is a prerequisite

- Level 3 ships the bash hooks and the git pre-commit hook only, with no PowerShell variants (decision D16) [D16,
  manifest 7-9].
- Windows participants need Git for Windows (Git Bash) for Level 3, and the hooks run there [pack 323-325, D9, D16].
- The Level 3 commit hook is a `PreToolUse` handler with the matcher `Bash` and the `if` rule `Bash(git commit *)`,
  and every permission rule that runs a command is a `Bash(...)` rule [settings 12-16, 21-24, 30, 34].
- With Git for Windows, Claude Code uses Git Bash for the Bash tool [Q1 126].
- Without Git Bash, Claude Code does not register the Bash tool at all, and a hook that matches only `Bash` never fires
  [Q2 1858-1859].
- So without Git Bash the commit hook does not run: a `git commit` that Claude runs is neither built first nor blocked
  by the hook, and the `Bash(...)` deny rules do not apply to it [Q2 1858-1859, pack 323-325, D9].
- Git Bash check: Claude Code must keep running shell commands through its Bash tool, so participants do not enable the
  PowerShell tool with `CLAUDE_CODE_USE_POWERSHELL_TOOL=1` [Q6 487, D9]. With Git Bash installed, the four `Bash` deny
  rules of Level 3 turn the PowerShell tool off unless that variable is set or a `PowerShell` permission rule exists
  [settings 21-24, Q6 483-490]. Where the PowerShell tool is on, Claude routes shell commands through it, and the `Bash`
  matcher, the `if` rule `Bash(git commit *)` and the `Bash(...)` rules do not apply to those commands [Q7 1855-1857,
  Q8 339, Q6 483, pack 324-325].

## 2. A hook that cannot start blocks the commit

- The commit hook handler sets `"onFailure": "block"` [settings 36].
- With it, Claude Code 2.1.295 or later blocks the commit when the hook cannot start, times out, or exits with any code
  other than 0 and 2 [E 26, E 29, E 35, Q3 964-972].
- So whenever the hook fires, a missing interpreter or script blocks every `git commit` through Claude until it is
  fixed, for example a hook command whose script path does not exist [E 29, E 35, Q3 966].
- A missing Git Bash is different: then the hook does not fire at all (section 1) [Q2 1858-1859].
- The docs also say that on Windows without Git Bash, a shell-form `PreToolUse` command hook that fails with
  `onFailure` unset can block the tool call [E 78]; the Level 3 commit hook sets `onFailure` [settings 36].
- The scripts themselves exit 0 (go ahead), 2 (block, stderr goes back to Claude) or 1 (the project folder cannot be
  entered) [H tbc-commit-green, H tbc-commit-red, H tbc-could-not-cd].
- The pack comment "Any other non-zero code does NOT block." [pack 283] is stale for this handler, because
  `onFailure` is set: product finding PF-QA-T7-1. The staged script keeps the pack text [pack check].
- The Stop handler has no `onFailure` [settings 42-51], and the field would have no effect on a `Stop` hook [Q4].
  Only its exit code 2 keeps Claude working [E 69]; its exit code 1 is a non-blocking error [Q5].

## 3. The git hook

- `.githooks/pre-commit` prints `pre-commit: ./mvnw -q verify`, runs `./mvnw -q verify` and aborts the commit when the
  build fails [pack 331-333, H githook-green-commit-goes-through, H githook-red-commit-aborted].
- It runs only after `git config core.hooksPath .githooks`, once per clone [pack 335, H
  githook-inactive-without-hooks-path], and `git commit --no-verify` skips it [H githook-skipped-by-no-verify].
- It is a `sh` script that calls the Unix wrapper `./mvnw`; `level-0-start` keeps `mvnw` and `.githooks/*` with LF line
  endings [pack 331, attributes]. Whether Git for Windows runs it for a `git commit` typed outside Git Bash, for
  example in CMD, is not checked here (check W3).

## 4. What needs a Windows machine (manual checks for Maik)

- W1: with Git for Windows, `git commit` through Claude Code runs the commit hook in Git Bash and a red build blocks it
  with exit code 2 and the stderr tail; this answers the pack's open point on the "shell Claude Code uses for hook
  commands there" [pack 327].
- W2: without Git for Windows, the commit hook does not fire for a `git commit` that Claude runs [Q2 1858-1859].
- W3: `git config core.hooksPath .githooks` makes Git for Windows run `.githooks/pre-commit` for a `git commit` typed
  in Git Bash and for one typed outside it, for example in CMD, and a red build aborts the commit.

## 5. Owner decisions (J6, D16)

- J6 left two questions for Maik [shape J6]: `"onFailure": "block"` on the commit hook, and handlers and rules for
  Windows without Git Bash.
- Maik decided both in D9: the commit hook gets `"onFailure": "block"`, and Windows participants need Git Bash, with
  no handlers or rules added for Windows without it [D9]. The field exists from Claude Code 2.1.295 on [E 10], and T7
  staged it [settings 36].
- Maik decided in D16 that Level 3 ships no PowerShell hook variants and the guide drops its PowerShell hook tabs [D16].

## 6. Product findings

- PF-QA-T7-1 (from T7): pack line 283 and guide line 105 say a non-zero code other than 2 does not block; with
  `onFailure` set it does [pack 283, G 105, E 35].
- PF-T8-1: `tests-before-commit.sh` looks for `git commit` only up to the first double quote of the command, so a
  command with a double quote before `git commit` (escaped as `\"` in the JSON input), such as
  `cd "<folder>" && git commit -am wip`, exits 0 without a build [H tbc-escaped-quotes-before-commit].
- PF-T8-2: plan feed-forward FF-T7-a asks this note to say that a missing Git Bash blocks every `git commit`; the docs
  and D9 say the hook does not fire without Git Bash [Q2 1858-1859, D9], so this note follows the docs (section 2).
- PF-T8-3: when `git ls-files` fails, `architecture-check-on-stop.sh` reads its empty output as "no untracked Java
  file" and exits 0 without the check [H acs-git-ls-files-fails].

## Sources

- `Q1` to `Q8`: Claude Code docs, quoted with line numbers in `evidence/level-3/t8/docs-quotes.txt`.
- `pack N`: line N of `docs/spec-pack/specs/10-harness-reference-solutions.md`.
- `D9`, `D16`: `docs/spec-pack/orchestrator/decisions.md`, sections D9 and D16 (D16 is on `factory` from commit
  `7ba87a9`); `shape J6`: line 46 of the spec 10 `shape.md`.
- `E N`: line N of `evidence/level-3/t7/d9-onfailure-and-hook-triggers.txt`.
- `settings N`: line N of `level-3/files/dot-claude/settings.json`; `manifest N`: line N of `level-3/manifest.tsv`.
- `pack check`: `evidence/level-3/t8/pack-blocks-vs-staged.txt`.
- `H <row>`: a row of the hook harness `tests/test-hooks.sh`, run in `evidence/level-3/t8/harness-bash5.txt` and
  `harness-bash32.txt`.
- `attributes`: the rules of the root `.gitattributes` of `level-0-start` that match `mvnw` and `.githooks/pre-commit`,
  quoted at the end of the trace file.
- `G N`: line N of `guide/docs/04-level-3-behavioral-control.mdx` on `origin/sdd/11-workshop-guide/spec` at
  `fb958b3`.
