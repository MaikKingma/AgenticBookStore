---
name: built-html-rule-techniques
description: Writing and proving a guide check:built rule (built HTML, sitemap, fixtures, source mutants) without a shared fixture lib
metadata:
  type: feedback
---

- Docusaurus minified HTML has unquoted attributes. `lib/html.mjs` drops a trailing slash of an unquoted value written
  last in the tag (`href=/Base/>` reads as `/Base`). Read hrefs from the raw tag like `hrefOf` in
  `guide/scripts/rules/navbar-built.mjs` (it is not exported; copy it, that file may be out of scope).
- `grep -o` with a long bounded repeat dies on "exceeds complexity limits" here; quote real markup with a small node
  script (`html.indexOf`, `slice`) instead.
- A rule wired for the default build dir only (options in `check-built.mjs`) breaks every test that runs the CLI with no
  argument on a fixture (`withCopiedScripts` in `navbar.test.mjs`, `level-progress.test.mjs`). Extend their fixtures
  and list each count change as a retarget; tests that only `match` stderr need no change.
- Never import a `*.test.mjs` from another test file (its tests would run twice and change the node:test total); a
  shared fixture helper needs a file outside the test files, so check the file scope before duplicating it.
- Prove witnesses with a mutant runner script (`.claude/comms/tmp/*.mjs`): string-replace the rule in memory, write,
  run `node --test <file>`, restore in `finally`, compare sha256. Check each mutant actually applied. A mutant that
  breaks the good path is killed too cheaply; write the `endsWith` or `includes` mutant so the good path still passes.
- A mutant equivalent in the all-green case (a count printed from a constant) needs a test on the partial-failure
  return value (a deleted file gives `chainPages` 10).
