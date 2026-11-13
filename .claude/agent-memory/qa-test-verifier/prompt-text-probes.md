---
name: prompt-text-probes
description: Text-only fixes (prompt resource, @Tool descriptions) - clause-level mutants and guard-test bite proofs
applies-to:
  - bookstore/src/main/resources/**
  - bookstore/src/main/java/**/assistant/**
metadata:
  type: feedback
---
Rule: for a task whose fix is only text (a `.st` prompt, annotation descriptions), the negative control is a swap of BOTH production files to `git show HEAD:<file>` and the proof of strength is a phrase-level mutant batch, not coverage.
**Why:** line coverage is vacuous for string constants, and a `contains(...)` test that pins a whole sentence can still leave its clauses free. Only deleting or rewording one clause at a time shows which words a test really owns.
**How to apply:**
- One `python3 -I` generator (exact-once replace, abort otherwise) plus a cp-restore runner; a 50-probe batch over two test classes takes about 4 minutes with `clean` (about 5 s per probe).
- Families: each list item dropped/swapped/added (genres, fields), one clause of each rule dropped, each spec line deleted on its own, the example in an instruction changed to a wrong spelling (spaced, hyphen, lowercase, other constant), renderer-unsafe chars (`{x}`, `$`, em dash) to prove the pre-existing safety test still bites, annotation text dropped from the `@Tool` description only and from the `@ToolParam` only, one word of a spec sentence altered.
- Expect survivors in explanatory clauses no AC pins (a "never X and Y together" parenthesis, a "you do not remember" sentence); report them as advisory with the one assertion that would close them.
- Guard tests that are green before and after by design need their own bite proof: run the integration class on the pre-fix bytes (green), then mutate the production behaviour the guard documents (a repository `like :q` criterion made match-all; the conversation id randomised per call) and name the red test lines.
Related: [[tool-adapter-probes]], [[env-switch-mutant-runner]]
