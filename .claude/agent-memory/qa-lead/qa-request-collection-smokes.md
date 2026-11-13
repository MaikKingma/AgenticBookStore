---
name: qa-request-collection-smokes
description: Verifying http/*.http request collections and live smoke replays: collection inputs vs E2E inputs, replaying the file's own bytes, driver negative controls, comment claims, leftover compose containers, PostgreSQL 18 NOT NULL catalogue rows, curl and Boot error-body details
metadata:
  type: feedback
---

Siblings: [[qa-bugfix-and-live-runs]] (shared compose database, live-captured fixtures), [[qa-final-brief-integrity]].

1. **Request collections differ from the E2E inputs.** The file's sample values (hyphenated ISBN on a successful add, lowercase genre, `q` combined with a filter) are often not what the integration tests send, so "proven by E2E" does not cover the file. Diff the file's inputs against the E2E calls and name each uncovered combination as a smoke item.
   - The smoke replays the file's own bytes (bodies cut with `sed -n`, sent with `curl --data @file`), never re-typed requests. Preferred form: a `python3 -I` driver that parses the file into its `###` blocks (method, URL, headers, body line range, ids from `client.global.set` handlers) and writes one evidence file with a CHECK line per expectation. A JSON witness body cannot carry a raw tab (`\t` escape, check on the decoded value). `ijhttp` is usually absent: the `> {% %}` handler syntax stays an inspection-only residual.
   - Smoke expectations come from controller annotations or asserting tests, never from the impl artifact's request table (it wrote 201 for a 202 add).
   - Request comments stating results (a total, stock left, a status) and header claims about data or folder ("every ISBN in this folder is unseeded") are claims: write each into the directive per statement across all files, or verifiers file them as advisory. A comment-fix criterion worded "line N reads X" silently deletes every other sentence on that line: write "the sentence S becomes X". When a collection's inputs change, grep other tasks' tests for constants naming them.
   - A replay driver is itself a falsifier needing negative controls: mutated copies of the file in SP (EAN swapped for another availability, boundary value moved, capture handler removed, query changed), each driver run exit 1 with its reason. A swap value must reach the property: one already used earlier in the file fails at the add with a 409; pick a census-checked value absent from the file and the remote catalogue.

2. **Remote reads and forwarders.** Before writing a field expectation on a live remote read, read the remote's response record: the distributor stores `retailerReference` but never echoes it and offers no order list. A disclosed recording forwarder (base URL pointed at it, status and body relayed verbatim) is accepted evidence for outbound bodies and call counts; it leaves the default base URL and direct wire framing (chunked bodies) unexercised, so name one forwarder-free run for qa-final.

3. **Compose leftovers.** With `spring-boot-docker-compose` on the classpath, `spring-boot:run` starts `compose.yaml` and stops it with `stop`, not `down`: a stopped leftover container returns old rows, so "exact search result on a fresh database" fails for the wrong reason. In the smoke preflight check `docker compose ... ps -a` and `down` any leftover first (but see [[qa-bugfix-and-live-runs]] on the shared database). Use the smoke to close inspection-only residuals cheaply (constraint names via `pg_constraint`, untested query parameters, null-list guards).

4. **PostgreSQL 18 catalogues NOT NULL as `pg_constraint` rows** (contype `n`, `<table>_<column>_not_null`): an "exactly these constraint names" query needs `and contype <> 'n'`; the `n` rows are a free nullability witness. A "no content type" probe needs `curl -H 'Content-Type:'` plus `-v` proof.
