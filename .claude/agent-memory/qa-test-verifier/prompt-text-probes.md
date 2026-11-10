---
name: prompt-text-probes
description: Probing a system-prompt resource plus @Tool/@ToolParam description text - mutant families, red-set shape, guard-test witnesses
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
- A domain parser used as a witness (`Genre.parse`): mutate it three ways (accept space, accept space by another spelling, drop `toUpperCase`); the last one dies as an ERROR (exception from the helper), which still counts as red.

Related: [[tool-adapter-probes]], [[env-switch-mutant-runner]]
