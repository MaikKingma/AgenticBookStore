---
name: subagent-run-mechanics
description: How to run the cycle as a subagent orchestrator here (waiting on children, hand-back, resume, steering children, cwd and memory paths, timestamps, memory guard, fast paths)
applies-to:
  - any
metadata:
  type: project
---

## Waiting and hand-back
- A foreground Bash call is moved to the background after about 180 s: poll in loops of at most 160 s with
  `.claude/comms/tmp/orchestrator-wait-inbox.sh <inbox> 160` or `orchestrator-wait-status.sh <artifact> 160`.
- The harness forces a hand-back when the run idles: keep the ledger resume point current before every long wait, and
  hand back with a precise resume point instead of waiting for a main-session check.
- SubagentHandback works once per resume of me; later updates to the main session go by SendMessage to "main".

## Steering children
- Prefer SendMessage to a finished or rate-limited child over a fresh spawn for rework, re-verification and resume:
  context kept, tokens saved. Its outbox can be stale, so ask it to rewrite the outbox first.
- I cannot TaskStop my children: stop or redirect one with an urgent inbox directive plus SendMessage, accept a
  BLOCKED verdict, record a plan amendment for an owner decision that arrives mid-task, and bound the next QA round to
  the change.
- Resume after a cut-off: children of the dead run can still be alive. Check the artifact mtime and `activity` before
  respawning; if under 15 min, arm a Monitor and wait (a respawned qa-lead once made two writers on one ledger).

## Paths and evidence
- A relaunch can start with cwd in the main checkout while the project root is a worktree: every brief names the
  absolute root and "memory only under <root>/.claude/agent-memory/<agent>/"; check the main checkout's `git status`
  after each agent.
- Shared scratchpad: verifiers overwrite each other's generic logs; brief every agent with a role and task prefix for
  evidence files (`t5-qa-*`).
- Take every ledger, registry and plan timestamp from `date -u`; hand-written stamps (mine and agents') run ahead and
  the QA lead checks ordering by mtime. Trust mtimes over `updated:`.

## Memory guard
- Run the guard BEFORE the memory commit, and after every factory merge.
- Agents keep growing their MEMORY.md hook past 250 chars: the curation prompt says "short hook, do not list every trap".

## Saving rounds
- Combine the QA lead's last task review and the qa-final brief in one spawn.
- Owner fast path ("fast but checked"): verifier PASS plus green CI merges; time-box any meta-review with a background
  `sleep 600` timer; one reduced qa-final.
- Referee times swing (20 to 34 s) with parallel orchestrators on the machine: quote both measurements.
