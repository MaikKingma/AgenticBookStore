---
name: harness-staging-lessons
description: Lessons from spec 10 (agent config staged for level branches, consistency checks instead of headless Claude runs); verify against the spec 10 plan before relying on them
metadata:
  type: project
---

- **Headless Claude Code under an isolated HOME cannot log in** (macOS Keychain does not follow HOME or
  CLAUDE_CONFIG_DIR; `--bare` needs an API key, which is forbidden). Maik's answer was D14: no agent runs Claude
  Code; scenarios become consistency checks plus a rehearsal checklist for Maik. Plan for that from Gate 1 when a spec
  asks for "run its scenarios" with Claude.
- **Stage agent config outside its live paths**: Claude Code loads CLAUDE.md, `.claude/rules` and `.claude/skills`
  from subfolders; spec 10 staged them under `docs/spec-pack/level-branches/` with `dot-` names and a manifest
  (plan.md J1, README there).
- **Maik's plugin guard (HR2)**: any Write of a settings file adding plugin keys, and any Bash command naming them,
  prompts him; brief agents to write such a file once, in one Write, and inspect it with Read only.
- **HR1 sandbox hygiene**: TMPDIR plus explicit `mktemp` templates (macOS `mktemp -d` ignores TMPDIR),
  `JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=...` (the JVM ignores TMPDIR), free ports (other worktrees hold 8080/8090),
  never a script that appends to a file it reads (one filled 300 GB), docker containers labelled and removed.
- **Rework rounds on doc-heavy tasks narrow slowly**: the QA lead rejected PASSes for false universal or descriptive
  sentences in authored files (reference.md, READMEs, rehearsal rows). Put "per-sentence truth table with a git grep
  counterexample per claim" and "every rehearsal row: start commit, `reset --hard <start> && clean -fdx`, judge
  against start, fresh clone folder for auto memory, sourced start triggers with a branch" into the implDirective of
  round 0.
- **Relaunched into an existing worktree as a subagent, cwd stays the main checkout** (`cd` does not persist), so
  every brief names the absolute project root and tells the agent to write memory only under
  `<root>/.claude/agent-memory/<agent>/`; check the main checkout's `git status` after each task (held for T9).
- **Parallel tracks in scratch worktrees** (`git worktree add` under the scratchpad): agents keep comms in the main
  worktree and memory in the track worktree; the QA lead writes memory in the main worktree, so copy its files into
  the track's memory commit only AFTER its handback (an earlier copy lost a line), then `git checkout --` them in the
  main worktree. Each later track merges the spec tip first; memory conflicts are union plus owner curation.
- **Factory merge before a cut**: run every gate (self-tests, referee) on the merge result BEFORE pushing it, never in
  parallel; a changed strip list (spec 09 D15) broke the staging self-test once. After a factory merge whose memory
  union overflows budgets, spawn one curator per over-budget agent type in parallel.
- **Prefer SendMessage to a finished child over a fresh spawn** for resume, rework and re-verification rounds: it
  keeps context and saves tokens (also after a usage-limit stop). One SubagentHandback per run; later updates to the
  main session go by SendMessage to "main".
- **Owner decisions arrive mid-task** (D9, D14, D16): redirect a running agent with an urgent inbox directive plus
  SendMessage (an orchestrator cannot TaskStop its children's tasks), record a plan amendment, and bound the next QA
  round to the change.
