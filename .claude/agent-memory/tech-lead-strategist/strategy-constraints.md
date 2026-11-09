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
  The pack task lists put the flow before the data adapter; flip it and say why in `dependsOn`. The same holds for
  data existence (a query adapter that composes flows merges after the task that guarantees the composed rows exist)
  and for pins: a view change merges in the same task as the full-context tests that pin its key set.
- **View before behaviour.** When a spec adds a view field and the behaviour that fills it, merge the view first
  (its pin edits stay mechanical, e.g. `null`) and let the behaviour task update the pins it changes; the behaviour
  task also owns the end-to-end test for its proxy or after-commit claims (spec 07 strategy). JSON pins with a null
  value cannot use `Map.of` (it throws on null); name a null-tolerant builder in the card.
- **Transactional claims need a failure device.** With `saveAndFlush` adapters, removing `@Transactional` is invisible
  on the happy path, so plan a behavioural proof per claim: a test-only `BEFORE_COMMIT` thrower plus an
  `AFTER_ROLLBACK` recorder for "events after commit, none on rollback", a `@MockitoSpyBean` on the shared base that
  throws for multi-aggregate atomicity. Inspection alone leaves the QA mutation probe surviving.
- **Tool trap: escapes in tool input.** Write/Edit decode a typed backslash-u-hex sequence into the literal character
  (it hit a strategy doc and an orchestrator brief). Java test data that needs such a character is built from code
  points (`Character.toString(0x2003)`); every doc and source written gets a byte-level `[^\x00-\x7F]` grep.
- **Criteria that cost QA rework rounds (specs 02, 03).** Name every falsifier test in the card up front; every
  ordering claim gets reverse-ordered data and a tie inserted opposite to the asserted order; an "unchanged" claim
  (signal, stock, version) gets success cases from both starting states; orchestrator-added criteria stay
  source-level and inside the task's file scope. Put these in the plan's ground rules, not only in cards.
- **Seed data consumed by later specs: derive guards from the consumers.** Read the downstream specs (import invariants,
  fixed years, contract values, ISBNs that must be absent) and pin each as a named seed test in the producing task;
  facts a test cannot check (death years, fictional names) get an outbox table plus a QA spot-check.
- **DoD vs orchestrator ledger.** Check DoD lines that write to `docs/spec-pack/orchestrator/progress.md`; the
  orchestrator may have decided not to edit it, which leaves the line unowned. Raise it as an open question.

Related: [[test-context-cost-model]], [[archunit-referee-design]].
