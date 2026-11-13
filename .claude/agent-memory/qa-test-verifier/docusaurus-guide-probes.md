---
name: docusaurus-guide-probes
description: guide/ node:test check harness - probe runner, wiring mutants, check:built, registration and rule-table guards
metadata:
  type: feedback
---
Rule: probe the Node harness with one fs-based runner script, not shell edits.
**Why:** a runner that edits, runs, records the exit code and restores from `cp -p` byte copies keeps probes honest and fast (`npm run check` about 1 s, a build about 2 s); hash equality with the snap-before file proves the restore.
**How to apply:**
- Runner in `.claude/comms/tmp/` as an ASCII `.mjs` (forbidden characters via `String.fromCodePoint`). Ops: write, append, replaceOnce with an exact-occurrence guard (a stale `from` shows as SETUP ERROR, not a kill). Restore touched files, delete created ones, compare hashes at the end.
- Mutants run `node --test "scripts/**/*.test.mjs"` through `spawnSync` with the glob unexpanded; collect `✖` lines (skip the summary `✖ failing tests:`). `npm run check` is `node --test` THEN `node scripts/check.mjs`: run the layers separately, because fixtures reading the real CSS fail first and mask whether the checker itself catches the defect. Check equivalent survivors (removing a `sort()` is unobservable on APFS).
- Anti-vacuity: a glob matching nothing exits 0 (`tests 0`); emptied test files count as passing; probe a literal-text rule against code blocks, inline code and a NUL-byte file. Never run `npm --prefix guide exec -- node ...` (fetched node@27 alpha, 0 tests): run node from a script that `cd`s into guide/.
- Offline matcher: WebKit's pageerror is `XMLHttpRequest cannot load file: //...` with no `Error: ` prefix. Playwright Firefox `context.route` never sees `file://`, so "aborted 0" proves nothing until `fetch('https://example.com/x')` moves the abort counter 0 -> 1 in every engine.

Rule: for a checker with pure rule modules, mutate the WIRING (`runChecks`, site-rules glue, CLI default root, option names), not only the rule functions.
**Why:** a rule module at 100 % coverage with every unit mutant killed can still be dropped by the pipeline (errors discarded, report lines not printed, option off, wrong root). Deleting one entry of a declarative table (contrast pairs, anchors) survives unless a test pins the table.
**How to apply:**
- One mutant per `report(...)`, `errors:` spread and `lines.push(...)`, warning instead of error, ban not called, first or last page dropped, wrong root, predicate substring or startsWith, no sort, message without page; plus a generated "remove one entry of every table" probe and a "lower the pin with the removal" control that must still fail by name.
- Reinsertion probe: put the banned token before the final required section, never at the end (an appended comment breaks structure rules). Keep byte copies OUTSIDE `docs/`. `npm run check` fails first in node:test; the ban's own stderr line shows only from `node scripts/check.mjs`: quote both.

Rule: for a `check:built` rule, run a mutant table over rule AND wiring, then replay defects on a scratch copy of the REAL build.
**Why:** fixture-only witnesses never show the rule reads real minified Docusaurus markup (unquoted attributes, `href=/Base/>`); a wiring mutant that restates an option already set is equivalent, not a survivor.
**How to apply:** runner rows `[id, desc, file, from, to, testFiles]`, sha256 compare at the end, `--test-reporter=spec`, plus a "retarget bites" pair (drop the `navbar`/`levels` option, fixture tests go red). Scratch layout MUST be `<top>/guide/{build,scripts,docusaurus.config.ts}` plus `<top>/docs` symlinked (built.mjs finds `docs/spec-pack` relative to scripts), else the control already fails and every probe looks red for the wrong reason. Built-HTML edits need a unique anchor (`</nav>`, `href=/Base/wrap-up>` repeat in the sidebar).

Rule: for a page-registration task (new page, count pins, marker rule), compare guard counts with the node:test total and mutate the "before X" scan at its boundaries.
**Why:** `rules/zero-tests.mjs` strips block comments textually, so a test string `'{/* a */}{/* unclosed'` swallowed 19 `test(` registrations (printed count fell while node:test rose). A "nothing but blanks before the box" scan survived `index < box.start - 1` because every witness had a blank line.
**How to apply:** script importing `countTestCases`, guard vs plain `^[ \t]*test\(` counts per file HEAD vs tip, equal deltas expected (flag only new drops). Ordering-rule witnesses: raw adjacent text, empty body (`:::tip` then `:::`), CRLF, indented opener, HTML comment, setext heading, link hidden in a comment in the box.

Rule: for page-to-plan guard rules (membership, fragment, no-minutes, script ban) generate one mutant per table entry from the rule source, plus bounds (`index <= range.to`); a test looping over the exported key table pins nothing (15 of 19 keys, 7 of 13 number words and a pattern's `i` flag survived). Pair controls in a scratch copy (`git show <base>:<test>`: survive with the base test, die with the tip test). Live probes edit the REAL plan, so a new plan literal also fails the pack-sweep manifest tests (read the guard's own message); `node scripts/check.mjs` alone never runs test-file guards, only `npm run check` does.
Related: [[docusaurus-markdown-html-probes]], [[docusaurus-content-ui-probes]], [[shell-traps]]
