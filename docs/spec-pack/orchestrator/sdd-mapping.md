# SDD mapping

Discovered on 2026-11-06 in `~/.claude/skills/`, `~/.claude/agents/`, `~/.claude/workflows/`, `~/.claude/scripts/` and `.claude/commands/agent-os/`. No installed plugins carry sdd commands. No `~/.claude/commands/` folder exists.

The sdd cycle orchestrator is the `orchestrator` agent (`~/.claude/agents/orchestrator.md`). It runs as a subagent, reproduces the `/sdd-cycle` stages with direct Agent spawns, self-accepts Gate 1 and Gate 2 when the product docs and specs settle the question, and stops with a needs-decision otherwise. Maik chose it over `/sdd-cycle` in the main session on 2026-11-06.

| Cycle stage | My command | Input it expects | Artefact it produces | Folders it writes to |
|---|---|---|---|---|
| Init | `/sdd-cycle` Phase 0 (skill `team-orchestration`, script `~/.claude/scripts/sdd-worktree.sh prune-stale`) | repo root | `.claude/comms/registry.md` | `.claude/comms/`, `.claude/worktrees/` |
| Shape | `/sdd-cycle` Phase 1 using the shaping steps of `/agent-os:shape-spec`, workflow `sdd-shape-support` (research-agent, senior-research-agent, tech-lead-strategist), then `qa-lead` strategy | feature brief, `agent-os/product/*`, `agent-os/standards/index.yml` | plan file, research and strategy outbox artefacts, QA ledger | `.claude/comms/mailboxes/`, `.claude/agent-memory/` |
| Gate 1 | `ExitPlanMode` in the main session, or self-accepted by the `orchestrator` agent | plan | `plan.md` frontmatter `gate1:` | none |
| Spec folder and spec PR | `/sdd-cycle` Phase 2/3 step 1 | approved plan | `agent-os/specs/<YYYY-MM-DD-HHMM>-<slug>/{shape,standards,references,plan}.md`, branch `sdd/<slug>/spec`, PR into base | `agent-os/specs/` |
| Implement and QA per task | workflow `sdd-implement` (implementation-agent with `/tdd`, qa-test-verifier, qa-lead meta-review) | `plan.md` task card, branch `sdd/<slug>/<task-id>` | code in the task's file scope, outbox artefacts, task PR into the spec branch, `chore(agent-memory)` commits | task file scope, `.claude/comms/`, `.claude/agent-memory/` |
| Final QA sweep | `/sdd-cycle` Phase 2/3 step 5 (qa-lead final-brief, `qa-final`, qa-lead final-review) | diff base to spec branch | qa-final verdict, Gate 2 QA assessment | `.claude/comms/`, `.claude/agent-memory/` |
| Gate 2 | AskUserQuestion in the main session, or self-accepted by the `orchestrator` agent | spec PR, qa-final verdict | `plan.md` `gate2:` | `agent-os/specs/` |
| Finalize | `/sdd-cycle` Phase 5 | none | memory commit, registry cleanup, spec PR handed over or merged with `merge: true` | `.claude/agent-memory/`, `.claude/comms/archive/` |
| Whole cycle, autonomous | agent `orchestrator` (reproduces all stages above with direct Agent spawns) | `spec: <brief>` or `phase:` plus flags | run ledger `.claude/comms/mailboxes/orchestrator/outbox/run-<slug>.md` plus everything above | same as above |
| Bug fix | `/sdd-bugfix` | bug report | `agent-os/specs/*-fix-*/bug.md` | same as above |
| Product docs | `/agent-os:plan-product` | interactive | `agent-os/product/{mission,roadmap,tech-stack}.md` | `agent-os/product/` |
| Standards | `/agent-os:discover-standards`, `/agent-os:index-standards`, `/agent-os:inject-standards` | codebase | `agent-os/standards/**.md`, `index.yml` | `agent-os/standards/` |

## Folders the SDD commands write to (for D5, spec 09)

- `agent-os/` (specs, product symlink, standards)
- `.claude/agent-memory/` (committed by the cycle in `chore(agent-memory)` commits)
- `.claude/comms/` (gitignored, required by the cycle)
- `.claude/worktrees/` (gitignored)
- `.claude/settings.json`, `.claude/commands/`, `.claude/skills/`
- `CLAUDE.md`
- branches `sdd/<slug>/spec` and `sdd/<slug>/<task-id>` on the remote

## Conventions carried into Phase 3

- Base branch for the cycle: `factory`. Task PRs squash-merge into the spec branch; the spec PR merges into `factory` with a merge commit (Maik, 2026-11-06).
- Module vocabulary: `bookstore`, `distributor`, `frontend`, `guide`, `root` (root pom and scripts).
- Commit format: Conventional Commits, Claude Code attribution footer permitted on `factory`.
- The `orchestrator` agent needs the Agent tool, not the Workflow tool. The helper scripts in `~/.claude/scripts/` (`sdd-worktree.sh`, `with-budget.sh`, `sdd-qa-preflight.sh`, `check-agent-memory.mjs`) are present and executable.
