---
name: strategy-constraints
description: Recurring factory constraints that should shape every task breakdown and trap list (module batching, stripped paths, DoD greps, shared Docker, git ownership)
metadata:
  type: project
---

- **One module per task.** The orchestrator batches by disjoint modules (root, bookstore, distributor, later frontend).
  Check briefed file scopes for spillover (spec 01: distributor config sat inside a "bookstore" task) and split it.
- **`main` strips agent and pack paths** (spec 09 publish script: `docs/spec-pack/`, `agent-os/`, `.claude/`,
  `CLAUDE.md`). Code, comments, POMs, YAML and CI must never cite them; explanations go inline in plain words.
  Proposed as an `agent-os/standards/` rule in the spec 01 strategy outbox; delete this bullet once it is a standard.
- **DoD greps are substring greps.** `grep -ri h2 ...` matches "no H2" in a comment; ban the token in the grep scope.
- **Shared machine.** Parallel agents share one Docker daemon; never stop it for a "Docker missing" scenario. Simulate
  per command: `DOCKER_HOST` pointing at a missing socket plus `-DargLine=-Duser.home=<empty dir>` (DOCKER_HOST alone
  falls back to the Docker Desktop socket). Compose project names follow the module directory, so live
  `spring-boot:run` checks collide across worktrees: one live runner at a time, `docker compose down` afterwards.
- **Git is orchestrator-owned.** Spec phrases like "verified in a throwaway branch" become uncommitted probes in the
  task worktree unless the orchestrator creates the branch; say which in the strategy.
- **Spec wording one to one.** Prefer a config that makes the spec's literal command valid (Windows CI: `shell: cmd`
  so `mvnw.cmd -B ...` runs as written) over rewording.
- **Green after every merge, bean order.** Tasks run one at a time and every full-context test shares one context, so
  a task adding a bean with a constructor dependency must merge after the task providing it: data adapter (port
  impl) before the flow that injects the port, flow before the controllers, `Clock` config with its first consumer.
  The pack task lists put the flow before the data adapter; flip it and say why in `dependsOn`.
- **DoD vs orchestrator ledger.** Check DoD lines that write to `docs/spec-pack/orchestrator/progress.md`; the
  orchestrator may have decided not to edit it, which leaves the line unowned. Raise it as an open question.

Related: [[test-context-cost-model]], [[archunit-referee-design]].
