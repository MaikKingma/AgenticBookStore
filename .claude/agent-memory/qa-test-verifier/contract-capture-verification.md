---
name: contract-capture-verification
description: Verifying captured contract/fixture files against a live service: own census, README-driven curl replay, comparator with mutated-copy controls, byte proofs for derived files
metadata:
  type: feedback
---

Rule: prove captured bodies are real by replaying them against a service you start yourself, with a comparator you have shown can fail.

**Why:** a census over the files only proves internal consistency; a hand-edited or stale capture passes it. The replay is the only evidence of "byte for byte the real service".
**How to apply:**
- Drive the replay from the README tables and the committed request files (curl `-o <file> -w %{http_code}`, `--data-binary @<request file>`), so the driver never holds a retyped request list. One run per server flag set (normal, chaos), then one comparator over both status files.
- Comparator: status vs README status column, byte-equality first, parsed-JSON equality after popping only the named capture-day fields (ids, delivery dates, relative release date, framework timestamp). Report byte-identical vs ignored-field-only counts. Controls on committed-dir copies: +1 on a number, one letter in a name, a changed README status, a changed non-ignored field, and a changed IGNORED field that must still exit 0.
- Replaying on the capture day makes relative dates equal too; only random ids and timestamps differ. Say so, so a later-day replay is not read as a regression.
- Derived files: `sed '<one substitution>' <source> | cmp - <derived>` exits 0, plus a control `cat <source> | cmp - <derived>` exits 1. This proves "exactly one change" at byte level; list insertions (subjectCodes) and swaps (contributors) are a single README-named change but several flattened paths, so assert them as whole-key comparisons.
- Genre/prefix tables: parse the spec line itself (regex on backticked codes) instead of retyping it; assert a falsifier where the second subject code would give a different genre.
- Start/stop: `./mvnw -pl distributor spring-boot:run` via a script in `.claude/comms/tmp/`, `run_in_background`; the background task reports "failed exit 1" after you kill the child, which is expected. Health is UP even with failure-rate=1.0 (chaos covers only /api/**). Kill the java PID shown by `lsof -nP -iTCP:8090 -sTCP:LISTEN`, then `lsof` exit 1 and `ps -p <pid>` empty prove teardown.
- Order proof when the referee must run last: take snap-after first, then the referee; re-run the referee if you ran it earlier.
Related: [[scaffold-verification-techniques]], [[shell-traps]]
