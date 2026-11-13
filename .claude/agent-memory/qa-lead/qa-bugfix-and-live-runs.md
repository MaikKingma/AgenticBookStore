---
name: qa-bugfix-and-live-runs
description: sdd-bugfix lite cycles (no strategy run, common block gaps), bugfix final brief (cross negative controls, scratch tests, worktree recipes), shared compose database across checkouts, live-captured contract fixtures
applies-to:
  - bookstore/src/test/**
  - bookstore/**/compose*.y*ml
  - "*.http"
metadata:
  type: feedback
---

Siblings: [[qa-final-brief-integrity]], [[qa-request-collection-smokes]], [[qa-directive-traps]]. Calibration for bugfix evidence chains: [[review-scope-rulings]].

1. **Bugfix lite cycles have no strategy run.** sdd-bugfix tasks carry only the reference.md section 5 qaDirective, so the common block is missing: task-prefixed scratch names, the warning inventory with exact counts ([[qa-warnings-and-logging]]) and repo-relative hash baselines ([[qa-final-brief-integrity]]). At the first review settle the gaps by reading the logs yourself and feed the block forward. Two tasks sharing test files also need "the earlier task's methods unchanged" in the later directive.

2. **Bugfix final brief.**
   - (a) Cross negative control: in each task's negctl worktree copy ONLY the other task's fixed production file; the regression test must still fail, proving it pins its own root cause. Cheapest: a worktree at the spec TIP, revert one task's root-cause file(s) from its parent commit, run the union of all tasks' regression classes and pin the exact expected red set (dry-run it), with an unmodified N-tip count first and a restore plus porcelain check between controls. Restore test files that call the fix's new constructor with the production file or the control dies at compile. An exported-variable control on a reverted test pin has only the exported loopback base URL between the test and the real API: never run it without that variable.
   - (b) A removed payload constraint makes flow paths newly reachable over HTTP that only flow tests pinned (same book 11 and -1 merging to 10): brief them, plus cross-task combos (padded e-mail with an out-of-range line), as scratch tests in a throwaway worktree; ask the verifier to `cp` the scratch test source to SP before `git worktree remove`, or assertions beyond printed status (stock, nothing stored) cannot be re-read.
   - (c) When each fix has its own layer's test and higher layers stub the lower port, the cross controls of (a) pass trivially; the real cross-task item is a scratch full-context test through the real adapter (in-test `HttpServer`, not the mocked base class), run on the tip, on base production (must be red) and with each single fix. Pin negctl states by per-commit production hashes.
   - (d) A negctl copies every NEW test resource the regression tests read, not only classes, or a guard test turns red for a missing file.
   - Live late answers: Playwright `route.fetch()` then a gated `route.fulfill({ response })`; browser-cache proof by Tomcat access log: two 200s on a non-reload second navigation. A walk provoking a 4xx on purpose (409) gets Chromium's `Failed to load resource ... 409` console error: name it as the one expected line, never "zero console errors".

3. **Live runs share one compose database across checkouts.** `bookstore/compose.yaml` gives project name `bookstore` in every worktree, so the container is `bookstore-postgres-1` everywhere; `docker compose down` from any checkout, or `spring-boot:run` (compose support, start-only), attaches to or removes a parallel spec's database. Lead cards still write "`docker compose down` afterwards": strike it at Gate 1.
   - Use a throwaway `docker run --name s<NN>-...-pg` on a free port, `java -jar` with `--spring.docker.compose.enabled=false` plus an explicit datasource, `docker rm -f`; evidence: `docker inspect` StartedAt of the shared container identical before and after.
   - Verifier session scripts a final brief tells qa-final to reuse often carry `spring-boot:run` plus `docker compose down -v`: the brief names the allowed rewrite of the copies (DB access via `docker exec <own container>`, start/stop, ports) and demands a quoted `diff`.
   - Owner rule: an agent never creates or removes `bookstore-postgres-1` and removes every container it creates. Write live steps as a numbered C0 item (own container, referee jar, app port never 8080; isolation proof by names-only listings, own container absent after, foreign names reported, shared container StartedAt equal; a full `docker ps -a` diff is never empty under parallel cycles).
   - A feed-forward pinning a count of hash lines goes stale when a later amendment adds one of those files to the next task's scope; my own feed-forwards must never start the app with `spring-boot:run` (it created the shared container).

4. **Live-captured contract fixtures (JSON bodies from a running service).** What held: an independent census script per acceptance criterion, a live replay whose comparator ignores only the capture-day fields named in advance (random ids, relative dates, framework error timestamps), byte `cmp` as the stronger form, and negative controls on both scripts (a cent, a name, a status, an ignored key that must stay ignored). Derived files get a byte proof. A chaos run is proven by the body (same GET 200 then 503), not the log. Consuming task: load every captured body from the classpath, assert capture-day values as literals, make the strict request matcher the killer of any request field the service does not echo.
